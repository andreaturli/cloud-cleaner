package io.cloudsoft.utilities.providers;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.ibm.cloud.api.rest.client.DeveloperCloud;
import com.ibm.cloud.api.rest.client.DeveloperCloudClient;
import io.cloudsoft.utilities.io.cloudsoft.utilities.model.Instance;

import java.util.Date;
import java.util.List;

import static io.cloudsoft.utilities.cli.CloudCleaner.IBM_SCE_PROVIDER;

public class IbmSmartCloudEnterprise extends Provider {

    public IbmSmartCloudEnterprise(String identity, String credential) {
        super(IBM_SCE_PROVIDER, identity, credential);
    }

    @Override
    public List<Instance> listInstances() throws Exception {
        List<Instance> instances = Lists.newArrayList();
        DeveloperCloudClient client = DeveloperCloud.getClient();
        client.setRemoteCredentials(identity, credential);
        try {
            for (com.ibm.cloud.api.rest.client.bean.Instance instance : client.describeInstances()) {
                instances.add(Instance.builder().id(instance.getID()).provider(provider)
                        .region(instance.getLocation()).type(instance.getInstanceType())
                        .status(instance.getStatus().toString())
                        .keyName(instance.getKeyName())
                        .uptime(new Date().getTime() - instance.getLaunchTime().getTime())
                                // .tags(nodeMetadata.getTags())
                        .build());
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        return instances;    }

    @Override
    public List<Instance> destroyInstances(String prefix) {
        System.out.println("No-op");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
