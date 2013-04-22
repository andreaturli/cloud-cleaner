package io.cloudsoft.utilities;

import com.google.common.base.Predicate;
import org.jclouds.ec2.domain.Tag;

import static io.cloudsoft.utilities.CloudCleaner.STATUS;

/**
 *
 * @author Andrea Turli
 * @version 0.1.0-SNAPSHOT
 */
public class TagPredicates {

   public static Predicate isInstance() {
      return new IsInstance();
   }

   public static Predicate getResource(String instanceId) {
      return new GetResource(instanceId);
   }

   public static Predicate containsStatus() {
      return new ContainsStatus();
   }
}

class IsInstance implements Predicate<Tag> {

   @Override
   public boolean apply(Tag input) {
      return input.getResourceType().equals(Tag.ResourceType.INSTANCE);
   }
}

class ContainsStatus implements Predicate<Tag> {

   @Override
   public boolean apply(Tag input) {
      return input.getKey().contains(STATUS);
   }
}

class GetResource implements Predicate<Tag> {

   private final String instanceId;

   public GetResource(String instanceId) {
      this.instanceId = instanceId;
   }

   @Override
   public boolean apply(Tag input) {
      return input.getResourceId().equals(instanceId);
   }
}
