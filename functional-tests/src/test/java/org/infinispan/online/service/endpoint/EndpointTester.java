package org.infinispan.online.service.endpoint;

import java.net.URL;
import java.util.concurrent.TimeUnit;

public interface EndpointTester {

   void testBasicEndpointCapabilities(URL urlToService);

   void testPutPerformance(URL urlToService, long timeout, TimeUnit timeUnit);

}
