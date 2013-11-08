package io.cloudsoft.utilities.providers;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.cloudsoft.utilities.io.cloudsoft.utilities.model.Instance;
import org.jclouds.ContextBuilder;
import org.jclouds.abiquo.AbiquoContext;
import org.jclouds.collect.IterableWithMarker;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.googlecomputeengine.GoogleComputeEngineApi;
import org.jclouds.googlecomputeengine.domain.Firewall;
import org.jclouds.googlecomputeengine.domain.Network;
import org.jclouds.googlecomputeengine.options.ListOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Iterables.getLast;

public class GoogleComputeEngine extends BasicProvider {

    private static final Logger log = LoggerFactory.getLogger(GoogleComputeEngine.class);

    public GoogleComputeEngine(String identity, String credential) {
        super(identity, credential);
    }

    @Override
    public String getName() {
        return GOOGLE_COMPUTE_ENGINE_PROVIDER;
    }

    @Override
    public List<Instance> listInstances() throws Exception {
        List<Instance> instances = Lists.newArrayList();
        ComputeServiceContext computeServiceContext = getComputeServiceContext(getName());
        try {
            for (NodeMetadata nodeMetadata : computeServiceContext.getComputeService().listNodesDetailsMatching(
                    Predicates.<ComputeMetadata>notNull())) {
                instances.add(Instance.builder().id(nodeMetadata.getId())
                        .name(nodeMetadata.getName())
                        .provider(getName())
                        .region(nodeMetadata.getLocation().getDescription()).type(nodeMetadata.getType().name())
                        .status(nodeMetadata.getStatus().name())
                        .groupName(nodeMetadata.getGroup())
                        .build());
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            computeServiceContext.close();
        }
        return instances;
    }

    @Override
    public void destroyNetworks(String projectName, String prefix) throws Exception {
        GoogleComputeEngineApi api = ContextBuilder.newBuilder(getName())
                .credentials(identity, credential).buildApi(GoogleComputeEngineApi.class);
        Set<String> networkOwners = Sets.newHashSet();
        for(IterableWithMarker<Firewall> iterableWithMarkerFirewall : api.getFirewallApiForProject(projectName).list().toSet()) {
            for(Firewall firewall : iterableWithMarkerFirewall.toSet()) {
                if(firewall.getName().startsWith(prefix)) {
                    final String networkName = getLast(Splitter.on("/").split(firewall.getNetwork().getPath()));
                    networkOwners.add(networkName);
                    log.info("Deleting firewall {} from network {} ...", firewall.getName(), networkName);
                    api.getFirewallApiForProject(projectName).delete(firewall.getName());
                    log.info("Deleted firewall {} from network {} !!!", firewall.getName(), networkName);
                }
            }
        }
        for (String networkName : networkOwners) {
            if (networkName.startsWith(prefix)) {
                log.info("Deleting network {} ...", networkName);
                api.getNetworkApiForProject(projectName).delete(networkName);
                log.info("Deleted network {} !!!", networkName);
            }
        }
    }
}
