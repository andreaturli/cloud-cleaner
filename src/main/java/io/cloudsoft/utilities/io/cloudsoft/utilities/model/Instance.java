package io.cloudsoft.utilities.io.cloudsoft.utilities.model;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Objects.toStringHelper;

public class Instance {

   private String id;
   private String provider;
   private String region;
   private String type;
   private String status;
   private String keyName;
   private long uptime;
   private Map<String, String> tags;

   public Instance(String id, String provider, String region, String type, String status, String keyName, long uptime,
                   Map<String,
           String> tags) {
      this.id = id;
      this.provider = provider;
      this.region = region;
      this.keyName = keyName;
      this.status = status;
      this.tags = tags;
      this.type = type;
      this.uptime = uptime;
      this.tags = tags;
   }

   public String getId() {
      return id;
   }

   public String getProvider() {
      return provider;
   }

   public String getRegion() {
      return region;
   }

   public String getKeyName() {
      return keyName;
   }

   public String getStatus() {
      return status;
   }

   public Map<String, String> getTags() {
      return tags;
   }

   public String getType() {
      return type;
   }

   public long getUptime() {
      return uptime;
   }

   @Override
   public int hashCode() {
      return Objects.hashCode(id);
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null || getClass() != obj.getClass()) return false;
      Instance that = Instance.class.cast(obj);
      return equal(this.id, that.id) &&
              equal(this.provider, that.provider) &&
              equal(this.region, that.region) &&
              equal(this.keyName, that.keyName) &&
              equal(this.status, that.status) &&
              equal(this.tags, that.tags) &&
              equal(this.type, that.type) &&
              equal(this.uptime, that.uptime);
   }

   public static Builder builder() {
      return new Builder();
   }

   public Builder toBuilder() {
      return new Builder().fromInstance(this);
   }

   public static final class Builder {

      private String id;
      private String provider;
      private String region;
      private String type;
      private String status;
      private String keyName;
      private long uptime;
      private Map<String, String> tags;

      public Builder id(String id) {
         this.id = id;
         return this;
      }

      public Builder provider(String provider) {
         this.provider = provider;
         return this;
      }

      public Builder region(String region) {
         this.region = region;
         return this;
      }

      public Builder type(String type) {
         this.type = type;
         return this;
      }

      public Builder status(String status) {
         this.status = status;
         return this;
      }

      public Builder keyName(String keyName) {
         this.keyName = keyName;
         return this;
      }

      public Builder uptime(long uptime) {
         this.uptime = uptime;
         return this;
      }

      public Builder tags(Map<String, String> tags) {
         if(this.tags == null)
            this.tags = Maps.newHashMap();
         this.tags.putAll(tags);
         return this;
      }

      protected Builder self() {
         return this;
      }

      public Instance build() {
         return new Instance(id, provider, region, type, status, keyName, uptime, tags);
      }

      public Builder fromInstance(Instance in) {
         return id(in.getId())
                 .provider(in.getProvider())
                 .region(in.getRegion())
                 .type(in.getType())
                 .status(in.getStatus())
                 .keyName(in.getKeyName())
                 .uptime(in.getUptime())
                 .tags(in.getTags());
      }

       @Override
       public String toString() {
           return Objects.toStringHelper(this)
                   .add("id", id)
                   .add("provider", provider)
                   .add("region", region)
                   .add("type", type)
                   .add("status", status)
                   .add("keyName", keyName)
                   .add("uptime", uptime)
                   .add("tags", tags)
                   .toString();
       }
   }
}
