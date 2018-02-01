package org.infinispan.online.service.caching;


import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.openshift.client.OpenShiftClient;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.online.service.endpoint.HotRodTester;
import org.infinispan.online.service.endpoint.RESTTester;
import org.infinispan.online.service.scaling.ScalingTester;
import org.infinispan.online.service.utils.DeploymentHelper;
import org.infinispan.online.service.utils.OpenShiftClientCreator;
import org.infinispan.online.service.utils.OpenShiftHandle;
import org.infinispan.online.service.utils.ReadinessCheck;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.List;

import static org.junit.Assert.assertNull;


@RunWith(ArquillianConditionalRunner.class)
@RequiresOpenshift
public class CachingServiceTest {

   private static final String SERVICE_NAME = "caching-service";

   URL hotRodService;
   URL restService;
   OpenShiftClient client = OpenShiftClientCreator.getClient();
   HotRodTester hotRodTester = new HotRodTester(SERVICE_NAME, client);
   RESTTester restTester = new RESTTester(SERVICE_NAME, client);

   ReadinessCheck readinessCheck = new ReadinessCheck();
   OpenShiftHandle handle = new OpenShiftHandle(client);

   @Deployment
   public static Archive<?> deploymentApp() {
      return ShrinkWrap
         .create(WebArchive.class, "test.war")
         .addAsLibraries(DeploymentHelper.testLibs())
         .addPackage(CachingServiceTest.class.getPackage())
         .addPackage(ReadinessCheck.class.getPackage())
         .addPackage(ScalingTester.class.getPackage())
         .addPackage(HotRodTester.class.getPackage());
   }

   @Before
   public void before() throws MalformedURLException {
      readinessCheck.waitUntilAllPodsAreReady(client);
      hotRodService = handle.getServiceWithName("caching-service-app-hotrod");
      restService = handle.getServiceWithName("caching-service-app-http");
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
   public void hotrod_should_see_all_pods() throws MalformedURLException {
      List<Pod> pods = handle.getPodsWithLabel("application", "caching-service-app");
      hotRodTester.testPodsVisible(hotRodService, pods);
   }
}
