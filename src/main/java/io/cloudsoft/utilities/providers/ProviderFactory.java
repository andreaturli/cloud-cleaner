package io.cloudsoft.utilities.providers;

import java.util.List;
import java.util.Map;

import static io.cloudsoft.utilities.cli.CloudCleaner.AWS_PROVIDER;
import static io.cloudsoft.utilities.cli.CloudCleaner.GOOGLE_COMPUTE_ENGINE_PROVIDER;
import static io.cloudsoft.utilities.cli.CloudCleaner.HPCLOUD_PROVIDER;
import static io.cloudsoft.utilities.cli.CloudCleaner.IBM_SCE_PROVIDER;
import static io.cloudsoft.utilities.cli.CloudCleaner.INTEROUTE_PROVIDER;
import static io.cloudsoft.utilities.cli.CloudCleaner.RACKSPACE_UK_PROVIDER;
import static io.cloudsoft.utilities.cli.CloudCleaner.RACKSPACE_US_PROVIDER;
import static io.cloudsoft.utilities.cli.CloudCleaner.SOFTLAYER_PROVIDER;

public class ProviderFactory {

    protected Map<String, List<String>> credentials;

    public ProviderFactory(Map<String, List<String>> credentials) {
        this.credentials = credentials;
    }

    public Provider createProvider(String provider) {
        if (provider.equals(AWS_PROVIDER)) {
            return new Ec2(AWS_PROVIDER, getIdentity(AWS_PROVIDER), getCredential(AWS_PROVIDER));
        } else if (provider.equals(RACKSPACE_UK_PROVIDER)) {
            return new Openstack(RACKSPACE_UK_PROVIDER, getIdentity(RACKSPACE_UK_PROVIDER),
                    getCredential(RACKSPACE_UK_PROVIDER));
        } else if (provider.equals(RACKSPACE_US_PROVIDER)) {
            return new Openstack(RACKSPACE_US_PROVIDER, getIdentity(RACKSPACE_US_PROVIDER),
                    getCredential(RACKSPACE_US_PROVIDER));
        } else if (provider.equals(HPCLOUD_PROVIDER)) {
            return new Openstack(HPCLOUD_PROVIDER, getIdentity(HPCLOUD_PROVIDER), getCredential(HPCLOUD_PROVIDER));
        } else if (provider.equals(SOFTLAYER_PROVIDER)) {
            return new Softlayer(getIdentity(SOFTLAYER_PROVIDER), getCredential(SOFTLAYER_PROVIDER));
        } else if (provider.equals(GOOGLE_COMPUTE_ENGINE_PROVIDER)) {
            return new GoogleComputeEngine(getIdentity(GOOGLE_COMPUTE_ENGINE_PROVIDER),
                    getCredential(GOOGLE_COMPUTE_ENGINE_PROVIDER));
        } else if (provider.equals(IBM_SCE_PROVIDER)) {
            return new IbmSmartCloudEnterprise(getIdentity(IBM_SCE_PROVIDER), getCredential(IBM_SCE_PROVIDER));
        } else if (provider.equals(INTEROUTE_PROVIDER)) {
            return new Interoute(getIdentity(INTEROUTE_PROVIDER), getCredential(INTEROUTE_PROVIDER));
        } else {
            throw new RuntimeException("Not supported api/provider: " + provider);
        }
    }

    private String getIdentity(String provider) {
        if(!credentials.containsKey(provider)) {
            throw new IllegalStateException("Cannot find identity for " + provider);
        }
        return credentials.get(provider).get(0);
    }

    private String getCredential(String provider) {
        if(!credentials.containsKey(provider)) {
            throw new IllegalStateException("Cannot find credential for " + provider);
        }
        return credentials.get(provider).get(1);
    }
}