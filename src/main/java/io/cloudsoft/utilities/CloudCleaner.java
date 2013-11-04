package io.cloudsoft.utilities;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.not;
import static com.google.common.base.Strings.emptyToNull;
import static org.jclouds.compute.predicates.NodePredicates.TERMINATED;

import com.abiquo.server.core.cloud.VirtualApplianceState;
import io.cloudsoft.utilities.io.cloudsoft.utilities.model.Instance;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.cloudsoft.utilities.predicates.TagPredicates;
import org.jclouds.ContextBuilder;
import org.jclouds.abiquo.AbiquoContext;
import org.jclouds.abiquo.domain.cloud.VirtualAppliance;
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

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Module;
import com.ibm.cloud.api.rest.client.DeveloperCloud;
import com.ibm.cloud.api.rest.client.DeveloperCloudClient;

/**
 * Utility to detect long-running VMs, and to kill them
 *
 * @author Andrea Turli
 * @version 0.1.0-SNAPSHOT
 */
public class CloudCleaner {

    private static final Logger LOG = LoggerFactory.getLogger(CloudCleaner.class);
    public static final String STATUS = "STATUS";
    /**
     * AWS EC2.
     */
    private static final String AWS_PROVIDER = "aws-ec2";
    /**
     * Rackspace Cloud Servers UK.
     */
    private static final String RACKSPACE_UK_PROVIDER = "rackspace-cloudservers-uk";
    /**
     * Rackspace Cloud Servers US.
     */
    private static final String RACKSPACE_US_PROVIDER = "rackspace-cloudservers-us";
    /**
     * Softlayer
     */
    private static final String SOFTLAYER_PROVIDER = "softlayer";
    /**
     * Hp Cloud Compute.
     */
    private static final String HPCLOUD_PROVIDER = "hpcloud-compute";
    /**
     * Google Cloud Compute
     */
    private static final String GOOGLE_COMPUTE_ENGINE_PROVIDER = "google-compute-engine";
    /**
     * IBM Smart Cloud Enterprise.
     */
    private static final String IBM_SCE_PROVIDER = "ibm-sce-compute";
    /**
     * Interoute.
     */
    private static final String INTEROUTE_PROVIDER = "interoute";
    private static final List<String> SUPPORTED_PROVIDERS =
            ImmutableList.of(/*GOOGLE_COMPUTE_ENGINE_PROVIDER,*/ SOFTLAYER_PROVIDER, AWS_PROVIDER,
                    HPCLOUD_PROVIDER, RACKSPACE_UK_PROVIDER, RACKSPACE_US_PROVIDER, INTEROUTE_PROVIDER);
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
        LIST, TAG_AND_CLEANUP, DESTROY
    }

    /**
     * Initialise the region and regular expressions.
     */
    public CloudCleaner(Map<String, List<String>> credentials) {
        this.credentials = credentials;
    }

    private static Map<String, List<String>> getCredentials(Optional<List<String>> providers) throws IOException {
        Map<String, List<String>> credentials = Maps.newLinkedHashMap();

        if (!providers.isPresent()) {
            putProviderAndItsCredentials(credentials, SUPPORTED_PROVIDERS);
        } else {
            putProviderAndItsCredentials(credentials, providers.get());
        }
        return credentials;
    }

    private static void putProviderAndItsCredentials(Map<String, List<String>> credentials,
                                                     List<String> providers) throws IOException {
        for (String provider : providers) {
            String identity = checkNotNull(System.getProperty(provider + "." + IDENTITY_PROPERTY), IDENTITY_PROPERTY + " for " + provider);
            String credential = checkNotNull(System.getProperty(provider + "." + CREDENTIAL_PROPERTY), CREDENTIAL_PROPERTY + " for " + provider);
            if (Boolean.getBoolean("showCredentialsInLog")) {
                LOG.info("provider({}) - identity({}), credential({})", provider, identity, credential);
            } else {
                LOG.info("Found credentials for provider({}) - identity({})", provider, identity);
            }
            credentials.put(provider, ImmutableList.of(identity, credential));
        }
    }

    private void destroy(Optional<List<String>> optionalProviders, String groupName) {
        if (!optionalProviders.isPresent()) {
            LOG.info("Detroy all nodes matching groupName: " + groupName);
            destroyNodes(SUPPORTED_PROVIDERS, groupName);
        } else {
            LOG.info("Detroy nodes of provider(s): {} matching groupName: {}", optionalProviders.get(), groupName);
            destroyNodes(optionalProviders.get(), groupName);
        }
    }

    private void destroyNodes(List<String> providers, String groupPrefix) {
        List<Instance> instances = Lists.newArrayList();
        for (String provider : providers) {
            if (provider.equals(INTEROUTE_PROVIDER)) {
                List<Instance> interouteInstancesDestroyed = destroyInterouteInstances(groupPrefix);
                instances.addAll(interouteInstancesDestroyed);
                printInstances(provider, interouteInstancesDestroyed);
            } else {
                throw new RuntimeException("Not supported api/provider: " + provider);
            }
        }
    }

    private List<Instance> destroyInterouteInstances(String groupPrefix) {
        List<Instance> instances = Lists.newArrayList();
        try {
            computeServiceContext = ContextBuilder.newBuilder("abiquo")
                    .endpoint("http://vdcbridge.interoute.com/jclouds/api/")
                    .credentials(credentials.get(INTEROUTE_PROVIDER).get(0), credentials.get(INTEROUTE_PROVIDER)
                            .get(1)).buildView(AbiquoContext.class);

            computeServiceContext.getComputeService().destroyNodesMatching(Predicates
                    .<NodeMetadata>and(not(TERMINATED), groupStartsWith(groupPrefix)));

            for (VirtualAppliance virtualAppliance : ((AbiquoContext) computeServiceContext).getCloudService()
                    .listVirtualAppliances()) {
                if (virtualAppliance.getName().startsWith(groupPrefix) && virtualAppliance.getState() == VirtualApplianceState.NOT_DEPLOYED) {
                    instances.add(Instance.builder().id(virtualAppliance.getId().toString()).provider(INTEROUTE_PROVIDER)
                            .region(virtualAppliance.getVirtualDatacenter().getName())
                            .status(virtualAppliance.getState().name())
                            .build());
                    virtualAppliance.delete();
                }
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            computeServiceContext.close();
        }
        return instances;
    }

    private Predicate<? super NodeMetadata> groupStartsWith(final String groupPrefix) {
        checkNotNull(emptyToNull(groupPrefix), "groupPrefix must be defined");
        return new Predicate<NodeMetadata>() {
            @Override
            public boolean apply(NodeMetadata nodeMetadata) {
                return nodeMetadata.getGroup().startsWith(groupPrefix);
            }

            @Override
            public String toString() {
                return "groupStartsWith(" + groupPrefix + ")";
            }
        };
    }

    private void listInstances(Optional<List<String>> optionalProviders) throws Exception {
        if (!optionalProviders.isPresent()) {
            LOG.info("List all nodes and their tags");
            retrieveAndPrintInstances(SUPPORTED_PROVIDERS);
        } else {
            LOG.info("List nodes of provider(s): {} with tags", optionalProviders.get());
            retrieveAndPrintInstances(optionalProviders.get());
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
            if (provider.equals(AWS_PROVIDER)) {
                List<Instance> ec2Instances = listEc2Instances(and(TagPredicates.isInstance()));
                instances.addAll(ec2Instances);
                printInstances(provider, ec2Instances);
            } else if (provider.equals(RACKSPACE_UK_PROVIDER) || provider.equals(RACKSPACE_US_PROVIDER)
                    || provider.equals(HPCLOUD_PROVIDER)) {
                List<Instance> openstackInstances = listOpenstackInstances(provider);
                instances.addAll(openstackInstances);
                printInstances(provider, openstackInstances);
            } else if (provider.equals(SOFTLAYER_PROVIDER)) {
                List<Instance> softlayerInstances = listSoftlayerInstances();
                instances.addAll(softlayerInstances);
                printInstances(provider, softlayerInstances);
            } else if (provider.equals(GOOGLE_COMPUTE_ENGINE_PROVIDER)) {
                List<Instance> googleComputeInstances = listGoogleComputeEngineInstances();
                instances.addAll(googleComputeInstances);
                printInstances(provider, googleComputeInstances);
            } else if (provider.equals(IBM_SCE_PROVIDER)) {
                List<Instance> ibmSceInstances = listIbmSceInstances();
                instances.addAll(ibmSceInstances);
                printInstances(provider, ibmSceInstances);
            } else if (provider.equals(INTEROUTE_PROVIDER)) {
                List<Instance> interouteInstances = listInterouteInstances();
                instances.addAll(interouteInstances);
                printInstances(provider, interouteInstances);
            } else {
                throw new RuntimeException("Not supported api/provider: " + provider);
            }
        }
        return instances;
    }

    private List<Instance> listInterouteInstances() {
        List<Instance> instances = Lists.newArrayList();
        try {
            computeServiceContext = ContextBuilder.newBuilder("abiquo")
                    .endpoint("http://vdcbridge.interoute.com/jclouds/api/")
                    .credentials(credentials.get(INTEROUTE_PROVIDER).get(0), credentials.get(INTEROUTE_PROVIDER)
                            .get(1)).buildView(AbiquoContext.class);
            for (NodeMetadata nodeMetadata : computeServiceContext.getComputeService().listNodesDetailsMatching(
                    Predicates.<ComputeMetadata>notNull())) {
                instances.add(Instance.builder().id(nodeMetadata.getId()).provider(INTEROUTE_PROVIDER)
                        .region(nodeMetadata.getLocation().getDescription()).type(nodeMetadata.getType().name())
                        .status(nodeMetadata.getStatus().name())
                                // .keyName(nodeMetadata.getType())
                                // .uptime(new Date().getTime() -
                                // computeServiceContext.getComputeService().getTime())
                                // .tags(nodeMetadata.getTags())
                        .build());
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            computeServiceContext.close();
        }
        return instances;
    }

    private List<Instance> listIbmSceInstances() {
        List<Instance> instances = Lists.newArrayList();
        DeveloperCloudClient client = DeveloperCloud.getClient();
        client.setRemoteCredentials(credentials.get(IBM_SCE_PROVIDER).get(0), credentials.get(IBM_SCE_PROVIDER).get(1));
        try {
            for (com.ibm.cloud.api.rest.client.bean.Instance instance : client.describeInstances()) {
                instances.add(Instance.builder().id(instance.getID()).provider(IBM_SCE_PROVIDER)
                        .region(instance.getLocation()).type(instance.getInstanceType())
                        .status(instance.getStatus().toString())
                        .keyName(instance.getKeyName())
                        .uptime(new Date().getTime() - instance.getLaunchTime().getTime())
                                // .tags(nodeMetadata.getTags())
                        .build());
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        return instances;
    }

    private List<Instance> listGoogleComputeEngineInstances() {
        List<Instance> instances = Lists.newArrayList();
        try {
            computeServiceContext = getComputeServiceContext(GOOGLE_COMPUTE_ENGINE_PROVIDER);
            for (NodeMetadata nodeMetadata : computeServiceContext.getComputeService().listNodesDetailsMatching(
                    Predicates.<ComputeMetadata>notNull())) {
                instances.add(Instance.builder().id(nodeMetadata.getId()).provider(GOOGLE_COMPUTE_ENGINE_PROVIDER)
                        .region(nodeMetadata.getLocation().getDescription()).type(nodeMetadata.getType().name())
                        .status(nodeMetadata.getStatus().name())
                                // .keyName(nodeMetadata.getType())
                                // .uptime(new Date().getTime() -
                                // computeServiceContext.getComputeService().getTime())
                                // .tags(nodeMetadata.getTags())
                        .build());
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            computeServiceContext.close();
        }
        return instances;
    }

    private List<Instance> listSoftlayerInstances() {
        List<Instance> instances = Lists.newArrayList();
        try {
            computeServiceContext = getComputeServiceContext(SOFTLAYER_PROVIDER);
            RestContext<SoftLayerClient, SoftLayerAsyncClient> client = computeServiceContext.unwrap();
            SoftLayerClient api = client.getApi();
            for (VirtualGuest virtualGuest : api.getVirtualGuestClient().listVirtualGuests()) {
                instances.add(Instance.builder().id(virtualGuest.getUuid()).provider(SOFTLAYER_PROVIDER)
                        .region(virtualGuest.getDatacenter().getLongName()).type("Cloud Compute Instance")
                        .status(virtualGuest.getPowerState().toString()).keyName(virtualGuest.getAccountId() + "")
                        .uptime(new Date().getTime() - virtualGuest.getCreateDate().getTime())
                                // .tags()
                        .build());
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            computeServiceContext.close();
        }
        return instances;
    }

    private List<Instance> listOpenstackInstances(String provider) {
        List<Instance> instances = Lists.newArrayList();
        try {
            computeServiceContext = getComputeServiceContext(provider);
            RestContext<NovaApi, NovaAsyncApi> client = computeServiceContext.unwrap();
            for (String zone : client.getApi().getConfiguredZones()) {
                ServerApi serverApiForZone = client.getApi().getServerApiForZone(zone);
                FluentIterable<? extends Server> servers = serverApiForZone.listInDetail().concat();

                for (Server server : servers) {
                    serverApiForZone.getMetadata(server.getId());
                    instances.add(Instance.builder().id(server.getId()).provider(provider).region(zone)
                            .type(server.getFlavor().getId()).status(server.getStatus().toString())
                            .keyName(server.getKeyName()).uptime(new Date().getTime() - server.getCreated().getTime())
                            .tags(serverApiForZone.getMetadata(server.getId())).build());
                }
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            computeServiceContext.close();
        }
        return instances;
    }

    /**
     * TODO: filter all instances that are not `termination protection` flagged
     */
    private List<Instance> listEc2Instances(Predicate predicate) {
        List<Instance> instances = Lists.newArrayList();
        try {
            computeServiceContext = getComputeServiceContext(AWS_PROVIDER);
            RestContext<EC2Client, EC2AsyncClient> client = computeServiceContext.unwrap();
            for (String region : client.getApi().getConfiguredRegions()) {
                TagApi tagApi = client.getApi().getTagApiForRegion(region).get();
                Set<? extends Reservation<? extends RunningInstance>> reservations = getReservations(client, region);
                for (Reservation reservation : reservations) {
                    for (Object aReservation : reservation) {
                        RunningInstance instance = (RunningInstance) aReservation;
                        ImmutableList<Tag> filteredTags = tagApi.list().filter(predicate).toImmutableList();
                        Map<String, String> tags = Maps.newHashMap();
                        for (Tag tag : filteredTags) {
                            if (tag.getResourceId().equals(instance.getId())) {
                                tags.put(tag.getKey(), tag.getValue().orNull());
                            }
                        }
                        instances.add(Instance.builder().id(instance.getId()).provider(AWS_PROVIDER).region(region)
                                .type(instance.getInstanceType()).status(instance.getInstanceState().value())
                                .keyName(instance.getKeyName())
                                .uptime(new Date().getTime() - instance.getLaunchTime().getTime()).tags(tags).build());
                    }
                }
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            computeServiceContext.close();
        }
        return instances;
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

    private void addTagToEc2Instances(String tagValue) throws Exception {
        for (Instance instance : listEc2Instances(and(TagPredicates.isInstance()))) {
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
                        "with: " + tagApi.list().filter(TagPredicates.hasId(instance.getId())).toImmutableList());
                tagApi.deleteFromResources(ImmutableList.of(STATUS, LAST_RUN), ImmutableList.of(instance.getId()));
                LOG.info("Tags " + STATUS + ", " + LAST_RUN + " related to CloudCleaner have been removed");
            }
        }
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
        List<String> providers = Lists.newArrayList(argv);
        Optional<List<String>> optionalProviders = Optional.absent();

        String action = argv[0];
        String groupPrefix = null;
        if (action.equalsIgnoreCase(ACTION.DESTROY.toString())) {
            if (argv.length < 2) {
                LOG.error("Invalid options used with 'destroy': " + argv);
                showHelp();
                System.exit(0);
            } else {
                groupPrefix = argv[1];
                providers.remove(0);
                providers.remove(0);
                optionalProviders = Optional.of(providers);
                for (String provider : optionalProviders.get()) {
                    if (!SUPPORTED_PROVIDERS.contains(provider)) {
                        LOG.error("Provider {} is not supported.", provider);
                        System.exit(1);
                    }
                }
            }
        } else {
            if (argv.length > 1) {
                providers.remove(0);
                optionalProviders = Optional.of(providers);
                for (String provider : optionalProviders.get()) {
                    if (!SUPPORTED_PROVIDERS.contains(provider)) {
                        LOG.error("Provider {} is not supported.", provider);
                        System.exit(1);
                    }
                }
            }
        }

        // Initialise and then execute the cleanUp method
        CloudCleaner cleaner = new CloudCleaner(getCredentials(optionalProviders));
        if (action.equalsIgnoreCase(ACTION.LIST.toString())) {
            cleaner.listInstances(optionalProviders);
        } else if (action.equalsIgnoreCase(ACTION.DESTROY.toString())) {
            cleaner.destroy(optionalProviders, groupPrefix);
        } else if (action.equalsIgnoreCase(ACTION.TAG_AND_CLEANUP.toString())) {
            LOG.info("Tag all nodes and cleanUp, if needed");
            cleaner.tagAndCleanUp();
        }
    }

    protected static void showHelp() {
        System.out.println();
        System.out.println("Usage: <command> [ --port Port ] [ --location Location ] [ --class Class ]");
        System.out.println();
        System.out.println("where <command> is any of:");
        System.out.println("        LIST [ provider1, provider2 ]");
        System.out.println("        TAG_AND_CLEANUP  start the applicaton specified by the --class option");
        System.out.println("        DESTROY groupName [provider1, ..., providerN]      destroy all the nodes matching" +
                " this " +
                "groupName" +
                " in" +
                " all the " +
                "supported providers");
        System.out.println();
    }

}
