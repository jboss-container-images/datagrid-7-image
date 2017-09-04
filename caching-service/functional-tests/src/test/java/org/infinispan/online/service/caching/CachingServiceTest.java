package org.infinispan.online.service.caching;

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
import io.fabric8.openshift.clnt.v2_5.OpenShiftClient;

@RunWith(ArquillianConditionalRunner.class)
@RequiresOpenshift
public class CachingServiceTest {

   @Named("infinispan-app-hotrod")
   @ArquillianResource
   URL service;

   @ArquillianResource
   KubernetesClient client;

   @Test
   public void should_have_default_cache() throws IOException {
      //given
      Configuration cachingServiceClientConfiguration = new ConfigurationBuilder()
      .addServer()
      .host(service.getHost())
      .port(service.getPort())
      .build();
      RemoteCacheManager cachingService = new RemoteCacheManager(cachingServiceClientConfiguration);
      RemoteCache<String, String> defaultCache = cachingService.getCache();

      //when
      defaultCache.put("test", "test");
      String valueObtainedFromTheCache = defaultCache.get("test");

      //then
      assertThat(valueObtainedFromTheCache).isEqualTo("test");
   }
}
