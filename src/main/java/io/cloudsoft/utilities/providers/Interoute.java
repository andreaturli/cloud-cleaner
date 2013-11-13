package io.cloudsoft.utilities.providers;

import com.abiquo.server.core.cloud.VirtualApplianceState;
import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import io.cloudsoft.utilities.io.cloudsoft.utilities.model.Instance;
import org.jclouds.ContextBuilder;
import org.jclouds.abiquo.AbiquoContext;
import org.jclouds.abiquo.domain.cloud.VirtualAppliance;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.domain.Credentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

import static com.google.common.base.Predicates.not;
import static org.jclouds.compute.predicates.NodePredicates.TERMINATED;

class Interoute extends BasicProvider {

   private static final Logger log = LoggerFactory.getLogger(Interoute.class);
   private static final String INTEROUTE_PROVIDER = "interoute";

   public Interoute(Set<Credentials> credentials) {
      super(credentials);
   }

   @Override
   public String getName() {
      return INTEROUTE_PROVIDER;
   }

   @Override
   public List<Instance> listInstances() {
      List<Instance> instances = Lists.newArrayList();
      for (Credentials creds : credentials) {
         ComputeServiceContext computeServiceContext = ContextBuilder.newBuilder("abiquo")
                 .endpoint("http://vdcbridge.interoute.com/jclouds/api/")
                 .credentials(creds.identity, creds.credential).buildView(AbiquoContext.class);
         try {
            for (NodeMetadata nodeMetadata : computeServiceContext.getComputeService().listNodesDetailsMatching(
                    Predicates.<ComputeMetadata>notNull())) {
               instances.add(Instance.builder().id(nodeMetadata.getId()).provider("abiquo")
                       .region(nodeMetadata.getLocation().getDescription()).type(nodeMetadata.getType().name())
                       .status(nodeMetadata.getStatus().name())
                       .name(nodeMetadata.getName())
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
   public void deleteNodes(String prefix) {
      List<Instance> instances = Lists.newArrayList();
      for (Credentials creds : credentials) {
         ComputeServiceContext computeServiceContext = ContextBuilder.newBuilder("abiquo")
                 .endpoint("http://vdcbridge.interoute.com/jclouds/api/")
                 .credentials(creds.identity, creds.credential).buildView(AbiquoContext.class);
         try {
            computeServiceContext.getComputeService()
                    .destroyNodesMatching(Predicates.<NodeMetadata>and(not(TERMINATED), groupStartsWith(prefix)));
            for (VirtualAppliance virtualAppliance : ((AbiquoContext) computeServiceContext).getCloudService()
                    .listVirtualAppliances()) {
               final String virtualApplianceName = virtualAppliance.getName();
               if (virtualApplianceName.startsWith(prefix) && virtualAppliance.getState() ==
                       VirtualApplianceState.NOT_DEPLOYED) {
                  instances.add(Instance.builder().id(virtualAppliance.getId().toString()).provider("abiquo")
                          .region(virtualAppliance.getVirtualDatacenter().getName())
                          .status(virtualAppliance.getState().name())
                          .build());
                  log.debug("Deleting '{}' ...", virtualApplianceName);
                  virtualAppliance.delete();
                  log.debug("Deleted '{}'", virtualApplianceName);
               }
            }
         } catch (Exception e) {
            throw Throwables.propagate(e);
         } finally {
            computeServiceContext.close();
         }
      }
   }

}