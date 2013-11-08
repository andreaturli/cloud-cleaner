package io.cloudsoft.utilities.providers;

public class HpCloudCompute extends Openstack {

    public HpCloudCompute(String identity, String credential) {
        super(identity, credential);
    }

    @Override
    public String getName() {
        return HPCLOUD_PROVIDER;
    }
}
