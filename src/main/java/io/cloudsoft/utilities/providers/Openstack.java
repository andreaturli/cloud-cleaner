package io.cloudsoft.utilities.providers;

import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.cloudsoft.utilities.io.cloudsoft.utilities.model.Instance;
import org.jclouds.collect.IterableWithMarker;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.domain.Credentials;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.NovaAsyncApi;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.jclouds.openstack.v2_0.domain.Resource;
import org.jclouds.rest.RestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class Openstack extends BasicProvider {

   protected static final String STATUS = "STATUS";
   private static final Logger log = LoggerFactory.getLogger(Openstack.class);

   public Openstack() {
      super();
   }

   public Openstack(Set<Credentials> credentials) {
      super(credentials);
   }

   @Override
   public List<Instance> listInstances() {
      List<Instance> instances = Lists.newArrayList();
      for (Credentials creds : credentials) {
         ComputeServiceContext computeServiceContext = getComputeServiceContext(getName(), creds.identity, creds.credential);
         try {
            RestContext<NovaApi, NovaAsyncApi> client = computeServiceContext.unwrap();
            for (String zone : client.getApi().getConfiguredZones()) {
               ServerApi serverApiForZone = client.getApi().getServerApiForZone(zone);
               FluentIterable<? extends Server> servers = serverApiForZone.listInDetail().concat();

               for (Server server : servers) {
                  serverApiForZone.getMetadata(server.getId());
                  instances.add(Instance.builder().id(server.getId())
                          .name(server.getName())
                          .provider(getName()).region(zone)
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
      }
      return instances;
   }

   @Override
   public void tagAndCleanInstances(String tag) throws Exception {
      for (Credentials creds : credentials) {
         ComputeServiceContext computeServiceContext = getComputeServiceContext(getName(), creds.identity, creds.credential);
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
                  log.info("{}={}", key, metadata.get(key));
               }
            }
         }
      }
   }

   protected enum TAG_VALUE {
      DELETABLE
   }

}