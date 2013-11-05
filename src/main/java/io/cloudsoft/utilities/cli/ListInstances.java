package io.cloudsoft.utilities.cli;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import io.airlift.command.Arguments;
import io.airlift.command.Command;
import io.cloudsoft.utilities.providers.ProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static io.cloudsoft.utilities.cli.CloudCleaner.SUPPORTED_PROVIDERS;

@Command(name = "list", description = "List all instances running on the clouds")
public class ListInstances extends CloudCleanerCommand {

    private static final Logger log = LoggerFactory.getLogger(ListInstances.class);

    @Arguments(description = "Cloud providers to be searched")
    public List<String> providers;

    @Override
    public void run() {
        super.run();
        try {
            listInstances(Optional.of(providers));
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private void listInstances(Optional<List<String>> optionalProviders) throws Exception {
        List<String> providers = optionalProviders.isPresent() ? optionalProviders.get() :  SUPPORTED_PROVIDERS;
        log.info("List all instances of provider(s): {} and their tags", providers);
        for (String provider : providers) {
                printInstances(provider, new ProviderFactory().createProvider(provider)
                        .listInstances());
        }
    }

}
