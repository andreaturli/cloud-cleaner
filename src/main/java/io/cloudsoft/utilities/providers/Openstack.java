package io.cloudsoft.utilities.providers;

import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.inject.Module;
import io.cloudsoft.utilities.io.cloudsoft.utilities.model.Instance;
import org.jclouds.Constants;
import org.jclouds.ContextBuilder;
import org.jclouds.collect.IterableWithMarker;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.NovaAsyncApi;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.jclouds.openstack.v2_0.domain.Resource;
import org.jclouds.rest.RestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.jclouds.compute.config.ComputeServiceProperties.TIMEOUT_SCRIPT_COMPLETE;

public abstract class Openstack extends BasicProvider {

    private static final Logger log = LoggerFactory.getLogger(Openstack.class);

    protected enum TAG_VALUE {
        DELETABLE
    }
    protected static final String STATUS = "STATUS";


    protected String identity;
    protected String credential;

    public Openstack(String identity, String credential) {
        super(identity, credential);
        this.identity = identity;
        this.credential = credential;
    }

    @Override
    public List<Instance> listInstances() throws Exception {
        List<Instance> instances = Lists.newArrayList();
        ComputeServiceContext computeServiceContext = getComputeServiceContext(getName());
        try {
            RestContext<NovaApi, NovaAsyncApi> client = computeServiceContext.unwrap();
            for (String zone : client.getApi().getConfiguredZones()) {
                ServerApi serverApiForZone = client.getApi().getServerApiForZone(zone);
                FluentIterable<? extends Server> servers = serverApiForZone.listInDetail().concat();

                for (Server server : servers) {
                    serverApiForZone.getMetadata(server.getId());
                    instances.add(Instance.builder().id(server.getId())
                            .name(server.getName())
                            .provider(getName()).region(zone)
                            .type(server.getFlavor().getId()).status(server.getStatus().toString())
                            .keyName(server.getKeyName()).uptime(new Date().getTime() - server.getCreated().getTime())
                            .tags(serverApiForZone.getMetadata(server.getId())).build());
                }
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            computeServiceContext.close();
        }
        return instances;
    }

    @Override
    public void tagAndCleanInstances(String tag) throws Exception {
        ComputeServiceContext computeServiceContext = getComputeServiceContext(getName());
        RestContext<NovaApi, NovaAsyncApi> client = computeServiceContext.unwrap();
        for (String zone : client.getApi().getConfiguredZones()) {
            ServerApi serverApiForZone = client.getApi().getServerApiForZone(zone);
            ImmutableList<? extends IterableWithMarker<? extends Resource>> iterableWithMarkers =
                    serverApiForZone.list().toImmutableList();

            for (IterableWithMarker<? extends Resource> iterableWithMarker : iterableWithMarkers) {
                String instanceId = iterableWithMarker.get(0).getId();
                serverApiForZone.setMetadata(instanceId, ImmutableMap.of(STATUS, TAG_VALUE.DELETABLE.toString()));
                Map<String, String> metadata = serverApiForZone.getMetadata(instanceId);
                for (String key : metadata.keySet()) {
                    log.info("{}={}", key, metadata.get(key));
                }
            }
        }
    }

    /**
     * Create a jclouds {@link org.jclouds.rest.RestContext} to access the Compute API.
     */
    protected ComputeServiceContext getComputeServiceContext(String provider) throws Exception {
        Properties properties = new Properties();
        long scriptTimeout = TimeUnit.MILLISECONDS.convert(20, TimeUnit.MINUTES);
        properties.setProperty(TIMEOUT_SCRIPT_COMPLETE, scriptTimeout + "");
        properties.setProperty(Constants.PROPERTY_TRUST_ALL_CERTS, "true");
        ImmutableSet<Module> modules = ImmutableSet.<Module>of(new SLF4JLoggingModule());
        ContextBuilder builder = ContextBuilder.newBuilder(provider)
                .credentials(identity, credential)
                .modules(modules)
                .overrides(properties);
        return builder.buildView(ComputeServiceContext.class);
    }
}