package io.cloudsoft.utilities.providers;

import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import io.cloudsoft.utilities.io.cloudsoft.utilities.model.Instance;
import org.jclouds.Constants;
import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.emptyToNull;
import static org.jclouds.compute.config.ComputeServiceProperties.TIMEOUT_SCRIPT_COMPLETE;

public abstract class BasicProvider implements Provider {

    private static final Logger log = LoggerFactory.getLogger(BasicProvider.class);

    protected static final String STATUS = "STATUS";
    protected static final String LAST_RUN = "LAST-RUN";

    protected enum ACTION {
        LIST, TAG_AND_CLEANUP, DESTROY
    }

    protected String identity;
    protected String credential;

    protected BasicProvider(String identity, String credential) {
        this.identity = identity;
        this.credential = credential;
    }

    /**
     * Create a jclouds {@link org.jclouds.rest.RestContext} to access the Compute API.
     */
    protected ComputeServiceContext getComputeServiceContext(String provider) throws Exception {
        Properties properties = new Properties();
        long scriptTimeout = TimeUnit.MILLISECONDS.convert(20, TimeUnit.MINUTES);
        properties.setProperty(TIMEOUT_SCRIPT_COMPLETE, scriptTimeout + "");
        properties.setProperty(Constants.PROPERTY_TRUST_ALL_CERTS, "true");
        ImmutableSet<Module> modules = ImmutableSet.<Module>of(new SLF4JLoggingModule());
        ContextBuilder builder = ContextBuilder.newBuilder(provider)
                .credentials(identity, credential)
                .modules(modules)
                .overrides(properties);
        return builder.buildView(ComputeServiceContext.class);
    }

    @Override
    public List<Instance> listInstances() throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void tagAndCleanInstances(String tag) throws Exception {
        log.debug("No-op");
    }

    @Override
    public void destroyNodes(final String prefix) throws Exception {
        ComputeServiceContext computeServiceContext = getComputeServiceContext(getName());
        try {
            for (NodeMetadata nodeMetadata : computeServiceContext.getComputeService().listNodesDetailsMatching(
                    new Predicate<ComputeMetadata>() {
                        @Override
                        public boolean apply(@Nullable ComputeMetadata input) {
                            return input.getName().startsWith(prefix);
                        }
                    })) {
                log.info("Deleting {} ...", nodeMetadata.getName());
                computeServiceContext.getComputeService().destroyNode(nodeMetadata.getId());
                log.info("Deleted {}!", nodeMetadata.getName());
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            computeServiceContext.close();
        }
    }

    @Override
    public void destroyNetworks(String projectName, String prefix) throws Exception {
        log.debug("No-op");
    }

    protected Predicate<NodeMetadata> groupStartsWith(final String groupPrefix) {
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
}