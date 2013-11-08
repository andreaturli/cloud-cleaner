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

public interface Provider {

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

    /*
    protected static final String LAST_RUN = "LAST-RUN";
    protected enum TAG_VALUE {
        DELETABLE
    }

    protected enum ACTION {
        LIST, TAG_AND_CLEANUP, DESTROY
    }
    */

    String getName();
    List<Instance> listInstances() throws Exception;
    void tagAndCleanInstances(String tag) throws Exception;

    void destroyNodes(String prefix) throws Exception;
    void destroyNetworks(String project, String prefix) throws Exception;
}