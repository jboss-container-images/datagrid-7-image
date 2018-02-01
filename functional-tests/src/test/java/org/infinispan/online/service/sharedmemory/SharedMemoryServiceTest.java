package org.infinispan.online.service.sharedmemory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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
import io.fabric8.openshift.client.OpenShiftClient;


@RunWith(ArquillianConditionalRunner.class)
@RequiresOpenshift
public class SharedMemoryServiceTest {

   private static final String SERVICE_NAME = "shared-memory-service";

   URL hotRodService;
   URL restService;

   OpenShiftClient client = OpenShiftClientCreator.getClient();;
   ReadinessCheck readinessCheck = new ReadinessCheck();
   HotRodTester hotRodTester = new HotRodTester(SERVICE_NAME, client);
   RESTTester restTester = new RESTTester(SERVICE_NAME, client);
   OpenShiftHandle handle = new OpenShiftHandle(client);

   @Deployment
   public static Archive<?> deploymentApp() {
      return ShrinkWrap
         .create(WebArchive.class, "test.war")
         .addAsLibraries(DeploymentHelper.testLibs())
         .addPackage(SharedMemoryServiceTest.class.getPackage())
         .addPackage(ReadinessCheck.class.getPackage())
         .addPackage(ScalingTester.class.getPackage())
         .addPackage(HotRodTester.class.getPackage());
   }

   @Before
   public void before() throws MalformedURLException {
      readinessCheck.waitUntilAllPodsAreReady(client);
      hotRodService = handle.getServiceWithName("shared-memory-service-app-hotrod");
      restService = handle.getServiceWithName("shared-memory-service-app-http");
   }

   @After
   public void after() {
      hotRodTester.clear(hotRodService);
   }

   @Test
   public void should_default_cache_be_accessible_via_hot_rod() throws IOException {
      hotRodTester.testBasicEndpointCapabilities(hotRodService);
   }

   @Test(expected = HotRodClientException.class)
   public void should_default_cache_be_protected_via_hot_rod() throws IOException {
      hotRodTester.testIfEndpointIsProtected(hotRodService);
   }

   @Test
   public void should_default_cache_be_accessible_via_REST() throws IOException {
      restTester.testBasicEndpointCapabilities(restService);
   }

   @Test
   public void should_default_cache_be_protected_via_REST() throws IOException {
      restTester.testIfEndpointIsProtected(restService);
   }
}
