package org.infinispan.online.service.sharedmemory;

import org.arquillian.cube.kubernetes.annotations.Named;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.infinispan.online.service.endpoint.HotRodTester;
import org.infinispan.online.service.endpoint.RESTTester;
import org.jboss.arquillian.test.api.ArquillianResource;
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

   HotRodTester hotRodTester = new HotRodTester();
   RESTTester restTester = new RESTTester();

   @Test
   public void should_default_cache_be_accessible_via_hot_rod() throws IOException {
      hotRodTester.testBasicEndpointCapabilities(hotRodService);
   }

   @Test
   public void should_default_cache_be_protected_via_REST() throws IOException {
      restTester.testIfEndpointIsProtected(restService);
   }
}
