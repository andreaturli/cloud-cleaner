package io.cloudsoft.utilities.providers;

import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.cloudsoft.utilities.io.cloudsoft.utilities.model.Instance;
import org.jclouds.ContextBuilder;
import org.jclouds.collect.IterableWithMarker;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.domain.Credentials;
import org.jclouds.googlecomputeengine.GoogleComputeEngineApi;
import org.jclouds.googlecomputeengine.domain.Firewall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

import static com.google.common.collect.Iterables.getLast;

public class GoogleComputeEngine extends BasicProvider {

   private static final Logger log = LoggerFactory.getLogger(GoogleComputeEngine.class);
   private static final String GOOGLE_COMPUTE_ENGINE_PROVIDER = "google-compute-engine";

   public GoogleComputeEngine() {
      super();
   }

   public GoogleComputeEngine(Set<Credentials> credentials) {
      super(credentials);
   }

   @Override
   public String getName() {
      return GOOGLE_COMPUTE_ENGINE_PROVIDER;
   }

   @Override
   public List<Instance> listInstances() {
      List<Instance> instances = Lists.newArrayList();
      for (Credentials creds : credentials) {
         ComputeServiceContext computeServiceContext = getComputeServiceContext(getName(), creds.identity, creds.credential);
         try {
            for (NodeMetadata nodeMetadata : computeServiceContext.getComputeService().listNodesDetailsMatching(
                    Predicates.<ComputeMetadata>notNull())) {
               instances.add(Instance.builder().id(nodeMetadata.getId())
                       .name(nodeMetadata.getName())
                       .provider(getName())
                       .region(nodeMetadata.getLocation().getDescription()).type(nodeMetadata.getType().name())
                       .status(nodeMetadata.getStatus().name())
                       .groupName(nodeMetadata.getGroup())
                       .build());
            }
         } catch (Exception e) {
            throw Throwables.propagate(e);
         } finally {
            computeServiceContext.close();
         }
      }
      return instances;
   }

   @Override
   public void deleteNetworks(String projectName, String prefix) throws Exception {
      for (Credentials creds : credentials) {
         GoogleComputeEngineApi api = ContextBuilder.newBuilder(getName())
                 .credentials(creds.identity, creds.credential).buildApi(GoogleComputeEngineApi.class);
         Set<String> networkNames = Sets.newHashSet();
         Set<Firewall> deletableFirewalls = deletableFirewalls(projectName, prefix, api);
         for (Firewall firewall : deletableFirewalls) {
            final String networkName = getLast(Splitter.on("/").split(firewall.getNetwork().getPath()));
            networkNames.add(networkName);
            deleteFirewall(api, projectName, firewall, networkName);
         }
         for (String networkName : networkNames) {
            log.info("Deleting network {} ...", networkName);
            api.getNetworkApiForProject(projectName).delete(networkName);
            log.info("Deleted network {}", networkName);
         }
      }
   }

   @Override
   public void deleteFirewalls(String projectName, String prefix) throws Exception {
      for (Credentials creds : credentials) {
         GoogleComputeEngineApi api = ContextBuilder.newBuilder(getName())
                 .credentials(creds.identity, creds.credential).buildApi(GoogleComputeEngineApi.class);
         Set<Firewall> deletableFirewalls = deletableFirewalls(projectName, prefix, api);
         for (Firewall firewall : deletableFirewalls) {
            String networkName = getLast(Splitter.on("/").split(firewall.getNetwork().getPath()));
            deleteFirewall(api, projectName, firewall, networkName);
         }
      }
   }

   private void deleteFirewall(GoogleComputeEngineApi api, String projectName, Firewall firewall, String networkName) {
      log.info("Deleting firewall {} from network {} ...", firewall.getName(), networkName);
      api.getFirewallApiForProject(projectName).delete(firewall.getName());
      log.info("Deleted firewall {} from network {} !!!", firewall.getName(), networkName);
   }

   private Set<Firewall> deletableFirewalls(String projectName, String prefix, GoogleComputeEngineApi api) {
      Set<Firewall> deletableFirewalls = Sets.newHashSet();
      for (IterableWithMarker<Firewall> iterableWithMarkerFirewall : api.getFirewallApiForProject(projectName).list().toSet()) {
         for (Firewall firewall : iterableWithMarkerFirewall.toSet()) {
            if (firewall.getName().startsWith(prefix)) {
               deletableFirewalls.add(firewall);
            }
         }
      }
      return deletableFirewalls;
   }

}
