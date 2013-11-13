package io.cloudsoft.utilities.cli;

import com.google.common.base.Throwables;
import io.airlift.command.Arguments;
import io.airlift.command.Command;
import io.airlift.command.Option;
import io.cloudsoft.utilities.providers.ProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Command(name = "tagAndClean", description = "Tag and clean long-running instances on the clouds")
public class TagAndCleanInstances extends CloudCleanerCommand {

    private static final Logger log = LoggerFactory.getLogger(TagAndCleanInstances.class);

    @Option(name = {"-t", "--tag"}, description = "Tag")
    public String tag = "";

    @Override
    public void run() {
        super.run();
        try {
            listInstances();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private void listInstances() throws Exception {
        log.info("List all instances of provider(s): {} and their tags", providers);
        for (String provider : providers) {
            new ProviderFactory().getProviderInstance(provider).tagAndCleanInstances(tag);
        }
    }

}
