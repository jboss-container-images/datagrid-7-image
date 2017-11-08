package org.infinispan.online.service.caching;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import io.fabric8.openshift.client.OpenShiftClient;

import org.arquillian.cube.kubernetes.annotations.Named;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.online.service.endpoint.HotRodTester;
import org.infinispan.online.service.endpoint.RESTTester;
import org.infinispan.online.service.utils.ReadinessCheck;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.TestCase.assertNull;

@RunWith(ArquillianConditionalRunner.class)
@RequiresOpenshift
public class CachingServiceTest {

   @Named("caching-service-app-hotrod")
   @ArquillianResource
   URL hotRodService;

   @Named("caching-service-app-http")
   @ArquillianResource
   URL restService;

   @ArquillianResource
   OpenShiftClient openShiftClient;

   ReadinessCheck readinessCheck = new ReadinessCheck();
   HotRodTester hotRodTester = new HotRodTester("caching-service");
   RESTTester restTester = new RESTTester();

   @Before
   public void before() {
      readinessCheck.waitUntilAllPodsAreReady(openShiftClient, 30, TimeUnit.SECONDS);
   }

   @Test
   public void should_not_blow_up_because_of_oom() {
      hotRodTester.testPutPerformance(hotRodService, 60, TimeUnit.SECONDS);
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

   // The eviction can not be turned off on caching service
   @Test(timeout = 600000)
   @Ignore // TODO remove this when https://issues.jboss.org/browse/ISPN-8577 is resolved
   public void should_put_entries_until_first_one_gets_evicted() {
      hotRodTester.evictionTest(hotRodService);
   }

   // Only the default cache should be available in caching service
   @Test
   @Ignore // remove when https://issues.jboss.org/browse/ISPN-8531 is resolved
   public void only_default_cache_should_be_available() {
      assertNull(hotRodTester.getNamedCache(hotRodService, "memcachedCache", true));
      assertNull(hotRodTester.getNamedCache(hotRodService, "nonExistent", true));

      restTester.testCacheAvailability(restService, "nonExistent", false);
      restTester.testCacheAvailability(restService, "memcachedCache", false);
   }

   @Test
   public void should_default_cache_be_protected_via_REST() throws IOException {
      restTester.testIfEndpointIsProtected(restService);
   }
}
