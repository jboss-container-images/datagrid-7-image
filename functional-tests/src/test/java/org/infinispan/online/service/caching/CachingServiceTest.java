package org.infinispan.online.service.caching;


import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.online.service.endpoint.HotRodTester;
import org.infinispan.online.service.endpoint.RESTTester;
import org.infinispan.online.service.scaling.ScalingTester;
import org.infinispan.online.service.utils.OpenShiftClientCreator;
import org.infinispan.online.service.utils.OpenShiftCommandlineClient;
import org.infinispan.online.service.utils.OpenShiftHandle;
import org.infinispan.online.service.utils.ReadinessCheck;
import org.infinispan.online.service.utils.TrustStore;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.fabric8.openshift.client.OpenShiftClient;

@RunWith(ArquillianConditionalRunner.class)
@RequiresOpenshift
@RunAsClient //run outside OpenShift
public class CachingServiceTest {

   private static final String SERVICE_NAME = "caching-service";

   URL hotRodService;
   URL restService;
   HotRodTester hotRodTester = new HotRodTester(SERVICE_NAME, "target");
   RESTTester restTester = new RESTTester(SERVICE_NAME, "target");
   ScalingTester scalingTester = new ScalingTester();
   OpenShiftCommandlineClient commandlineClient = new OpenShiftCommandlineClient();

   ReadinessCheck readinessCheck = new ReadinessCheck();
   OpenShiftClient client = OpenShiftClientCreator.getClient();
   OpenShiftHandle handle = new OpenShiftHandle(client);

   @Before
   public void before() throws MalformedURLException {
      readinessCheck.waitUntilAllPodsAreReady(client);
      hotRodService = handle.getServiceWithName("caching-service-app-hotrod");
      restService = handle.getServiceWithName("caching-service-app-http");
      TrustStore.create("target", SERVICE_NAME, client);
   }

   @After
   public void after() {
      hotRodTester.clear(hotRodService);
   }

   @Test
   public void should_read_and_write_through_rest_endpoint() throws IOException {
      restTester.putGetRemoveTest(restService);
   }

   @Test(expected = HotRodClientException.class)
   public void should_default_cache_be_protected_via_hot_rod() throws IOException {
      hotRodTester.testIfEndpointIsProtected(hotRodService);
   }

   @Test
   public void should_read_and_write_through_hotrod_endpoint() {
      hotRodTester.putGetTest(hotRodService);
   }

   @Test(timeout = 600000)
   public void should_put_entries_until_first_one_gets_evicted() {
      hotRodTester.evictionTest(hotRodService);
   }

   @Test
   public void only_default_cache_should_be_available() {
      assertNull(hotRodTester.getNamedCache(hotRodService, "memcachedCache"));
      assertNull(hotRodTester.getNamedCache(hotRodService, "nonExistent"));

      restTester.testCacheAvailability(restService, "nonExistent", false);
      restTester.testCacheAvailability(restService, "memcachedCache", false);
   }

   @Test
   public void should_default_cache_be_protected_via_REST() throws IOException {
      restTester.testIfEndpointIsProtected(restService);
   }

   @Test
   public void should_discover_new_cluster_members_when_scaling_up() {
      scalingTester.testFormingAClusterAfterScalingUp("caching-service-app", hotRodService, commandlineClient, readinessCheck, client, hotRodTester);
   }
}
