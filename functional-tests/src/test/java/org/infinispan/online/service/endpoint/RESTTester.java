package org.infinispan.online.service.endpoint;

import org.infinispan.online.service.utils.TestObjectCreator;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;


public class RESTTester implements EndpointTester {

   public void testBasicEndpointCapabilities(URL urlToService) {
      post(urlToService.toString() + "rest/default/should_default_cache_be_accessible_via_REST", "test", 200);
   }

   @Override
   public void testPutPerformance(URL urlToService, long timeout, TimeUnit timeUnit) {
      long endTime = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(timeout, timeUnit);
      TestObjectCreator testObjectCreator = new TestObjectCreator();
      while(System.currentTimeMillis() - endTime < 0) {
         String key = testObjectCreator.getRandomString(1000);
         String value = testObjectCreator.getRandomString(1000);
         post(value, urlToService.toString() + "rest/default/" + key, 200);
      }
   }

   public void testIfEndpointIsProtected(URL urlToService) {
      post(urlToService.toString() + "rest/default/should_default_cache_be_accessible_via_REST", "test", 401);
   }

   protected void post(String url, String body, int expectedCode) {
      given()
         .body(body)
      .when()
         .post(url)
      .then()
         .statusCode(expectedCode);
   }
}
