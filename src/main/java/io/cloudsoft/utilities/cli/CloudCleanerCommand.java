package io.cloudsoft.utilities.cli;

import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import io.airlift.command.Arguments;
import io.airlift.command.Option;
import io.cloudsoft.utilities.io.cloudsoft.utilities.model.Instance;
import io.cloudsoft.utilities.providers.Accounts;
import io.cloudsoft.utilities.providers.Providers;
import org.jclouds.domain.Credentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

public class CloudCleanerCommand implements Runnable {

   private static final Logger log = LoggerFactory.getLogger(CloudCleanerCommand.class);
   @Arguments(description = "Cloud providers to be searched")
   public Set<String> providers;
   @Option(name = "-s", description = "show credentials in log")
   public boolean showCredentialsInLog;

   public void run() {
      log.info("Cloud providers to be searched: " + Iterables.toString(providers));
      try {
         providers = Sets.intersection(Providers.validProviders(), providers);
         log.info("CloudCleaner will search the following providers: " + Iterables.toString(providers));
         for (String provider : providers) {
            for (Credentials creds : Accounts.getLoginCredentials(provider)) {
               if (showCredentialsInLog) {
                  log.info("Found credentials for provider({}) - identity({}), credential({})", provider, creds.identity, creds.credential);
               } else {
                  log.info("Found credentials for provider({}) - identity({})", provider, creds.identity);
               }
            }
         }
      } catch (Exception e) {
         throw Throwables.propagate(e);
      }

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
