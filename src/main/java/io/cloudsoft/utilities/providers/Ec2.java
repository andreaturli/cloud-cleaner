package io.cloudsoft.utilities.providers;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.cloudsoft.utilities.io.cloudsoft.utilities.model.Instance;
import io.cloudsoft.utilities.predicates.TagPredicates;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.domain.Credentials;
import org.jclouds.ec2.EC2Api;
import org.jclouds.ec2.domain.InstanceState;
import org.jclouds.ec2.domain.Reservation;
import org.jclouds.ec2.domain.RunningInstance;
import org.jclouds.ec2.domain.Tag;
import org.jclouds.ec2.features.TagApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Predicates.and;

class Ec2 extends BasicProvider {

   private static final Logger log = LoggerFactory.getLogger(Ec2.class);
   private static final String AWS_EC2 = "aws-ec2";

   private EC2Api api;

   public Ec2(Set<Credentials> credentials) {
      super(credentials);
   }

   @Override
   public String getName() {
      return AWS_EC2;
   }

   @Override
   public List<Instance> listInstances() {
      List<Instance> instances = Lists.newArrayList();
      for (Credentials creds : credentials) {
         ComputeServiceContext computeServiceContext = getComputeServiceContext(getName(), creds.identity, creds.credential);
         try {
            api = computeServiceContext.unwrapApi(EC2Api.class);
            for (String region : api.getConfiguredRegions()) {
               TagApi tagApi = api.getTagApiForRegion(region).get();
               Set<? extends Reservation<? extends RunningInstance>> reservations = getReservations(api, region);
               for (Reservation reservation : reservations)
                  for (Object aReservation : reservation) {
                     RunningInstance instance = (RunningInstance) aReservation;
                     ImmutableList<Tag> filteredTags = tagApi.list().filter(and(TagPredicates.isInstance())).toList();
                     Map<String, String> tags = Maps.newHashMap();
                     for (Tag tag : filteredTags) {
                        if (tag.getResourceId().equals(instance.getId())) {
                           tags.put(tag.getKey(), tag.getValue().orNull());
                        }
                     }
                     instances.add(Instance.builder().id(instance.getId())
                             .name(instance.getDnsName())
                             .provider(getName()).region(region)
                             .type(instance.getInstanceType()).status(instance.getInstanceState().value())
                             .keyName(instance.getKeyName())
                             .uptime(new Date().getTime() - instance.getLaunchTime().getTime())
                             .tags(tags)
                             .build());
                  }
            }
         } catch (Exception e) {
            throw Throwables.propagate(e);
         } finally {
            computeServiceContext.close();
         }
      }
      return instances;
   }

   private Set<? extends Reservation<? extends RunningInstance>> getReservations(EC2Api api, String region) {
      if(api.getInstanceApiForRegion(region).isPresent()) {
         return api.getInstanceApiForRegion(region).get().describeInstancesInRegion(region);
      } else
         return ImmutableSet.of();
   }

   @Override
   public void tagAndCleanInstances(String tag) throws Exception {
      for (Instance instance : listInstances()) {
         applyTagToInstance(instance, tag, instance.getRegion(), 72l);
      }
   }

   private void applyTagToInstance(Instance instance, String tagValue, String region, long threshold) throws Exception {
      if (TimeUnit.MILLISECONDS.toHours(instance.getUptime()) > threshold && instance.getStatus().equals
              (InstanceState.RUNNING.value())) {
         TagApi tagApi = api.getTagApiForRegion(region).get();
         if (tagApi != null) {
            ImmutableMap<String, String> tags = ImmutableMap.of(STATUS, tagValue, LAST_RUN, new Date().toString());
            tagApi.applyToResources(tags, ImmutableList.of(instance.getId()));
            log.info(Instance.builder().fromInstance(instance).tags(tags).build().toString() + " has been tagged " +
                    "" +
                    "with: " + tagApi.list().filter(TagPredicates.hasId(instance.getId())).toList());
            tagApi.deleteFromResources(ImmutableList.of(STATUS, LAST_RUN), ImmutableList.of(instance.getId()));
            log.info("Tags " + STATUS + ", " + LAST_RUN + " related to CloudCleaner have been removed");
         }
      }
   }

}