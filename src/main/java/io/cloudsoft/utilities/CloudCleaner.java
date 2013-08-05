package io.cloudsoft.utilities;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Module;
import io.cloudsoft.utilities.io.cloudsoft.utilities.model.Instance;
import org.jclouds.ContextBuilder;
import org.jclouds.aws.ec2.AWSEC2Client;
import org.jclouds.aws.ec2.services.AWSInstanceClient;
import org.jclouds.collect.IterableWithMarker;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;
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
import org.jclouds.softlayer.SoftLayerAsyncClient;
import org.jclouds.softlayer.SoftLayerClient;
import org.jclouds.softlayer.domain.VirtualGuest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.and;

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
   private static final String RACKSPACE_UK_PROVIDER = "rackspace-cloudservers-uk";
   /**
    * Softlayer
    */
   private static final String SOFTLAYER_PROVIDER = "softlayer";
   /**
    * Hp Cloud Compute.
    */
   private static final String HPCLOUD_PROVIDER = "hpcloud-compute";

   private static final List<String> SUPPORTED_PROVIDERS = ImmutableList.of(SOFTLAYER_PROVIDER, AWS_PROVIDER,
           RACKSPACE_UK_PROVIDER, HPCLOUD_PROVIDER);
   /**
    * System property for access key.
    */
   private static final String IDENTITY_PROPERTY = "identity";

   /**
    * System property for secret key.
    */
   private static final String CREDENTIAL_PROPERTY = "credential";
   private static final String LONG_RUNNING = "LONG-RUNNING";
   private static final String LAST_RUN = "LAST-RUN";
   private final Map<String, List<String>> credentials;
   private ComputeServiceContext computeServiceContext;

   private enum TAG_VALUE {
      DELETABLE
   }

   private enum ACTION {
      LIST, TAG_AND_CLEANUP
   }
   /**
    * Initialise the region and regular expressions.
    */
   public CloudCleaner(Map<String, List<String>> credentials) {
      this.credentials = credentials;
   }

   private void listInstances(Optional<String> provider) throws Exception {
      if (!provider.isPresent()) {
         LOG.info("List all nodes and their tags");
         retrieveAndPrintInstances(SUPPORTED_PROVIDERS);
      } else {
         LOG.info("List nodes from provider: {} and its tags", provider.get());
         retrieveAndPrintInstances(ImmutableList.of(provider.get()));
      }
   }

   private void printInstances(String provider, List<Instance> instances) {
      LOG.info("==================================================================================================");
      LOG.info("  PROVIDER '{}'", provider);
      LOG.info("==================================================================================================");
      for (Instance instance : instances) {
         LOG.info(instance.toString());
      }
   }

   private List<Instance> retrieveAndPrintInstances(List<String> providers) throws Exception {
      List<Instance> instances = Lists.newArrayList();
      for (String provider : providers) {
         try {
            computeServiceContext = getComputeServiceContext(provider);
            if (provider.equals(AWS_PROVIDER)) {
               List<Instance> ec2Instances = listEc2Instances(and(TagPredicates.isInstance()));
               instances.addAll(ec2Instances);
               printInstances(provider, ec2Instances);
            } else if (provider.equals(RACKSPACE_UK_PROVIDER)) {
               List<Instance> rackspaceUkInstances = listOpenstackInstances(RACKSPACE_UK_PROVIDER);
               instances.addAll(rackspaceUkInstances);
               printInstances(provider, rackspaceUkInstances);
            } else if (provider.equals(HPCLOUD_PROVIDER)) {
               List<Instance> hpCloudInstances = listOpenstackInstances(HPCLOUD_PROVIDER);
               instances.addAll(hpCloudInstances);
               printInstances(provider, hpCloudInstances);
            } else if (provider.equals(SOFTLAYER_PROVIDER)) {
               List<Instance> softlayerInstances = listSoftlayerInstances();
               instances.addAll(softlayerInstances);
               printInstances(provider, softlayerInstances);
            } else {
               throw new RuntimeException("Not supported api/provider: " + provider);
            }
         } finally {
            computeServiceContext.close();
         }
      }
      return instances;
   }

   private List<Instance>  listSoftlayerInstances() {
      List<Instance> instances = Lists.newArrayList();
      RestContext<SoftLayerClient, SoftLayerAsyncClient> client = computeServiceContext.unwrap();
      SoftLayerClient api = client.getApi();
      for(VirtualGuest virtualGuest :  api.getVirtualGuestClient().listVirtualGuests()) {
         instances.add(Instance.builder()
                 .id(virtualGuest.getUuid())
                 .provider(SOFTLAYER_PROVIDER)
                 .region(virtualGuest.getDatacenter().getLongName())
                 .type("Cloud Compute Instance")
                 .status(virtualGuest.getPowerState().toString())
                 .keyName(virtualGuest.getAccountId() + "")
                 .uptime(new Date().getTime() - virtualGuest.getCreateDate().getTime())
                 //.tags()
                 .build());
      }

      return instances;
   }

   private List<Instance> listOpenstackInstances(String provider) {
      List<Instance> instances = Lists.newArrayList();
      RestContext<NovaApi, NovaAsyncApi> client = computeServiceContext.unwrap();
      for (String zone : client.getApi().getConfiguredZones()) {
         ServerApi serverApiForZone = client.getApi().getServerApiForZone(zone);
         FluentIterable<? extends Server> servers = serverApiForZone.listInDetail().concat();

         for (Server server: servers) {
            serverApiForZone.getMetadata(server.getId());
            instances.add(Instance.builder()
                    .id(server.getId())
                    .provider(provider)
                    .region(zone)
                    .type(server.getFlavor().getId())
                    .status(server.getStatus().toString())
                    .keyName(server.getKeyName())
                    .uptime(new Date().getTime() - server.getCreated().getTime())
                    .tags(serverApiForZone.getMetadata(server.getId()))
                    .build());
         }
      }
      return instances;
   }

   /** TODO: filter all instances that are not `termination protection` flagged */
   private List<Instance> listEc2Instances(Predicate predicate) throws Exception {
      List<Instance> instances = Lists.newArrayList();
      RestContext<EC2Client, EC2AsyncClient> client = computeServiceContext.unwrap();
      for (String region : client.getApi().getConfiguredRegions()) {
         TagApi tagApi = client.getApi().getTagApiForRegion(region).get();
         Set<? extends Reservation<? extends RunningInstance>> reservations = getReservations(client, region);
         for (Reservation reservation : reservations) {
            for (Object aReservation : reservation) {
               RunningInstance instance = (RunningInstance) aReservation;
               ImmutableList<Tag> filteredTags = tagApi.list().filter(predicate).toList();
               Map<String, String> tags = Maps.newHashMap();
               for (Tag tag : filteredTags) {
                  if (tag.getResourceId().equals(instance.getId())) {
                     tags.put(tag.getKey(), tag.getValue().orNull());
                  }
               }
               instances.add(Instance.builder()
                       .id(instance.getId())
                       .provider(AWS_PROVIDER)
                       .region(region)
                       .type(instance.getInstanceType())
                       .status(instance.getInstanceState().value())
                       .keyName(instance.getKeyName())
                       .uptime(new Date().getTime() - instance.getLaunchTime().getTime())
                       .tags(tags)
                       .build());
            }
         }
      }
      return instances;
   }

   /**
    * Perform the tag and cleanup.
    */
   public void tagAndCleanUp() throws Exception {
      for (String provider : SUPPORTED_PROVIDERS) {
         LOG.info("============== Provider:'{}' ==============", provider);
         computeServiceContext = getComputeServiceContext(provider);
         try {
            if (provider.equalsIgnoreCase(AWS_PROVIDER)) {
                addTagToEc2Instances(LONG_RUNNING);
               //tagInstancesAsDeletable();
               //listInstancesWithTagAboutStatus();
            } else if (provider.equalsIgnoreCase(RACKSPACE_UK_PROVIDER)) {
               cleanUpRackspace();
            }
         } finally {
            computeServiceContext.close();
         }
      }
   }

   /*
   private void tagInstancesAsDeletable() throws Exception {
      RestContext<EC2Client, EC2AsyncClient> client = computeServiceContext.unwrap();
      for (String region : client.getApi().getConfiguredRegions()) {
         Set<? extends Reservation<? extends RunningInstance>> reservations = getReservations(client, region);
         for (Reservation reservation : reservations) {
            Iterator itr = reservation.iterator();
            while (itr.hasNext()) {
               RunningInstance instance = (RunningInstance) itr.next();
               applyTagToInstance(instance, TAG_VALUE.DELETABLE.toString(), client, region, reservations, 72l);
            }
         }
      }
   }
   */

   private void addTagToEc2Instances(String tagValue) throws Exception {
      for(Instance instance : listEc2Instances(and(TagPredicates.isInstance()))) {
         applyTagToInstance(instance, tagValue, instance.getRegion(), 72l);
      }
   }

   private void applyTagToInstance(Instance instance, String tagValue, String region, long threshold) {
      if (TimeUnit.MILLISECONDS.toHours(instance.getUptime()) > threshold && instance.getStatus().equals
              (InstanceState.RUNNING.value())) {
         RestContext<EC2Client, EC2AsyncClient> client = computeServiceContext.unwrap();
         TagApi tagApi = client.getApi().getTagApiForRegion(region).get();
         if (tagApi != null) {
            ImmutableMap<String, String> tags = ImmutableMap.of(STATUS, tagValue, LAST_RUN, new Date().toString());
            tagApi.applyToResources(tags, ImmutableList.of(instance.getId()));
            LOG.info(Instance.builder().fromInstance(instance).tags(tags).build().toString() + " has been tagged " +
                    "with: " + tagApi.list().filter(TagPredicates.hasId(instance.getId())).toList());
            tagApi.deleteFromResources(ImmutableList.of(STATUS, LAST_RUN), ImmutableList.of(instance.getId()));
            LOG.info("Tags " + STATUS + ", " + LAST_RUN + " related to CloudCleaner have been removed");
         }
      }
   }

   private boolean isInstanceInsideRegion(Set<? extends Reservation<? extends RunningInstance>> reservations,
                                          final String instanceId) {
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
                 serverApiForZone.list().toImmutableList();

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
    * Create a jclouds {@link RestContext} to access the Compute API.
    */
   public ComputeServiceContext getComputeServiceContext(String provider) throws Exception {
      ImmutableSet<Module> modules = ImmutableSet.<Module>of(new SLF4JLoggingModule());
      ContextBuilder builder = ContextBuilder.newBuilder(provider)
              .credentials(credentials.get(provider).get(0), credentials.get(provider).get(1))
              .modules(modules);
      return builder.buildView(ComputeServiceContext.class);
   }

   /**
    * Command-line entry point.
    * <p/>
    * See {@code README.md} for usage example.
    */
   public static void main(String... argv) throws Exception {
      if (argv.length < 1) {
         System.exit(1);
      }
      Optional<String> provider = Optional.absent();
      if(argv.length == 2) {
         provider =  Optional.of(argv[1]);
         if(!SUPPORTED_PROVIDERS.contains(provider.get())) {
            LOG.error("Provider {} is not supported.", provider.get());
            System.exit(1);
         }
      }
      String action = argv[0];

      // Initialise and then execute the cleanUp method
      CloudCleaner cleaner = new CloudCleaner(getCredentials(provider));
      if (action.equalsIgnoreCase(ACTION.LIST.toString())) {
         cleaner.listInstances(provider);
      } else if (action.equalsIgnoreCase(ACTION.TAG_AND_CLEANUP.toString())) {
         LOG.info("Tag all nodes and cleanUp, if needed");
         cleaner.tagAndCleanUp();
      }
   }

   private static Map<String, List<String>> getCredentials(Optional<String> provider) {
      Map<String, List<String>> credentials = Maps.newLinkedHashMap();

      if (!provider.isPresent()) {
         for(String supportedProvider : SUPPORTED_PROVIDERS) {
            putProviderAndItsCredentials(credentials, supportedProvider);
         }
      } else {
         LOG.info("Adding credentials to provider: {}", provider.get());
         putProviderAndItsCredentials(credentials, provider.get());
      }
      return credentials;
   }

   private static void putProviderAndItsCredentials(Map<String, List<String>> credentials, String provider) {
      credentials.put(provider, ImmutableList.of(
              checkNotNull(System.getProperty(provider + "." + IDENTITY_PROPERTY), IDENTITY_PROPERTY),
              checkNotNull(System.getProperty(provider + "." + CREDENTIAL_PROPERTY), CREDENTIAL_PROPERTY)
      ));
   }

}