package io.cloudsoft.utilities.providers;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import io.cloudsoft.utilities.io.cloudsoft.utilities.model.Instance;
import org.jclouds.Constants;
import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.jclouds.compute.config.ComputeServiceProperties.TIMEOUT_SCRIPT_COMPLETE;

public abstract class Provider {

    private static final Logger log = LoggerFactory.getLogger(Provider.class);

    /**
     * AWS EC2.
     */
    public static final String AWS_PROVIDER = "aws-ec2";
    /**
     * Rackspace Cloud Servers UK.
     */
    public static final String RACKSPACE_UK_PROVIDER = "rackspace-cloudservers-uk";
    /**
     * Rackspace Cloud Servers US.
     */
    public static final String RACKSPACE_US_PROVIDER = "rackspace-cloudservers-us";
    /**
     * Softlayer
     */
    public static final String SOFTLAYER_PROVIDER = "softlayer";
    /**
     * Hp Cloud Compute.
     */
    public static final String HPCLOUD_PROVIDER = "hpcloud-compute";
    /**
     * Google Cloud Compute
     */
    public static final String GOOGLE_COMPUTE_ENGINE_PROVIDER = "google-compute-engine";
    /**
     * IBM Smart Cloud Enterprise.
     */
    public static final String IBM_SCE_PROVIDER = "ibm-sce-compute";
    /**
     * Interoute.
     */
    public static final String INTEROUTE_PROVIDER = "interoute";

    protected static final String STATUS = "STATUS";
    protected static final String LAST_RUN = "LAST-RUN";
    protected enum TAG_VALUE {
        DELETABLE
    }

    protected enum ACTION {
        LIST, TAG_AND_CLEANUP, DESTROY
    }

    protected String name;
    protected String identity;
    protected String credential;

    protected Provider(String name, String identity, String credential) {
        this.name = name;
        this.identity = identity;
        this.credential = credential;
    }

    public List<Instance> listInstances() throws Exception {
        log.debug("listInstances");
        return null;
    }

    public void tagAndCleanInstances(String tag) throws Exception {
        log.debug("tagAndCleanInstances with tag: " + tag);
    }

    public List<Instance> destroyInstances(String prefix) {
        log.debug("destroyInstances with " + prefix);
        return null;
    }

    /**
     * Create a jclouds {@link org.jclouds.rest.RestContext} to access the Compute API.
     */
    protected ComputeServiceContext getComputeServiceContext(String provider) throws Exception {
        Properties properties = new Properties();
        long scriptTimeout = TimeUnit.MILLISECONDS.convert(20, TimeUnit.MINUTES);
        properties.setProperty(TIMEOUT_SCRIPT_COMPLETE, scriptTimeout + "");
        properties.setProperty(Constants.PROPERTY_TRUST_ALL_CERTS, "true");
        ImmutableSet<Module> modules = ImmutableSet.<Module>of(new SLF4JLoggingModule());
        ContextBuilder builder = ContextBuilder.newBuilder(provider)
                .credentials(identity, credential)
                .modules(modules)
                .overrides(properties);
        return builder.buildView(ComputeServiceContext.class);
    }
}