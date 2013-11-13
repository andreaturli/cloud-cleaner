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

public class ProviderFactory {

   private static final Logger log = LoggerFactory.getLogger(ProviderFactory.class);

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