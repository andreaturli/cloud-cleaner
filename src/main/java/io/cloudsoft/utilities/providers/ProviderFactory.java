package io.cloudsoft.utilities.providers;

import com.google.common.reflect.ClassPath;
import org.jclouds.domain.Credentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

import static io.cloudsoft.utilities.providers.Provider.AWS_PROVIDER;
import static io.cloudsoft.utilities.providers.Provider.RACKSPACE_UK_PROVIDER;
import static io.cloudsoft.utilities.providers.Provider.RACKSPACE_US_PROVIDER;
import static io.cloudsoft.utilities.providers.Provider.HPCLOUD_PROVIDER;
import static io.cloudsoft.utilities.providers.Provider.SOFTLAYER_PROVIDER;
import static io.cloudsoft.utilities.providers.Provider.GOOGLE_COMPUTE_ENGINE_PROVIDER;
import static io.cloudsoft.utilities.providers.Provider.INTEROUTE_PROVIDER;
import static io.cloudsoft.utilities.providers.Provider.IBM_SCE_PROVIDER;

public class ProviderFactory {

   private static final Logger log = LoggerFactory.getLogger(ProviderFactory.class);

   /*
   public Provider createProvider(String provider) throws Exception {
      if (provider.equals(AWS_PROVIDER)) {
         return new Ec2(Accounts.getLoginCredentials(AWS_PROVIDER));
      } else if (provider.equals(RACKSPACE_UK_PROVIDER)) {
         return new RackspaceCloudserversUK(Accounts.getLoginCredentials(RACKSPACE_UK_PROVIDER));
      } else if (provider.equals(RACKSPACE_US_PROVIDER)) {
         return new RackspaceCloudserversUS(Accounts.getLoginCredentials(RACKSPACE_US_PROVIDER));
      } else if (provider.equals(HPCLOUD_PROVIDER)) {
         return new HpCloudCompute(Accounts.getLoginCredentials(HPCLOUD_PROVIDER));
      } else if (provider.equals(SOFTLAYER_PROVIDER)) {
         return new Softlayer(Accounts.getLoginCredentials(SOFTLAYER_PROVIDER));
      } else if (provider.equals(GOOGLE_COMPUTE_ENGINE_PROVIDER)) {
         return new GoogleComputeEngine(Accounts.getLoginCredentials(GOOGLE_COMPUTE_ENGINE_PROVIDER));
      } else if (provider.equals(INTEROUTE_PROVIDER)) {
         return new Interoute(Accounts.getLoginCredentials(INTEROUTE_PROVIDER));
      } else if (provider.equals(IBM_SCE_PROVIDER)) {
         return new IbmSmartCloudEnterprise(Accounts.getLoginCredentials(IBM_SCE_PROVIDER));
      } else {
         throw new IllegalArgumentException("Not supported api/provider: " + provider);
      }
   }
   */

   public Provider getProviderInstance(String provider) throws IOException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
      Set<Credentials> credentials = Accounts.getLoginCredentials(provider);
      Set<ClassPath.ClassInfo> classInfoImmutableSet = ClassPath.from(Thread.currentThread()
              .getContextClassLoader()).getTopLevelClasses("io.cloudsoft.utilities.providers");
      for (ClassPath.ClassInfo classInfo : classInfoImmutableSet) {
         Class cls = Class.forName(classInfo.getName());
         if (!Modifier.isAbstract(cls.getModifiers()) &&
                 (BasicProvider.class.isAssignableFrom(cls) ||
                         Openstack.class.isAssignableFrom(cls))) {
            Constructor<?> constructor = cls.getConstructor(Set.class);
            Object obj = constructor.newInstance(credentials);
            Class noparams[] = {};
            Method method = cls.getDeclaredMethod("getName", noparams);
            String providerName = (String) method.invoke(obj, null);
            if (provider.equals(providerName))
               return (Provider) obj;
         }
      }
      return null;
   }

}