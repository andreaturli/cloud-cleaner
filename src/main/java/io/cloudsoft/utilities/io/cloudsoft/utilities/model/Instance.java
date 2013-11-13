package io.cloudsoft.utilities.io.cloudsoft.utilities.model;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Objects.equal;

public class Instance {

    private String id;
    private String name;
    private String provider;
    private String region;
    private String type;
    private String status;
    private String keyName;
    private String groupName;
    private long uptime;
    private Map<String, String> tags;

    public Instance(String id, String name, String provider, String region, String type, String status,
                    String keyName, long uptime,
                    String groupName, Map<String,String> tags) {
        this.id = id;
        this.name = name;
        this.provider = provider;
        this.region = region;
        this.keyName = keyName;
        this.status = status;
        this.tags = tags;
        this.type = type;
        this.uptime = uptime;
        this.groupName = groupName;
        this.tags = tags;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
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

    public String getGroupName() {
        return groupName;
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
                equal(this.name, that.name) &&
                equal(this.provider, that.provider) &&
                equal(this.region, that.region) &&
                equal(this.keyName, that.keyName) &&
                equal(this.status, that.status) &&
                equal(this.tags, that.tags) &&
                equal(this.type, that.type) &&
                equal(this.groupName, that.groupName) &&
                equal(this.uptime, that.uptime);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("id", id)
                .add("name", name)
                .add("provider", provider)
                .add("region", region)
                .add("type", type)
                .add("status", status)
                .add("keyName", keyName)
                .add("uptime", TimeUnit.HOURS.convert(uptime, TimeUnit.MILLISECONDS) + " hour(s)")
                .add("groupName", groupName)
                .add("tags", tags)
                .toString();
    }

    public Builder toBuilder() {
        return new Builder().fromInstance(this);
    }

    public static final class Builder {

        private String id;
        private String name;
        private String provider;
        private String region;
        private String type;
        private String status;
        private String keyName;
        private long uptime;
        private String groupName;
        private Map<String, String> tags;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
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

        public Builder groupName(String groupName) {
            this.groupName = groupName;
            return this;
        }

        public Builder tags(Map<String, String> tags) {
            if (this.tags == null)
                this.tags = Maps.newHashMap();
            this.tags.putAll(tags);
            return this;
        }

        protected Builder self() {
            return this;
        }

        public Instance build() {
            return new Instance(id, name, provider, region, type, status, keyName, uptime, groupName, tags);
        }

        public Builder fromInstance(Instance in) {
            return id(in.getId())
                    .name(in.getName())
                    .provider(in.getProvider())
                    .region(in.getRegion())
                    .type(in.getType())
                    .status(in.getStatus())
                    .keyName(in.getKeyName())
                    .uptime(in.getUptime())
                    .groupName(in.getGroupName())
                    .tags(in.getTags());
        }

    }
}
