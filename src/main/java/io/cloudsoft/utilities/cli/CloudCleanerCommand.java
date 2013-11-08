package io.cloudsoft.utilities.cli;

import io.airlift.command.Option;
import io.airlift.command.OptionType;
import io.cloudsoft.utilities.io.cloudsoft.utilities.model.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CloudCleanerCommand implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(CloudCleanerCommand.class);

    @Option(type = OptionType.GLOBAL, name = "-v", description = "Verbose mode")
    public boolean verbose;

    @Option(name = "-s", description = "show credentials in log")
    public boolean showCredentialsInLog;

    public CloudCleanerCommand() {
        this.verbose = false;
    }

    public void run() {
        System.out.println(getClass().getSimpleName());
    }

    public void printInstances(String provider, List<Instance> instances) {
        log.info("==================================================================================================");
        log.info("  PROVIDER '{}' - {} instances running", provider, instances.size());
        log.info("==================================================================================================");
        for (Instance instance : instances) {
            log.info(instance.toString());
        }
    }

}
