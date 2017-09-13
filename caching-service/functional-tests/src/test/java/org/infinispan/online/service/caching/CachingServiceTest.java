package org.infinispan.online.service.caching;

import static com.jayway.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URL;

import org.arquillian.cube.kubernetes.annotations.Named;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.fabric8.kubernetes.clnt.v2_5.KubernetesClient;

@RunWith(ArquillianConditionalRunner.class)
@RequiresOpenshift
public class CachingServiceTest {

   @Named("infinispan-app-hotrod")
   @ArquillianResource
   URL hotRodService;

   @Named("infinispan-app-http")
   @ArquillianResource
   URL restService;

   @Named("infinispan-app-memcached")
   @ArquillianResource
   URL memcachedService; //we just want to test injection here...

   @ArquillianResource
   KubernetesClient client;

   @Test
   public void should_default_cache_be_accessible_via_hot_rod() throws IOException {
      //given
      Configuration cachingServiceClientConfiguration = new ConfigurationBuilder()
        .addServer()
        .host(hotRodService.getHost())
        .port(hotRodService.getPort())
        .build();
      RemoteCacheManager cachingService = new RemoteCacheManager(cachingServiceClientConfiguration);
      RemoteCache<String, String> defaultCache = cachingService.getCache();

      //when
      defaultCache.put("should_default_cache_be_accessible_via_hot_rod", "test");
      String valueObtainedFromTheCache = defaultCache.get("should_default_cache_be_accessible_via_hot_rod");

      //then
      assertThat(valueObtainedFromTheCache).isEqualTo("test");
   }

   @Test
   public void should_default_cache_be_accessible_via_REST() throws IOException {
      given()
        .body("test")
     .when()
        .post(restService.toString() + "rest/default/should_default_cache_be_accessible_via_REST")
     .then()
        .statusCode(200);
   }
}
