package org.infinispan.online.service.endpoint;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

public class HotRodTester implements EndpointTester {

   public void testBasicEndpointCapabilities(URL urlToService) {
      //given
      Configuration cachingServiceClientConfiguration = new ConfigurationBuilder()
         .addServer()
         .host(urlToService.getHost())
         .port(urlToService.getPort())
         .build();
      RemoteCacheManager cachingService = new RemoteCacheManager(cachingServiceClientConfiguration);
      RemoteCache<String, String> defaultCache = cachingService.getCache();

      //when
      defaultCache.put("should_default_cache_be_accessible_via_hot_rod", "test");
      String valueObtainedFromTheCache = defaultCache.get("should_default_cache_be_accessible_via_hot_rod");

      //then
      assertThat(valueObtainedFromTheCache).isEqualTo("test");
   }
}
