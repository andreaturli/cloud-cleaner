package io.cloudsoft.utilities.providers;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import io.cloudsoft.utilities.io.cloudsoft.utilities.model.Instance;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.rest.RestContext;
import org.jclouds.softlayer.SoftLayerAsyncClient;
import org.jclouds.softlayer.SoftLayerClient;
import org.jclouds.softlayer.domain.VirtualGuest;

import java.util.Date;
import java.util.List;

public class Softlayer extends BasicProvider {

    public Softlayer(String identity, String credential) {
        super(identity, credential);
    }

    @Override
    public String getName() {
        return SOFTLAYER_PROVIDER;
    }

    @Override
    public List<Instance> listInstances() throws Exception {
        List<Instance> instances = Lists.newArrayList();
        ComputeServiceContext computeServiceContext = getComputeServiceContext(getName());
        try {
            RestContext<SoftLayerClient, SoftLayerAsyncClient> client = computeServiceContext.unwrap();
            SoftLayerClient api = client.getApi();
            for (VirtualGuest virtualGuest : api.getVirtualGuestClient().listVirtualGuests()) {
                instances.add(Instance.builder().id(virtualGuest.getUuid()).provider(getName())
                        .region(virtualGuest.getDatacenter().getLongName()).type("Cloud Compute Instance")
                        .status(virtualGuest.getPowerState().toString()).keyName(virtualGuest.getAccountId() + "")
                        .uptime(new Date().getTime() - virtualGuest.getCreateDate().getTime())
                        .build());
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            computeServiceContext.close();
        }
        return instances;    }

}
