package io.cloudsoft.utilities.providers;

import brooklyn.config.BrooklynProperties;
import brooklyn.config.ConfigKey;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.reflect.ClassPath;
import io.cloudsoft.utilities.io.cloudsoft.utilities.model.Instance;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.get;

public class Providers {

    /**
       Scans all the providers available in io.cloudsoft.utilities.providers package looking for classes that
     implement the @{Provider} interface
     */
    public static Set<String> listSupportedProviders() throws Exception {
        Set<String> providerNames = Sets.newHashSet();
        Set<ClassPath.ClassInfo> classInfoImmutableSet = ClassPath.from(Thread.currentThread()
                .getContextClassLoader()).getTopLevelClasses("io.cloudsoft.utilities.providers");
        for(ClassPath.ClassInfo classInfo : classInfoImmutableSet) {
            final String className = classInfo.getName();
            Class cls = Class.forName(className);
            if(!Modifier.isAbstract(cls.getModifiers()) &&
                    (BasicProvider.class.isAssignableFrom(cls) ||
                            Openstack.class.isAssignableFrom(cls))) {
                Constructor<?> constructor = cls.getConstructor(String.class, String.class);
                Object obj = constructor.newInstance("", "");
                Class noparams[] = {};
                Method method = cls.getDeclaredMethod("getName", noparams);
                providerNames.add((String) method.invoke(obj, null));
            }
        }
        return providerNames;
    }

    public static void main(String[] args) throws Exception {
        Providers.validProviders();
    }

    public static Set<String> listProvidersInBrooklynProperties() throws Exception {
        Set<String> providerNames = Sets.newHashSet();
        final BrooklynProperties brooklynProperties = BrooklynProperties.Factory.newDefault();
        for (String key : brooklynProperties.asMapWithStringKeys().keySet()) {
            if (key.startsWith("brooklyn.location.named.")) {
                providerNames.add(get(Splitter.on(".").split(key), 3));
            }
        }
        return providerNames;
    }

    public static Set<String> validProviders() throws Exception {
        Set<String> fromBrooklynProperties = listProvidersInBrooklynProperties();
        Set<String> supportedProviders = listSupportedProviders();
        final Sets.SetView<String> intersection = Sets.intersection(fromBrooklynProperties, supportedProviders);
        if (intersection.isEmpty()) {
           return fromBrooklynProperties;
        }
        return intersection;
    }

}
