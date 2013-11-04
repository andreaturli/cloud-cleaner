package io.cloudsoft.utilities.cli;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import io.airlift.command.Option;
import io.airlift.command.OptionType;
import io.cloudsoft.utilities.io.cloudsoft.utilities.model.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.cloudsoft.utilities.cli.CloudCleaner.SUPPORTED_PROVIDERS;

public class CloudCleanerCommand implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(CloudCleanerCommand.class);

    /**
     * System property for access key.
     */
    protected static final String IDENTITY_PROPERTY = "identity";
    /**
     * System property for secret key.
     */
    protected static final String CREDENTIAL_PROPERTY = "credential";

    @Option(type = OptionType.GLOBAL, name = "-v", description = "Verbose mode")
    public boolean verbose;

    @Option(name = "-s", description = "show credentials in log")
    public boolean showCredentialsInLog;

    private final Map<String, List<String>> providerAndCredentials;

    public CloudCleanerCommand() {
        providerAndCredentials = initCredentials();
        this.verbose = false;
    }

    public void run() {
        System.out.println(getClass().getSimpleName());
    }

    private Map<String, List<String>> initCredentials() {
        Map<String, List<String>> credentials = Maps.newHashMap();
        for (String provider : SUPPORTED_PROVIDERS) {
            String identity = checkNotNull(System.getProperty(provider + "." + IDENTITY_PROPERTY), IDENTITY_PROPERTY + " for " + provider);
            String credential = checkNotNull(System.getProperty(provider + "." + CREDENTIAL_PROPERTY), CREDENTIAL_PROPERTY + " for " + provider);
            if (showCredentialsInLog) {
                log.info("provider({}) - identity({}), credential({})", provider, identity, credential);
            } else {
                log.info("Found credentials for provider({}) - identity({})", provider, identity);
            }
            credentials.put(provider, ImmutableList.of(identity, credential));
        }
        return credentials;
    }

    public void printInstances(String provider, List<Instance> instances) {
        log.info("==================================================================================================");
        log.info("  PROVIDER '{}'", provider);
        log.info("==================================================================================================");
        for (Instance instance : instances) {
            log.info(instance.toString());
        }
    }

    public Map<String, List<String>> getCredentials() {
        return providerAndCredentials;
    }
}

