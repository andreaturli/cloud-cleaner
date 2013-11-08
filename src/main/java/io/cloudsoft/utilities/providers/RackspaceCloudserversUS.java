package io.cloudsoft.utilities.providers;

public class RackspaceCloudserversUS extends Openstack {

    public RackspaceCloudserversUS(String identity, String credential) {
        super(identity, credential);
    }

    @Override
    public String getName() {
        return RACKSPACE_US_PROVIDER;
    }
}
