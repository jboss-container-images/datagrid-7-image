package org.infinispan.online.service.endpoint;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.SaslQop;
import org.infinispan.online.service.utils.TestObjectCreator;

import static junit.framework.TestCase.assertNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.infinispan.online.service.utils.TestObjectCreator.generateConstBytes;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class HotRodTester implements EndpointTester {

   private final String serverName;
   private String hotRodKey = "hotRodKey";

   public HotRodTester(String serverName) {
      this.serverName = serverName;
   }

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
      while (System.currentTimeMillis() - endTime < 0) {
         String key = testObjectCreator.getRandomString(1000);
         String value = testObjectCreator.getRandomString(1000);
         defaultCache.put(key, value);
      }
   }

   @Override
   public void testIfEndpointIsProtected(URL urlToService) {
      testBasicEndpointCapabilities(urlToService, false);
   }

   public RemoteCache<String, String> getDefaultCache(URL urlToService, boolean authenticate) {
      RemoteCacheManager cachingService = getRemoteCacheManager(urlToService, authenticate);
      return cachingService.getCache();
   }

   public RemoteCache<String, String> getNamedCache(URL urlToService, String cacheName, boolean authenticate) {
      RemoteCacheManager cachingService = getRemoteCacheManager(urlToService, authenticate);
      return cachingService.getCache(cacheName);
   }

   public RemoteCacheManager getRemoteCacheManager(URL urlToService, boolean authenticate) {
      //given
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

      return new RemoteCacheManager(cachingServiceClientConfiguration);
   }

   public void putGetTest(URL hotRodService) {
      RemoteCacheManager cachingService = getRemoteCacheManager(hotRodService, true);

      stringPutGetTest(cachingService);
      stringUpdateGetTest(cachingService);
      stringRemoveTest(cachingService);

      intPutGetTest(cachingService);
      byteArrayPutGetTest(cachingService);
   }

   private void byteArrayPutGetTest(RemoteCacheManager cachingService) {
      //given
      RemoteCache<String, byte[]> byteArrayCache = cachingService.getCache();
      byte[] byteArrayValue = generateConstBytes(4096);
      //when
      byteArrayCache.put(hotRodKey, byteArrayValue);
      //then
      assertArrayEquals(byteArrayValue, byteArrayCache.get(hotRodKey));
   }

   private void intPutGetTest(RemoteCacheManager cachingService) {
      //given
      RemoteCache<String, Integer> intCache = cachingService.getCache();
      Integer intValue = 5;
      //when
      intCache.put(hotRodKey, intValue);
      //then
      assertEquals(intValue, intCache.get(hotRodKey));
   }

   private void stringRemoveTest(RemoteCacheManager cachingService) {
      //given
      RemoteCache<String, String> stringCache = cachingService.getCache();
      stringCache.put(hotRodKey, "value");
      //when
      stringCache.remove(hotRodKey);
      //then
      assertNull(stringCache.get(hotRodKey));
   }

   private void stringPutGetTest(RemoteCacheManager cachingService) {
      //given
      RemoteCache<String, String> stringCache = cachingService.getCache();
      //when
      stringCache.put(hotRodKey, "value");
      //then
      assertEquals("value", stringCache.get(hotRodKey));
   }

   private void stringUpdateGetTest(RemoteCacheManager cachingService) {
      //given
      RemoteCache<String, String> stringCache = cachingService.getCache();
      //when
      stringCache.put(hotRodKey, "value");
      stringCache.put(hotRodKey, "newValue");
      //then
      assertEquals("newValue", stringCache.get(hotRodKey));
   }

   public void evictionTest(URL hotRodService) {
      //given
      RemoteCacheManager cachingService = getRemoteCacheManager(hotRodService, true);
      RemoteCache<String, byte[]> byteArrayCache = cachingService.getCache();
      byte[] firstValue = generateConstBytes(4096);

      String key = "key";
      byteArrayCache.put(key, firstValue);
      assertArrayEquals(firstValue, byteArrayCache.get(key));

      //when the first entry is evicted, the test ends
      long counter = 0;
      while (byteArrayCache.get(key) != null) {
         byte[] value = generateConstBytes(4096);

         byteArrayCache.put(key + counter++, value);
         assertArrayEquals(value, byteArrayCache.get(key));
      }
   }
}
