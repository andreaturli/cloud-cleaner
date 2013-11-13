package io.cloudsoft.utilities.providers;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.ibm.cloud.api.rest.client.DeveloperCloud;
import com.ibm.cloud.api.rest.client.DeveloperCloudClient;
import io.cloudsoft.utilities.io.cloudsoft.utilities.model.Instance;
import org.jclouds.domain.Credentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Set;

public class IbmSmartCloudEnterprise extends BasicProvider {

   private static final Logger log = LoggerFactory.getLogger(IbmSmartCloudEnterprise.class);
   private static final String IBM_SCE_PROVIDER = "ibm-sce-compute";

   public IbmSmartCloudEnterprise(Set<Credentials> credentials) {
      super(credentials);
   }

   @Override
   public String getName() {
      return IBM_SCE_PROVIDER;
   }

   @Override
   public List<Instance> listInstances() {
      List<Instance> instances = Lists.newArrayList();
      DeveloperCloudClient client = DeveloperCloud.getClient();
      for (Credentials creds : credentials) {
         client.setRemoteCredentials(creds.identity, creds.credential);
         try {
            for (com.ibm.cloud.api.rest.client.bean.Instance instance : client.describeInstances()) {
               instances.add(Instance.builder().id(instance.getID()).provider(getName())
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
      }
      return instances;
   }

}

