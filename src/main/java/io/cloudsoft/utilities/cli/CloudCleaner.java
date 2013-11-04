package io.cloudsoft.utilities.cli;

import com.google.common.collect.ImmutableList;
import io.airlift.command.Cli;
import io.airlift.command.Help;

import java.util.List;

public class CloudCleaner {

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
    public static final List<String> SUPPORTED_PROVIDERS =
            ImmutableList.of(/*GOOGLE_COMPUTE_ENGINE_PROVIDER,*/ SOFTLAYER_PROVIDER, AWS_PROVIDER,
                    HPCLOUD_PROVIDER, RACKSPACE_UK_PROVIDER, RACKSPACE_US_PROVIDER, INTEROUTE_PROVIDER);

    public static void main(String[] args) {
        Cli.CliBuilder<Runnable> builder = Cli.<Runnable>builder("cloud-cleaner")
                .withDescription("Your cloud assistant")
                .withDefaultCommand(Help.class)
                .withCommands(Help.class, ListInstances.class, DestroyInstances.class);

        Cli<Runnable> gitParser = builder.build();
        gitParser.parse(args).run();
    }
}