package io.cloudsoft.utilities.providers;

import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import io.cloudsoft.utilities.io.cloudsoft.utilities.model.Instance;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;

import java.util.List;

import static io.cloudsoft.utilities.cli.CloudCleaner.GOOGLE_COMPUTE_ENGINE_PROVIDER;

public class GoogleComputeEngine extends Provider {

    public GoogleComputeEngine(String identity, String credential) {
        super(GOOGLE_COMPUTE_ENGINE_PROVIDER, identity, credential);
    }


    @Override
    public List<Instance> listInstances() throws Exception {
        List<Instance> instances = Lists.newArrayList();
        ComputeServiceContext computeServiceContext = getComputeServiceContext(provider);
        try {
            for (NodeMetadata nodeMetadata : computeServiceContext.getComputeService().listNodesDetailsMatching(
                    Predicates.<ComputeMetadata>notNull())) {
                instances.add(Instance.builder().id(nodeMetadata.getId()).provider(provider)
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
        System.out.println("No-op");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
