package io.cloudsoft.utilities.cli;

import com.google.common.base.Throwables;
import io.airlift.command.Arguments;
import io.airlift.command.Command;
import io.cloudsoft.utilities.providers.ProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Command(name = "listinstances", description = "List all instances running on the clouds")
public class ListInstances extends CloudCleanerCommand {

   private static final Logger log = LoggerFactory.getLogger(ListInstances.class);

   @Override
   public void run() {
      super.run();
      try {
         listInstances();
      } catch (Exception e) {
         log.error("Cannot execute the list instances command.", e);
         throw Throwables.propagate(e);
      }
   }

   private void listInstances() throws Exception {
      log.info("List all instances of provider(s): {} and their tags", providers);
      for (String provider : providers) {
         printInstances(provider, new ProviderFactory().getProviderInstance(provider).listInstances());
      }
   }

}
