package io.cloudsoft.utilities.cli;

import com.google.common.base.Throwables;
import io.airlift.command.Arguments;
import io.airlift.command.Command;
import io.airlift.command.Option;
import io.cloudsoft.utilities.providers.ProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Command(name = "cleanup", description = "Clean up the given cloud (nodes, firewalls, networks)")
public class CleanCloud extends CloudCleanerCommand {

    private static final Logger log = LoggerFactory.getLogger(ListInstances.class);

    @Option(name = {"-prj", "--project"}, description = "Name of the project", required = false)
    public String projectName = "";

    @Option(name = {"-np", "--networkPrefix"}, description = "Network prefix", required = false)
    public String networkPrefix = "";

    @Option(name = {"-gp", "--groupPrefix"}, description = "Group prefix", required = true)
    public String groupPrefix = "";

    @Override
    public void run() {
        super.run();
        try {
            cleanUp();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private void cleanUp() throws Exception {
       for (String provider : providers) {
         log.info("Clean up cloud({}) with groupPrefix({})", provider, groupPrefix);
         new ProviderFactory().getProviderInstance(provider).deleteNetworks(projectName, networkPrefix);
         new ProviderFactory().getProviderInstance(provider).deleteNodes(groupPrefix);
       }
    }

}
