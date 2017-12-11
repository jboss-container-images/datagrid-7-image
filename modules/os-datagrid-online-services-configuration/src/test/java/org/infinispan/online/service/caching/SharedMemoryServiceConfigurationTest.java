package org.infinispan.online.service.caching;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

import org.infinispan.online.service.caching.assertions.ResultAssertion;
import org.infinispan.online.service.caching.assertions.XmlAssertion;
import org.infinispan.online.service.caching.util.ConfigurationScriptInvoker;
import org.infinispan.online.service.caching.util.TestResourceLocator;
import org.infinispan.online.service.caching.util.TestServerLocator;
import org.junit.Before;
import org.junit.Test;

public class SharedMemoryServiceConfigurationTest {

   TestServerLocator testServerLocator = new TestServerLocator();
   ConfigurationScriptInvoker configurationScriptInvoker = new ConfigurationScriptInvoker();
   TestResourceLocator testResourceLocator = new TestResourceLocator();

   Path servicesXml = testServerLocator.locateServer().resolve("standalone/configuration/services.xml");
   Path jbossHome = testServerLocator.locateServer();

   Map<String, String> requiredScriptParameters = new HashMap<>();
   Map<String, String> requiredEnvVars = new HashMap<>();

   public SharedMemoryServiceConfigurationTest() {
      requiredEnvVars.put("APPLICATION_USER", "test");
      requiredEnvVars.put("APPLICATION_USER_PASSWORD", "test");

      requiredScriptParameters.put("num_owners", "3");
      requiredScriptParameters.put("keystore_file", "test");
      requiredScriptParameters.put("keystore_password", "test");
   }


   @Before
   public void beforeTest() throws IOException {
      Path baselineConfiguration = testResourceLocator.locateFile("caching-service/services-7.2.xml");
      Files.copy(baselineConfiguration, servicesXml, StandardCopyOption.REPLACE_EXISTING);
   }

   @Test
   public void should_leave_all_endpoints() {
     //when
      ConfigurationScriptInvoker.Result result = configurationScriptInvoker.invokeScript(jbossHome, "shared-memory-service", requiredScriptParameters, requiredEnvVars);

      //then
      ResultAssertion.assertThat(result).printResult().isOk();
      XmlAssertion.assertThat(servicesXml).hasNoXPath("//*[local-name()='memcached-connector']");
      XmlAssertion.assertThat(servicesXml).hasXPath("//*[local-name()='rest-connector']");
      XmlAssertion.assertThat(servicesXml).hasXPath("//*[local-name()='hotrod-connector']");
   }

   @Test
   public void should_add_kube_ping() {
      //when
      ConfigurationScriptInvoker.Result result = configurationScriptInvoker.invokeScript(jbossHome, "shared-memory-service", requiredScriptParameters, requiredEnvVars);

      //then
      ResultAssertion.assertThat(result).printResult().isOk();

      XmlAssertion.assertThat(servicesXml)
      .hasXPath("//*[local-name()='stack' and @name='kubernetes']")
      .hasXPath("//*[local-name()='transport' and @type='TCP']")
      .hasXPath("//*[local-name()='protocol' and @type='openshift.KUBE_PING']")
      .hasXPath("//*[local-name()='protocol' and @type='FD_SOCK']")
      .hasXPath("//*[local-name()='protocol' and @type='FD_ALL']")
      .hasXPath("//*[local-name()='protocol' and @type='VERIFY_SUSPECT']")
      .hasXPath("//*[local-name()='protocol' and @type='pbcast.NAKACK2']")
      .hasXPath("//*[local-name()='protocol' and @type='UNICAST3']")
      .hasXPath("//*[local-name()='protocol' and @type='pbcast.STABLE']")
      .hasXPath("//*[local-name()='protocol' and @type='pbcast.GMS']")
      .hasXPath("//*[local-name()='protocol' and @type='MFC']")
      .hasXPath("//*[local-name()='protocol' and @type='FRAG3']");
   }

   @Test
   public void should_fail_if_num_owners_not_configured() {
      // when
      ConfigurationScriptInvoker.Result result = configurationScriptInvoker.invokeScript(jbossHome, "shared-memory-service", requiredScriptParameters, requiredEnvVars);

      //then
      ResultAssertion.assertThat(result).printResult().isOk();
      XmlAssertion.assertThat(servicesXml)
         .hasXPath("//*[local-name()='distributed-cache-configuration' and @name='shared-memory-service' and @owners='3']");
   }

   @Test
   public void should_fail_if_no_user_is_specified() {
      Map<String, String> envVariables = new HashMap<>(requiredEnvVars);
      envVariables.remove("APPLICATION_USER");
      //when
      ConfigurationScriptInvoker.Result result = configurationScriptInvoker.invokeScript(jbossHome, "caching-service", requiredScriptParameters, envVariables);

      //then
      ResultAssertion.assertThat(result).printResult().isFailed();
   }

   @Test
   public void should_fail_if_no_user_password_is_specified() {
      Map<String, String> envVariables = new HashMap<>(requiredEnvVars);
      envVariables.remove("APPLICATION_USER_PASSWORD");
      //when
      ConfigurationScriptInvoker.Result result = configurationScriptInvoker.invokeScript(jbossHome, "caching-service", requiredScriptParameters, envVariables);

      //then
      ResultAssertion.assertThat(result).printResult().isFailed();
   }

   @Test
   public void should_adjust_configuration_templates() {
      //when
      ConfigurationScriptInvoker.Result result = configurationScriptInvoker.invokeScript(jbossHome, "shared-memory-service", requiredScriptParameters, requiredEnvVars);

      //then
      ResultAssertion.assertThat(result).printResult().isOk();

      XmlAssertion.assertThat(servicesXml)
         .hasXPath("//*[local-name()='distributed-cache-configuration' and @name='shared-memory-service']")
         .hasNoXPath("//*[local-name()='distributed-cache-configuration' and @name='caching-service']")
         .hasXPath("//*[local-name()='distributed-cache' and @name='default' and @configuration='shared-memory-service']")
         .hasNoXPath("//*[local-name()='distributed-cache' and @name='memcachedCache' and @configuration='shared-memory-service']");
//  https://issues.jboss.org/browse/ISPN-8341
//         .hasNoXPath("//*[local-name()='distributed-cache-configuration' and @name='default']")
//         .hasNoXPath("//*[local-name()='distributed-cache-configuration' and @name='memcachedCache']")
   }
}
