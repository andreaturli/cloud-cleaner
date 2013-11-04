package io.cloudsoft.utilities.providers;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.cloudsoft.utilities.TagPredicates;
import io.cloudsoft.utilities.io.cloudsoft.utilities.model.Instance;
import org.jclouds.aws.ec2.AWSEC2Client;
import org.jclouds.aws.ec2.services.AWSInstanceClient;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.ec2.EC2AsyncClient;
import org.jclouds.ec2.EC2Client;
import org.jclouds.ec2.domain.Reservation;
import org.jclouds.ec2.domain.RunningInstance;
import org.jclouds.ec2.domain.Tag;
import org.jclouds.ec2.features.TagApi;
import org.jclouds.rest.RestContext;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Predicates.and;

class Ec2 extends Provider {

    protected Ec2(String provider, String identity, String credential) {
        super(provider, identity, credential);
    }

    @Override
    public List<Instance> listInstances() throws Exception {
        List<Instance> instances = Lists.newArrayList();
        ComputeServiceContext computeServiceContext = getComputeServiceContext(provider);
        try {
            RestContext<EC2Client, EC2AsyncClient> client = computeServiceContext.unwrap();
            for (String region : client.getApi().getConfiguredRegions()) {
                TagApi tagApi = client.getApi().getTagApiForRegion(region).get();
                Set<? extends Reservation<? extends RunningInstance>> reservations = getReservations(client, region);
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
                        instances.add(Instance.builder().id(instance.getId()).provider(provider).region(region)
                                .type(instance.getInstanceType()).status(instance.getInstanceState().value())
                                .keyName(instance.getKeyName())
                                .uptime(new Date().getTime() - instance.getLaunchTime().getTime()).tags(tags).build());
                    }
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            computeServiceContext.close();
        }
        return instances;
    }

    private Set<? extends Reservation<? extends RunningInstance>> getReservations(RestContext<EC2Client, EC2AsyncClient> client, String region) {
        AWSInstanceClient instanceClient = AWSEC2Client.class.cast(client.getApi()).getInstanceServices();
        return instanceClient.describeInstancesInRegion(region);
    }

    @Override
    public List<Instance> destroyInstances(String prefix) {
        System.out.println("No-op");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

}