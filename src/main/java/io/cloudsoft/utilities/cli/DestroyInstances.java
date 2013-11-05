package io.cloudsoft.utilities.cli;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import io.airlift.command.Arguments;
import io.airlift.command.Command;
import io.airlift.command.Option;
import io.cloudsoft.utilities.io.cloudsoft.utilities.model.Instance;
import io.cloudsoft.utilities.providers.ProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static io.cloudsoft.utilities.cli.CloudCleaner.SUPPORTED_PROVIDERS;

@Command(name = "destroy", description = "Destroy all instances in the clouds matching a given prefix")
public class DestroyInstances extends CloudCleanerCommand {

    private static final Logger log = LoggerFactory.getLogger(ListInstances.class);

    @Arguments(description = "Cloud providers to be searched")
    public List<String> providers;

    @Option(name = {"-p", "--prefix"}, description = "Group prefix")
    public String prefix = "";

    @Override
    public void run() {
        super.run();
        try {
            destroyInstances(Optional.of(providers), prefix);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private List<Instance> destroyInstances(Optional<List<String>> optionalProviders, String prefix) {
        List<Instance> destroyedInstances = Lists.newArrayList();
        List<String> providers = optionalProviders.isPresent() ? optionalProviders.get() :  SUPPORTED_PROVIDERS;
        log.info("Destroy all instances of provider(s): {} matching prefix: {}", providers, prefix);
        for (String provider : providers) {
            destroyedInstances = new ProviderFactory().createProvider(provider)
                    .destroyInstances(prefix);
        }
        return destroyedInstances;
    }

}
