package org.infinispan.online.service.endpoint;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import io.restassured.response.Response;
import org.infinispan.online.service.utils.TestObjectCreator;

import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;

public class RESTTester implements EndpointTester {

   private String cache = "default";
   private String value = "value";
   private String key = "restKey";
   private String newValue = "newValue";

   public void testBasicEndpointCapabilities(URL urlToService) {
      post(urlToService.toString() + "rest/default/should_default_cache_be_accessible_via_REST", "test", 200, true);
   }

   public void testCacheAvailability(URL urlToService, String cache, boolean shouldBeAvailable) {
      int expectedCode = shouldBeAvailable ? 200 : 404;
      post(urlToService.toString() + "rest/" + cache + "/should_cache_be_accessible_via_REST", "test", expectedCode, true);
   }

   @Override
   public void testPutPerformance(URL urlToService, long timeout, TimeUnit timeUnit) {
      //given
      long endTime = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(timeout, timeUnit);
      TestObjectCreator testObjectCreator = new TestObjectCreator();
      //when
      while (System.currentTimeMillis() - endTime < 0) {
         String key = testObjectCreator.getRandomString(1000);
         String value = testObjectCreator.getRandomString(1000);
         post(value, urlToService.toString() + "rest/default/" + key, 200, true);
      }
   }

   public void testIfEndpointIsProtected(URL urlToService) {
      post(urlToService.toString() + "rest/default/should_default_cache_be_accessible_via_REST", "test", 401, false);
   }

   private void post(String url, String body, int expectedCode, boolean authenticate) {
      RequestSpecification spec = given();
      if (authenticate)
         spec = spec.auth().basic("test", "test");

      spec
         .body(body)
         .when()
         .post(url)
         .then()
         .statusCode(expectedCode);
   }

   public void putGetRemoveTest(URL urlToService) throws IOException {
      String stringUrl = urlToService.toString();

      putGetTest(stringUrl);
      removeTest(stringUrl);
   }

   private void removeTest(String stringUrl) {
      //given
      put(stringUrl, key, value);
      //when
      removeMethod(stringUrl, key);
      //then
      getMethod(stringUrl, key, 404);
   }

   private void putGetTest(String stringUrl) {
      //when
      put(stringUrl, key, value);
      //then
      String restValue = getMethod(stringUrl, key);
      assertEquals(value, restValue);

      //when
      put(stringUrl, key, newValue);
      //then
      restValue = getMethod(stringUrl, key);
      assertEquals(newValue, restValue);
   }

   protected void put(String url, String body, int expectedCode, boolean authenticate) {
      RequestSpecification spec = given();
      if (authenticate)
         spec = spec.auth().basic("test", "test");

      spec.body(body).when().put(url).then().statusCode(expectedCode);
   }

   protected void delete(String url, String body, int expectedCode, boolean authenticate) {
      RequestSpecification spec = given();
      if (authenticate)
         spec = spec.auth().basic("test", "test");

      spec.body(body).when().delete(url).then().statusCode(expectedCode);
   }

   protected String get(String url, int expectedCode, boolean authenticate) {
      RequestSpecification spec = given();
      if (authenticate)
         spec = spec.auth().basic("test", "test");

      Response response = spec.when().get(url);
      response.then().statusCode(expectedCode);
      return response.andReturn().body().asString();
   }

   public void post(String urlServerAddress, String key, String value, int expectedCode) {
      post(urlServerAddress.toString() + "rest/" + cache + "/" + key, value, expectedCode, true);
   }

   public void post(String urlServerAddress, String key, String value) {
      post(urlServerAddress, key, value, 200);
   }

   public void put(String urlServerAddress, String key, String value, int expectedCode) {
      put(urlServerAddress.toString() + "rest/" + cache + "/" + key, value, expectedCode, true);
   }

   public void put(String urlServerAddress, String key, String value) {
      put(urlServerAddress, key, value, 200);
   }

   public String getMethod(String urlServerAddress, String key, int expectedCode) {
      return get(urlServerAddress.toString() + "rest/" + cache + "/" + key, expectedCode, true);
   }

   public String getMethod(String urlServerAddress, String key) {
      return getMethod(urlServerAddress, key, 200);
   }

   public void removeMethod(String urlServerAddress, String key, int expectedCode) {
      delete(urlServerAddress.toString() + "rest/" + cache + "/" + key, "", expectedCode, true);
   }

   public void removeMethod(String urlServerAddress, String key) {
      removeMethod(urlServerAddress, key, 200);
   }
}
