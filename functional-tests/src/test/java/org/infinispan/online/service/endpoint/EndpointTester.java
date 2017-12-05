package org.infinispan.online.service.endpoint;

import java.net.URL;
import java.util.concurrent.TimeUnit;

public interface EndpointTester {

   String USERNAME = "test";
   String PASSWORD = "test";

   void testBasicEndpointCapabilities(URL urlToService);

   void testPutPerformance(URL urlToService, long timeout, TimeUnit timeUnit);

   void testIfEndpointIsProtectedAgainstNoCredentials(URL urlToService);

   void testIfEndpointIsProtectedAgainstWrongCredentials(URL urlToService);
}
