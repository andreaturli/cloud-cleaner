package io.cloudsoft.utilities.providers;

import io.cloudsoft.utilities.io.cloudsoft.utilities.model.Instance;

import java.util.List;

public interface Provider {

   String getName();

   List<Instance> listInstances();

   void tagAndCleanInstances(String tag) throws Exception;

   void deleteNodes(String prefix) throws Exception;

   /**
    * This will delete firewall or securityGroups
    *
    * @param project
    * @param prefix
    * @throws Exception
    */
   void deleteFirewalls(String project, String prefix) throws Exception;

   void deleteNetworks(String project, String prefix) throws Exception;
}