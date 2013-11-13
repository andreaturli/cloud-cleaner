package io.cloudsoft.utilities.providers;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import io.cloudsoft.utilities.io.cloudsoft.utilities.model.Instance;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.domain.Credentials;
import org.jclouds.rest.RestContext;
import org.jclouds.softlayer.SoftLayerAsyncClient;
import org.jclouds.softlayer.SoftLayerClient;
import org.jclouds.softlayer.domain.VirtualGuest;

import java.util.Date;
import java.util.List;
import java.util.Set;

public class Softlayer extends BasicProvider {

   private static final String SOFTLAYER_PROVIDER = "softlayer";

   public Softlayer(Set<Credentials> credentials) {
      super(credentials);
   }

   @Override
   public String getName() {
      return SOFTLAYER_PROVIDER;
   }

   @Override
   public List<Instance> listInstances() {
      List<Instance> instances = Lists.newArrayList();
      for (Credentials creds : credentials) {
         ComputeServiceContext computeServiceContext = getComputeServiceContext(getName(), creds.identity, creds.credential);
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
      }
      return instances;
   }

}
