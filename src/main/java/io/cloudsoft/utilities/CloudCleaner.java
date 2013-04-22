package io.cloudsoft.utilities;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.inject.Module;
import org.jclouds.Context;
import org.jclouds.ContextBuilder;
import org.jclouds.aws.ec2.AWSEC2Client;
import org.jclouds.aws.ec2.services.AWSInstanceClient;
import org.jclouds.collect.IterableWithMarker;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.ec2.EC2AsyncClient;
import org.jclouds.ec2.EC2Client;
import org.jclouds.ec2.domain.InstanceState;
import org.jclouds.ec2.domain.Reservation;
import org.jclouds.ec2.domain.RunningInstance;
import org.jclouds.ec2.domain.Tag;
import org.jclouds.ec2.features.TagApi;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.NovaAsyncApi;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.jclouds.openstack.v2_0.domain.Resource;
import org.jclouds.rest.RestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.and;
import static java.lang.String.format;

/**
 * Utility to detect long-running VMs, and to kill them
 *
 * @author Andrea Turli
 * @version 0.1.0-SNAPSHOT
 */
public class CloudCleaner {

   public static final String STATUS = "STATUS";
   private static final Logger LOG = LoggerFactory.getLogger(CloudCleaner.class);
   /**
    * AWS EC2.
    */
   private static final String AWS_PROVIDER = "aws-ec2";
   /**
    * Rackspace Cloud Servers UK.
    */
   private static final String RACKSPACE_PROVIDER = "rackspace-cloudservers-uk";
   /**
    * System property for access key.
    */
   private static final String IDENTITY_PROPERTY = "identity";
   /**
    * System property for secret key.
    */
   private static final String CREDENTIAL_PROPERTY = "credential";
   private final Map<String, List<String>> credentials;
   private final List<String> supportedProviders = ImmutableList.of(AWS_PROVIDER, RACKSPACE_PROVIDER);
   private ComputeServiceContext computeServiceContext;

   /**
    * Initialise the region and regular expressions.
    */
   public CloudCleaner(Map<String, List<String>> credentials) {
      this.credentials = credentials;
   }

   /**
    * Command-line entry point.
    * <p/>
    * See {@code README.md} for usage example.
    */
   public static void main(String... argv) throws Exception {
      if (argv.length != 1) {
         System.exit(1);
      }
      Map<String, List<String>> credentials = Maps.newLinkedHashMap();
      credentials.put(AWS_PROVIDER, ImmutableList.of(
              System.getProperty(AWS_PROVIDER + "." + IDENTITY_PROPERTY),
              System.getProperty(AWS_PROVIDER + "." + CREDENTIAL_PROPERTY)));
      credentials.put(RACKSPACE_PROVIDER, ImmutableList.of(
              System.getProperty(RACKSPACE_PROVIDER + "." + IDENTITY_PROPERTY),
              System.getProperty(RACKSPACE_PROVIDER + "." + CREDENTIAL_PROPERTY)));
      // Initialise and then execute the cleanUp method
      CloudCleaner cleaner = new CloudCleaner(credentials);
      if (argv[0].equalsIgnoreCase(ACTION.LIST.toString())) {
         LOG.info("List all nodes and their tags");
         cleaner.listAllNodes();
      } else if (argv[0].equalsIgnoreCase(ACTION.TAG_AND_CLEANUP.toString()))
         cleaner.cleanUp();
   }

   private void listAllNodes() throws Exception {
      for (String provider : supportedProviders) {
         LOG.info("==================================================================================================");
         LOG.info("  PROVIDER '{}'", provider);
         LOG.info("==================================================================================================");
         computeServiceContext = getComputeServiceContext(provider);
         try {
            if (provider.equals(AWS_PROVIDER)) {
               listEc2Instances(and(TagPredicates.isInstance()));
            } else if (provider.equals(RACKSPACE_PROVIDER)) {
               listRackspaceInstances();
            }
         } finally {
            computeServiceContext.close();
         }
      }
   }

