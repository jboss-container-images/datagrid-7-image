package org.infinispan.online.service.endpoint;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.online.service.utils.TestObjectCreator;

import java.net.URL;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class HotRodTester implements EndpointTester {

   public void testBasicEndpointCapabilities(URL urlToService) {
      //given
      RemoteCache<String, String> defaultCache = getDefaultCache(urlToService);

      //when
      defaultCache.put("should_default_cache_be_accessible_via_hot_rod", "test");
      String valueObtainedFromTheCache = defaultCache.get("should_default_cache_be_accessible_via_hot_rod");

      //then
      assertThat(valueObtainedFromTheCache).isEqualTo("test");
   }

   @Override
   public void testPutPerformance(URL urlToService, long timeout, TimeUnit timeUnit) {
      //given
      RemoteCache<String, String> defaultCache = getDefaultCache(urlToService);
      long endTime = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(timeout, timeUnit);
      TestObjectCreator testObjectCreator = new TestObjectCreator();

      //when
      while(System.currentTimeMillis() - endTime < 0) {
         String key = testObjectCreator.getRandomString(1000);
         String value = testObjectCreator.getRandomString(1000);
         defaultCache.put(key, value);
      }
   }

   private RemoteCache<String, String> getDefaultCache(URL urlToService) {
      Configuration cachingServiceClientConfiguration = new ConfigurationBuilder()
         .addServer()
         .host(urlToService.getHost())
         .port(urlToService.getPort())
         .build();
      RemoteCacheManager cachingService = new RemoteCacheManager(cachingServiceClientConfiguration);
      return cachingService.getCache();
   }
}
