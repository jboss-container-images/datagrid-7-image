package org.infinispan.online.service.sharedmemory;

import io.fabric8.openshift.client.OpenShiftClient;
import org.arquillian.cube.kubernetes.annotations.Named;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.online.service.endpoint.HotRodTester;
import org.infinispan.online.service.endpoint.RESTTester;
import org.infinispan.online.service.scaling.ScalingTester;
import org.infinispan.online.service.utils.OpenShiftCommandlineClient;
import org.infinispan.online.service.utils.ReadinessCheck;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URL;

@RunWith(ArquillianConditionalRunner.class)
@RequiresOpenshift
public class SharedMemoryServiceTest {

   @Named("shared-memory-service-app-hotrod")
   @ArquillianResource
   URL hotRodService;

   @Named("shared-memory-service-app-http")
   @ArquillianResource
   URL restService;

   @ArquillianResource
   OpenShiftClient openShiftClient;

   ReadinessCheck readinessCheck = new ReadinessCheck();
   HotRodTester hotRodTester = new HotRodTester("shared-memory-service");
   RESTTester restTester = new RESTTester();
   ScalingTester scalingTester = new ScalingTester();
   OpenShiftCommandlineClient commandlineClient = new OpenShiftCommandlineClient();

   @Before
   public void before() {
      readinessCheck.waitUntilAllPodsAreReady(openShiftClient);
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

   @Test
   public void should_discover_new_cluster_members_when_scaling_up() {
      scalingTester.testFormingAClusterAfterScalingUp("shared-memory-service-app", hotRodService, commandlineClient, readinessCheck, openShiftClient, hotRodTester);
   }
}
