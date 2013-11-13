package io.cloudsoft.utilities.providers;

import org.jclouds.domain.Credentials;

import java.util.Set;

public class RackspaceCloudserversUS extends Openstack {

   private static final String RACKSPACE_US_PROVIDER = "rackspace-cloudservers-us";

   public RackspaceCloudserversUS() {
      super();
   }

   public RackspaceCloudserversUS(Set<Credentials> credentials) {
      super(credentials);
   }

   @Override
   public String getName() {
      return RACKSPACE_US_PROVIDER;
   }
}
