package io.cloudsoft.utilities.providers;

import brooklyn.config.BrooklynProperties;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.reflect.ClassPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

import static com.google.common.collect.Iterables.get;

public class Providers {

   private static final Logger log = LoggerFactory.getLogger(Providers.class);

   /**
    * Scans all the providers available in io.cloudsoft.utilities.providers package looking for classes that
    * implement the @{Provider} interface
    */
   public static Set<String> listSupportedProviders() throws Exception {
      Set<String> providerNames = getProviderNamesWithReflection("io.cloudsoft.utilities.providers");
      log.debug("CloudCleaner supports the following providers: " + Iterables.toString(providerNames));
      return providerNames;
   }

   public static Set<String> listProvidersInBrooklynProperties() {
      Set<String> providerNames = Sets.newHashSet();
      final BrooklynProperties brooklynProperties = BrooklynProperties.Factory.newDefault();
      for (Object obj : brooklynProperties.asMapWithStringKeys().values()) {
         String value = (String) obj;
         if (value.startsWith("jclouds:")) {
            final String providerName = get(Splitter.on(":").split(value), 1);
            providerNames.add(providerName);
         }
      }
      log.debug("brooklyn.properties defines configurations for the following providers: " + Iterables.toString(providerNames));
      return providerNames;
   }

   public static Set<String> validProviders() throws Exception {
      Set<String> fromBrooklynProperties = listProvidersInBrooklynProperties();
      Set<String> supportedProviders = listSupportedProviders();
      final Sets.SetView<String> intersection = Sets.intersection(fromBrooklynProperties, supportedProviders);
      if (intersection.isEmpty()) {
         return Sets.newHashSet();
      }
      log.debug("CloudCleaner considers valid the following providers:" + Iterables.toString(intersection));
      return intersection;
   }

   public static Set<String> getProviderNamesWithReflection(String packageName) throws Exception {
      Set<String> providerNames = Sets.newHashSet();

      Set<ClassPath.ClassInfo> classInfoImmutableSet = ClassPath.from(Thread.currentThread()
              .getContextClassLoader()).getTopLevelClasses(packageName);
      for (ClassPath.ClassInfo classInfo : classInfoImmutableSet) {
         final String className = classInfo.getName();
         Class cls = Class.forName(className);
         if (!Modifier.isAbstract(cls.getModifiers()) &&
                 (BasicProvider.class.isAssignableFrom(cls) ||
                         Openstack.class.isAssignableFrom(cls))) {
            Constructor<?> constructor = cls.getConstructor(Set.class);
            Object obj = constructor.newInstance(Sets.newHashSet());
            Class noparams[] = {};
            Method method = cls.getDeclaredMethod("getName", noparams);
            providerNames.add((String) method.invoke(obj, null));
         }
      }
      return providerNames;
   }

}