   private void listEc2Instances(Predicate predicate) throws Exception {
      RestContext<EC2Client, EC2AsyncClient> client = computeServiceContext.unwrap();
      for (String region : client.getApi().getConfiguredRegions()) {
         TagApi tagApi = client.getApi().getTagApiForRegion(region).get();
         LOG.info("--------------------------------------------------------------------------------------------------");
         LOG.info("  Region:'{}'", region);
         LOG.info("---------------------------------------------------------------------------------------------------");
         Set<? extends Reservation<? extends RunningInstance>> reservations = getReservations(client, region);
         for (Reservation reservation : reservations) {
            Iterator itr = reservation.iterator();
            while (itr.hasNext()) {
               RunningInstance instance = (RunningInstance) itr.next();
               ImmutableList<Tag> filteredTags = tagApi.list().filter(predicate).toList();
               Map<String, String> tags = Maps.newHashMap();
               for(Tag tag : filteredTags) {
                  if(tag.getResourceId().equals(instance.getId())) {
                     tags.put(tag.getKey(), tag.getValue().orNull());
                  }
               }
               long milliseconds = new Date().getTime() - instance.getLaunchTime().getTime();
               printInstanceDetails(instance.getId(), instance.getInstanceType(), instance.getInstanceState().value(),
                       instance.getKeyName(),
                       tags, milliseconds);
            }
         }
      }
   }

   private void listRackspaceInstances() throws Exception {
      RestContext<NovaApi, NovaAsyncApi> client = computeServiceContext.unwrap();
      for (String zone : client.getApi().getConfiguredZones()) {
         ServerApi serverApiForZone = client.getApi().getServerApiForZone(zone);
         ImmutableList<? extends IterableWithMarker<? extends Resource>> iterableWithMarkers =
                 serverApiForZone.list().toList();

         for (IterableWithMarker<? extends Resource> iterableWithMarker : iterableWithMarkers) {
            String instanceId = iterableWithMarker.get(0).getId();
            Server server = serverApiForZone.get(instanceId);
            long milliseconds = new Date().getTime() - server.getCreated().getTime();
            Map<String, String> metadata = serverApiForZone.getMetadata(instanceId);
            printInstanceDetails(instanceId, server.getFlavor().getId(), server.getStatus().toString(),
                    server.getKeyName(), metadata, milliseconds);
         }
      }
   }

   private void printInstanceDetails(String instanceId, String instanceType, String status, String keyName, Map<String,
           String> tags, long milliseconds) {
      LOG.info("\t* Instance '{}' [type:'{}', status:'{}', keyName:'{}'] has an uptime of {} days ({} hours)",
              instanceId,
              instanceType,
              status,
              keyName,
              TimeUnit.MILLISECONDS.toDays(milliseconds),
              TimeUnit.MILLISECONDS.toHours(milliseconds));
      for (String key : tags.keySet()) {
         LOG.info("\t\t> Tag: {}={}", key, tags.get(key));
      }
   }

   /**
    * Perform the cleanup.
    */
   public void cleanUp() throws Exception {
      for (String provider : supportedProviders) {
         LOG.info("============== Provider:'{}' ==============", provider);
         try {
            computeServiceContext = getComputeServiceContext(provider);
            ComputeService computeService = computeServiceContext.getComputeService();
            /*
            if (provider.equalsIgnoreCase(AWS_PROVIDER)) {
               //addTagToInstance("i-e64216ac", "long-running");
               //tagInstancesAsDeletable();
               listInstancesWithTagAboutStatus();
            } else if (provider.equalsIgnoreCase(RACKSPACE_PROVIDER)) {
               cleanUpRackspace();
            }
            */
         } finally {
            computeServiceContext.close();
         }
      }
   }

   private void listNodesWithUserMetadata(ComputeService computeService) {
      for (ComputeMetadata node : computeService.listNodes()) {
         LOG.info("Instance '{}' has an uptime of {} days ({} hours)", node.getId(), node.getName());
         for (String key : node.getUserMetadata().keySet()) {
            LOG.info("Tag:\n{}={}\n", key, node.getUserMetadata().get(key));
         }
      }
   }

   private void tagInstancesAsDeletable() throws Exception {
      RestContext<EC2Client, EC2AsyncClient> client = computeServiceContext.unwrap();
      for (String region : client.getApi().getConfiguredRegions()) {
         Set<? extends Reservation<? extends RunningInstance>> reservations = getReservations(client, region);
         for (Reservation reservation : reservations) {
            Iterator itr = reservation.iterator();
            while (itr.hasNext()) {
               RunningInstance instance = (RunningInstance) itr.next();
               applyTagToInstance(instance, TAG_VALUE.DELETABLE.toString(), client, region, reservations);
            }
         }
      }
   }

