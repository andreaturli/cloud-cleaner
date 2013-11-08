package io.cloudsoft.utilities.cli;

import com.google.common.base.Throwables;
import io.airlift.command.Arguments;
import io.airlift.command.Command;
import io.airlift.command.Option;
import io.cloudsoft.utilities.providers.ProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Command(name = "destroynetworks", description = "Destroy all networks in the clouds matching a given prefix")
public class DestroyNetworks extends CloudCleanerCommand {

    private static final Logger log = LoggerFactory.getLogger(ListInstances.class);

    @Arguments(description = "Cloud providers to be searched")
    public List<String> providers;

    @Option(name = {"-prj", "--project"}, description = "Name of the project", required = false)
    public String projectName = "";

    @Option(name = {"-p", "--prefix"}, description = "Group prefix")
    public String prefix = "";

    @Override
    public void run() {
        super.run();
        try {
            destroyNetworks();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private void destroyNetworks() throws Exception {
        log.info("Destroy all networks of provider(s): {} matching prefix: {}", providers, prefix);
        for (String provider : providers) {
            new ProviderFactory().createProvider(provider)
                    .destroyNetworks(projectName, prefix);
        }
    }

}
