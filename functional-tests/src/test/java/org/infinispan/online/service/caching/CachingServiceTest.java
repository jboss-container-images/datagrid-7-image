package org.infinispan.online.service.caching;

import io.fabric8.openshift.client.OpenShiftClient;
import org.arquillian.cube.kubernetes.annotations.Named;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.infinispan.online.service.endpoint.HotRodTester;
import org.infinispan.online.service.endpoint.RESTTester;
import org.infinispan.online.service.utils.ReadinessCheck;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

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
   HotRodTester hotRodTester = new HotRodTester();
   RESTTester restTester = new RESTTester();

   @Before
   public void before() {
      readinessCheck.waitUntilAllPodsAreReady(openShiftClient, 30, TimeUnit.SECONDS);
   }

   @Test
   public void should_default_cache_be_accessible_via_hot_rod() throws IOException {
      hotRodTester.testBasicEndpointCapabilities(hotRodService);
   }

   @Test
   @Ignore("This test will eventually blow up. We are still tuning off-heap parameters")
   public void should_not_blow_up_because_of_oom() {
      hotRodTester.testPutPerformance(hotRodService, 60, TimeUnit.SECONDS);
   }

   @Test
   public void should_default_cache_be_accessible_via_REST() throws IOException {
      restTester.testBasicEndpointCapabilities(restService);
   }
}
