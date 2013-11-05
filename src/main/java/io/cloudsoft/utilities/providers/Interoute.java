package io.cloudsoft.utilities.providers;

import com.abiquo.server.core.cloud.VirtualApplianceState;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import io.cloudsoft.utilities.io.cloudsoft.utilities.model.Instance;
import org.jclouds.ContextBuilder;
import org.jclouds.abiquo.AbiquoContext;
import org.jclouds.abiquo.domain.cloud.VirtualAppliance;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.not;
import static com.google.common.base.Strings.emptyToNull;
import static org.jclouds.compute.predicates.NodePredicates.TERMINATED;

class Interoute extends Provider {

    protected Interoute(String identity, String credential) {
        super("abiquo", identity, credential);
    }

    @Override
    public List<Instance> listInstances() {
        List<Instance> instances = Lists.newArrayList();
        ComputeServiceContext computeServiceContext = ContextBuilder.newBuilder("abiquo")
                .endpoint("http://vdcbridge.interoute.com/jclouds/api/")
                .credentials(identity, credential).buildView(AbiquoContext.class);
        try {
            for (NodeMetadata nodeMetadata : computeServiceContext.getComputeService().listNodesDetailsMatching(
                    Predicates.<ComputeMetadata>notNull())) {
                instances.add(Instance.builder().id(nodeMetadata.getId()).provider("abiquo")
                        .region(nodeMetadata.getLocation().getDescription()).type(nodeMetadata.getType().name())
                        .status(nodeMetadata.getStatus().name())
                                // .keyName(nodeMetadata.getType())
                                // .uptime(new Date().getTime() -
                                // computeServiceContext.getComputeService().getTime())
                                // .tags(nodeMetadata.getTags())
                        .build());
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            computeServiceContext.close();
        }
        return instances;    }

    @Override
    public List<Instance> destroyInstances(String prefix) {
        List<Instance> instances = Lists.newArrayList();
        ComputeServiceContext computeServiceContext = ContextBuilder.newBuilder("abiquo")
                .endpoint("http://vdcbridge.interoute.com/jclouds/api/")
                .credentials(identity, credential).buildView(AbiquoContext.class);
        try {
            computeServiceContext.getComputeService()
                    .destroyNodesMatching(Predicates.<NodeMetadata>and(not(TERMINATED), groupStartsWith(prefix)));
            for (VirtualAppliance virtualAppliance : ((AbiquoContext) computeServiceContext).getCloudService()
                    .listVirtualAppliances()) {
                if (virtualAppliance.getName().startsWith(prefix) && virtualAppliance.getState() ==
                        VirtualApplianceState.NOT_DEPLOYED) {
                    instances.add(Instance.builder().id(virtualAppliance.getId().toString()).provider(provider)
                            .region(virtualAppliance.getVirtualDatacenter().getName())
                            .status(virtualAppliance.getState().name())
                            .build());
                    virtualAppliance.delete();
                }
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            computeServiceContext.close();
        }
        return instances;
    }

    private Predicate<? super NodeMetadata> groupStartsWith(final String groupPrefix) {
        checkNotNull(emptyToNull(groupPrefix), "groupPrefix must be defined");
        return new Predicate<NodeMetadata>() {
            @Override
            public boolean apply(NodeMetadata nodeMetadata) {
                return nodeMetadata.getGroup().startsWith(groupPrefix);
            }

            @Override
            public String toString() {
                return "groupStartsWith(" + groupPrefix + ")";
            }
        };
    }

}