   private void addTagToInstance(final RunningInstance instance, String tagValue) throws Exception {
      RestContext<EC2Client, EC2AsyncClient> client = computeServiceContext.unwrap();
      for (String region : client.getApi().getConfiguredRegions()) {
         Set<? extends Reservation<? extends RunningInstance>> reservations = getReservations(client, region);
         applyTagToInstance(instance, tagValue, client, region, reservations);
      }
   }

   private void applyTagToInstance(RunningInstance instance, String tagValue, RestContext<EC2Client,
           EC2AsyncClient> client, String region, Set<? extends Reservation<? extends RunningInstance>> reservations) {
      long milliseconds = new Date().getTime() - instance.getLaunchTime().getTime();
      if (TimeUnit.MILLISECONDS.toHours(milliseconds) > 72l) {
         if (instance.getInstanceState().equals(InstanceState.RUNNING) && isInstanceInsideRegion(reservations,
                 instance.getId(), client,
                 region)) {
            TagApi tagApi = client.getApi().getTagApiForRegion(region).get();
            if (tagApi != null) {
               Date now = new Date();
               tagApi.applyToResources(ImmutableMap.of(STATUS, tagValue, "LAST-RUN",
                       now.toString()),
                       ImmutableList.of(instance.getId()));
               LOG.info("Instance '{}' tagged as '{}' ({}) because it has an uptime of {} days ({} hours)",
                       instance.getId(),
                       tagValue,
                       now.toString(),
                       TimeUnit.MILLISECONDS.toDays(milliseconds),
                       TimeUnit.MILLISECONDS.toHours(milliseconds));
               //tagApi.deleteFromResources(ImmutableList.of(STATUS, "LAST-RUN"), ImmutableList.of(instance.getId()));
            }
         }
      }
   }

   private boolean isInstanceInsideRegion(Set<? extends Reservation<? extends RunningInstance>> reservations,
                                          final String instanceId, RestContext<EC2Client, EC2AsyncClient> client, String region) {
      return Iterables.size(Iterables.filter(reservations, new Predicate<Reservation>() {
         @Override
         public boolean apply(Reservation input) {
            return !Iterables.isEmpty(Iterables.filter(input, new Predicate<RunningInstance>() {

               @Override
               public boolean apply(RunningInstance input) {
                  return input.getId().equals(instanceId);
               }
            }
            ));
         }
      })) > 0;
   }

   private Set<? extends Reservation<? extends RunningInstance>> getReservations(RestContext<EC2Client, EC2AsyncClient> client, String region) {
      AWSInstanceClient instanceClient = AWSEC2Client.class.cast(client.getApi()).getInstanceServices();
      return instanceClient.describeInstancesInRegion(region);
   }

   private void cleanUpRackspace() throws Exception {
      RestContext<NovaApi, NovaAsyncApi> client = computeServiceContext.unwrap();
      for (String zone : client.getApi().getConfiguredZones()) {
         ServerApi serverApiForZone = client.getApi().getServerApiForZone(zone);
         ImmutableList<? extends IterableWithMarker<? extends Resource>> iterableWithMarkers =
                 serverApiForZone.list().toList();

         for (IterableWithMarker<? extends Resource> iterableWithMarker : iterableWithMarkers) {
            String instanceId = iterableWithMarker.get(0).getId();
            serverApiForZone.setMetadata(instanceId, ImmutableMap.of(STATUS, TAG_VALUE.DELETABLE.toString()));
            Map<String, String> metadata = serverApiForZone.getMetadata(instanceId);
            for (String key : metadata.keySet()) {
               LOG.info("{}={}", key, metadata.get(key));
            }
         }
      }
   }

   /**
    * Create a jclouds {@link RestContext} to access the Rackspace API.
    */
   public ComputeServiceContext getComputeServiceContext(String provider) throws Exception {
      ImmutableSet<Module> modules = ImmutableSet.<Module>of(new SLF4JLoggingModule());
      ContextBuilder builder = ContextBuilder.newBuilder(provider)
              .credentials(credentials.get(provider).get(0), credentials.get(provider).get(1))
              .modules(modules);
      return builder.buildView(ComputeServiceContext.class);
   }

   private enum TAG_VALUE {
      DELETABLE
   }

   private enum ACTION {
      LIST, TAG_AND_CLEANUP
   }

}
