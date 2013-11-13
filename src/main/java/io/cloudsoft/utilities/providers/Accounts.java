package io.cloudsoft.utilities.providers;

import brooklyn.config.BrooklynProperties;
import com.google.common.base.Predicate;
import com.google.common.collect.*;
import org.jclouds.domain.Credentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;


public class Accounts {

   private static final Logger log = LoggerFactory.getLogger(Credentials.class);

   public static Map<String, Set<Credentials>> listCredentialsInBrooklynProperties() {
      Map<String, Set<Credentials>> credentials = Maps.newHashMap();
      final BrooklynProperties brooklynProperties = BrooklynProperties.Factory.newDefault();
      Map<String, Object> brooklynPropertiesMap = brooklynProperties.asMapWithStringKeys();
      Map<String, Set<String>> providersWithPrefixes = Maps.newHashMap();

      for (final String providerName : Providers.listProvidersInBrooklynProperties()) {
         Iterable<Map.Entry<String, Object>> entrySet = Iterables.filter(brooklynPropertiesMap.entrySet(), new Predicate<Map.Entry<String, Object>>() {
            @Override
            public boolean apply(@Nullable Map.Entry<String, Object> input) {
               return ((String) input.getValue()).contains("jclouds:" + providerName);
            }
         });
         // extract prefixes of the config keys
         Set<String> prefixes = Sets.newHashSet();
         for (Map.Entry<String, Object> entry : entrySet) {
            prefixes.add(entry.getKey());
         }
         providersWithPrefixes.put(providerName, prefixes);
      }
      for (Map.Entry<String, Set<String>> providerWithPrefix : providersWithPrefixes.entrySet()) {
         String provider = providerWithPrefix.getKey();
         Set<String> prefixes = providerWithPrefix.getValue();
         Set<Credentials> creds = Sets.newHashSet();
         for (String prefix : prefixes) {
            String identity = "", credential = "";
            for (String key : brooklynPropertiesMap.keySet()) {
               if (key.equals(prefix + ".identity")) {
                  identity = (String) brooklynPropertiesMap.get(key);
               }
               if (key.equals(prefix + ".credential")) {
                  credential = (String) brooklynPropertiesMap.get(key);
               }
               if (!identity.isEmpty() && !credential.isEmpty()) {
                  creds.add(new Credentials(identity, credential));
               }
            }
         }
         if (!creds.isEmpty())
            credentials.put(provider, creds);
      }
      return credentials;
      }

   public static void main(String[] args) throws Exception {
      Accounts.listCredentialsInBrooklynProperties();
   }

   public static Set<Credentials> getLoginCredentials(String provider) {
      return Accounts.listCredentialsInBrooklynProperties().get(provider);
   }

}
