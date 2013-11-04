package io.cloudsoft.utilities.providers;

import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import io.cloudsoft.utilities.io.cloudsoft.utilities.model.Instance;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.NovaAsyncApi;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.jclouds.rest.RestContext;

import java.util.Date;
import java.util.List;

class Openstack extends Provider {

    protected Openstack(String provider, String identity, String credential) {
        super(provider, identity, credential);
    }

    @Override
    public List<Instance> listInstances() throws Exception {
        List<Instance> instances = Lists.newArrayList();
        ComputeServiceContext computeServiceContext = getComputeServiceContext(provider);
        try {
            RestContext<NovaApi, NovaAsyncApi> client = computeServiceContext.unwrap();
            for (String zone : client.getApi().getConfiguredZones()) {
                ServerApi serverApiForZone = client.getApi().getServerApiForZone(zone);
                FluentIterable<? extends Server> servers = serverApiForZone.listInDetail().concat();

                for (Server server : servers) {
                    serverApiForZone.getMetadata(server.getId());
                    instances.add(Instance.builder().id(server.getId()).provider(provider).region(zone)
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
    public List<Instance> destroyInstances(String prefix) {
        System.out.println("No-op");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

}