package org.infinispan.online.service.endpoint;

import java.net.URL;

import static io.restassured.RestAssured.given;


public class RESTTester implements EndpointTester {

   public void testBasicEndpointCapabilities(URL urlToService) {
      given()
         .body("test")
      .when()
         .post(urlToService.toString() + "rest/default/should_default_cache_be_accessible_via_REST")
      .then()
         .statusCode(200);
   }

   public void testIfEndpointIsProtected(URL urlToService) {
      given()
         .body("test")
      .when()
         .post(urlToService.toString() + "rest/default/should_default_cache_be_accessible_via_REST")
         .then()
      .statusCode(401);
   }
}
