package io.cloudsoft.utilities.providers;

public class RackspaceCloudserversUK extends Openstack {

    public RackspaceCloudserversUK(String identity, String credential) {
        super(identity, credential);
    }

    @Override
    public String getName() {
        return RACKSPACE_UK_PROVIDER;
    }
}
