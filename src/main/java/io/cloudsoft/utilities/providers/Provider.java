package io.cloudsoft.utilities.providers;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import io.cloudsoft.utilities.io.cloudsoft.utilities.model.Instance;
import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;

import java.util.List;

public abstract class Provider {

    String provider;
    String identity;
    String credential;

    protected Provider(String provider, String identity, String credential) {
        this.provider = provider;
        this.identity = identity;
        this.credential = credential;
    }

    public String getProvider() {
        return provider;
    }

    public abstract List<Instance> listInstances() throws Exception;
    public abstract List<Instance> destroyInstances(String prefix);

    /**
     * Create a jclouds {@link org.jclouds.rest.RestContext} to access the Compute API.
     */
    protected ComputeServiceContext getComputeServiceContext(String provider) throws Exception {
        ImmutableSet<Module> modules = ImmutableSet.<Module>of(new SLF4JLoggingModule());
        ContextBuilder builder = ContextBuilder.newBuilder(provider)
                .credentials(identity, credential)
                .modules(modules);
        return builder.buildView(ComputeServiceContext.class);
    }
}