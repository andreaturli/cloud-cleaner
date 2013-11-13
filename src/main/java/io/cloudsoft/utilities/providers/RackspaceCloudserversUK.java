package io.cloudsoft.utilities.providers;

import org.jclouds.domain.Credentials;

import java.util.Set;

public class RackspaceCloudserversUK extends Openstack {

   private static final String RACKSPACE_UK_PROVIDER = "rackspace-cloudservers-uk";

   public RackspaceCloudserversUK() {
      super();
   }

   public RackspaceCloudserversUK(Set<Credentials> credentials) {
      super(credentials);
   }

   @Override
   public String getName() {
      return RACKSPACE_UK_PROVIDER;
   }
}
