package io.cloudsoft.utilities.providers;

import org.jclouds.domain.Credentials;

import java.util.Set;

public class HpCloudCompute extends Openstack {

   private static final String HPCLOUD_PROVIDER = "hpcloud-compute";

   public HpCloudCompute() {
      super();
   }

   public HpCloudCompute(Set<Credentials> credentials) {
       super(credentials);
    }

    @Override
    public String getName() {
        return HPCLOUD_PROVIDER;
    }
}
