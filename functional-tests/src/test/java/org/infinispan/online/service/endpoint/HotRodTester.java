package org.infinispan.online.service.endpoint;

import static junit.framework.TestCase.assertNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.infinispan.online.service.utils.TestObjectCreator.generateConstBytes;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.net.InetSocketAddress;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

import io.fabric8.openshift.client.OpenShiftClient;
import org.infinispan.client.hotrod.CacheTopologyInfo;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.SaslQop;
import org.infinispan.commons.util.CloseableIteratorSet;
import org.infinispan.online.service.utils.TrustStore;

import io.fabric8.kubernetes.api.model.Pod;

public class HotRodTester implements EndpointTester {

   private final String serviceName;
   private String hotRodKey = "hotRodKey";
   private TrustStore trustStore;

   public HotRodTester(String serviceName, OpenShiftClient client) {
      this.serviceName = serviceName;
      trustStore = new TrustStore(client, serviceName);
   }

   public void clear(URL urlToService) {
      RemoteCacheManager cachingService = getRemoteCacheManager(urlToService, true);
      RemoteCache<String, String> defaultCache = cachingService.getCache();
      CloseableIteratorSet<String> entriesToBeDeleted = defaultCache.keySet();
      entriesToBeDeleted.stream().forEach(k -> defaultCache.remove(k));
   }

   public void testBasicEndpointCapabilities(URL urlToService) {
      testBasicEndpointCapabilities(urlToService, true);
   }

   private void testBasicEndpointCapabilities(URL urlToService, boolean authenticate) {
      //given
      RemoteCacheManager cachingService = getRemoteCacheManager(urlToService, authenticate);
      RemoteCache<String, String> defaultCache = cachingService.getCache();

      //when
      defaultCache.put("should_default_cache_be_accessible_via_hot_rod", "test");
      String valueObtainedFromTheCache = defaultCache.get("should_default_cache_be_accessible_via_hot_rod");

      //then
      assertThat(valueObtainedFromTheCache).isEqualTo("test");
   }

   @Override
   public void testIfEndpointIsProtected(URL urlToService) {
      testBasicEndpointCapabilities(urlToService, false);
   }

   public void testPodsVisible(URL urlToService, List<Pod> pods) {
      RemoteCache<String, String> defaultCache = getDefaultCache(urlToService);
      CacheTopologyInfo topology = defaultCache.getCacheTopologyInfo();

      List<String> podIPs = pods.stream()
         .map(pod -> pod.getStatus().getPodIP())
         .sorted()
         .collect(Collectors.toList());
      List<String> cacheNodeIPs = topology.getSegmentsPerServer().keySet().stream()
         .map(addr -> ((InetSocketAddress)addr).getHostName())
         .sorted()
         .collect(Collectors.toList());

      assertThat(cacheNodeIPs).isEqualTo(podIPs);
   }

   private RemoteCache<String, String> getDefaultCache(URL urlToService) {
      RemoteCacheManager cachingService = getRemoteCacheManager(urlToService);
      return cachingService.getCache();
   }

   public RemoteCache<String, String> getNamedCache(URL urlToService, String cacheName) {
      RemoteCacheManager cachingService = getRemoteCacheManager(urlToService);
      return cachingService.getCache(cacheName);
   }

   public int getNumberOfNodesInTheCluster(URL urlToService) {
      return getDefaultCache(urlToService).getCacheTopologyInfo().getSegmentsPerServer().keySet().size();
   }

   private RemoteCacheManager getRemoteCacheManager(URL urlToService) {
      return getRemoteCacheManager(urlToService, true);
   }

   private RemoteCacheManager getRemoteCacheManager(URL urlToService, boolean authenticate) {
      Configuration cachingServiceClientConfiguration = new ConfigurationBuilder()
         .addServer()
         .host(urlToService.getHost())
         .port(urlToService.getPort())
         .security()
         .ssl().enabled(true)
         .trustStoreFileName(trustStore.getPath())
         .trustStorePassword(TrustStore.TRUSTSTORE_PASSWORD)
         .authentication().enabled(authenticate)
         .username("test")
         .password("test")
         .realm("ApplicationRealm")
         .saslMechanism("DIGEST-MD5")
         .saslQop(SaslQop.AUTH)
         .serverName(serviceName)
         .build();

      return new RemoteCacheManager(cachingServiceClientConfiguration);
   }

   public void putGetTest(URL hotRodService) {
      RemoteCacheManager cachingService = getRemoteCacheManager(hotRodService);

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
      RemoteCacheManager cachingService = getRemoteCacheManager(hotRodService);
      RemoteCache<String, byte[]> byteArrayCache = cachingService.getCache();
      byte[] randomValue = generateConstBytes(1024*1024);
      int currentCacheSize = -1;
      int lastCacheSize = -1;
      long counter = 0;

      //when
      do {
         lastCacheSize = currentCacheSize;
         String key = "key" + counter++;

         byteArrayCache.put(key, randomValue);
         assertArrayEquals(randomValue, byteArrayCache.get(key));

         currentCacheSize = byteArrayCache.size();
      } while (currentCacheSize > lastCacheSize);
   }
}
