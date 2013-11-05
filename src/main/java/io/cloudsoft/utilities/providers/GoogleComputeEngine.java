package io.cloudsoft.utilities.providers;

import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import io.cloudsoft.utilities.io.cloudsoft.utilities.model.Instance;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;

import java.util.List;

public class GoogleComputeEngine extends Provider {

    public GoogleComputeEngine(String identity, String credential) {
        super(GOOGLE_COMPUTE_ENGINE_PROVIDER, identity, credential);
    }

    @Override
    public List<Instance> listInstances() throws Exception {
        List<Instance> instances = Lists.newArrayList();
        ComputeServiceContext computeServiceContext = getComputeServiceContext(name);
        try {
            for (NodeMetadata nodeMetadata : computeServiceContext.getComputeService().listNodesDetailsMatching(
                    Predicates.<ComputeMetadata>notNull())) {
                instances.add(Instance.builder().id(nodeMetadata.getId()).provider(name)
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
        return instances;
    }

}
