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
import org.infinispan.online.service.utils.OpenShiftClientCreator;
import org.infinispan.online.service.utils.OpenShiftCommandlineClient;
import org.infinispan.online.service.utils.OpenShiftHandle;
import org.infinispan.online.service.utils.ReadinessCheck;
import org.infinispan.online.service.utils.TrustStore;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.fabric8.openshift.client.OpenShiftClient;

@RunWith(ArquillianConditionalRunner.class)
@RequiresOpenshift
@RunAsClient
public class SharedMemoryServiceTest {

   private static final String SERVICE_NAME = "shared-memory-service";

   URL hotRodService;
   URL restService;

   ReadinessCheck readinessCheck = new ReadinessCheck();
   HotRodTester hotRodTester = new HotRodTester(SERVICE_NAME, "target");
   RESTTester restTester = new RESTTester(SERVICE_NAME, "target");
   ScalingTester scalingTester = new ScalingTester();
   OpenShiftCommandlineClient commandlineClient = new OpenShiftCommandlineClient();
   OpenShiftClient client;

   @Before
   public void before() throws MalformedURLException {
      client = OpenShiftClientCreator.getClient();
      OpenShiftHandle handle = new OpenShiftHandle(client);
      readinessCheck.waitUntilAllPodsAreReady(client);
      hotRodService = handle.getServiceWithName("shared-memory-service-app-hotrod");
      restService = handle.getServiceWithName("shared-memory-service-app-http");
      TrustStore.create("target", SERVICE_NAME, client);
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
      scalingTester.testFormingAClusterAfterScalingUp("shared-memory-service-app", hotRodService, commandlineClient, readinessCheck, client, hotRodTester);
   }
}
