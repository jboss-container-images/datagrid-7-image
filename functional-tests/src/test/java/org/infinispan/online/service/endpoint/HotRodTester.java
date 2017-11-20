package org.infinispan.online.service.endpoint;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.SaslQop;
import org.infinispan.online.service.utils.TestObjectCreator;

public class HotRodTester implements EndpointTester {

   private final String serverName;

   public HotRodTester(String serverName) {
      this.serverName = serverName;
   }

   @Override
   public void testBasicEndpointCapabilities(URL urlToService) {
      testBasicEndpointCapabilities(urlToService, true);
   }

   public void testBasicEndpointCapabilities(URL urlToService, boolean authenticate) {
      //given
      RemoteCache<String, String> defaultCache = getDefaultCache(urlToService, authenticate);

      //when
      defaultCache.put("should_default_cache_be_accessible_via_hot_rod", "test");
      String valueObtainedFromTheCache = defaultCache.get("should_default_cache_be_accessible_via_hot_rod");

      //then
      assertThat(valueObtainedFromTheCache).isEqualTo("test");
   }

   @Override
   public void testPutPerformance(URL urlToService, long timeout, TimeUnit timeUnit) {
      //given
      RemoteCache<String, String> defaultCache = getDefaultCache(urlToService, true);
      long endTime = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(timeout, timeUnit);
      TestObjectCreator testObjectCreator = new TestObjectCreator();

      //when
      while(System.currentTimeMillis() - endTime < 0) {
         String key = testObjectCreator.getRandomString(1000);
         String value = testObjectCreator.getRandomString(1000);
         defaultCache.put(key, value);
      }
   }

   @Override
   public void testIfEndpointIsProtected(URL urlToService) {
      testBasicEndpointCapabilities(urlToService, false);
   }

   private RemoteCache<String, String> getDefaultCache(URL urlToService, boolean authenticate) {
      Configuration cachingServiceClientConfiguration = new ConfigurationBuilder()
         .addServer()
         .host(urlToService.getHost())
         .port(urlToService.getPort())
         .security().authentication().enabled(authenticate)
            .username("test")
            .password("test")
            .realm("ApplicationRealm")
            .saslMechanism("DIGEST-MD5")
            .saslQop(SaslQop.AUTH)
            .serverName(serverName)
         .build();
      RemoteCacheManager cachingService = new RemoteCacheManager(cachingServiceClientConfiguration);
      return cachingService.getCache();
   }
}
