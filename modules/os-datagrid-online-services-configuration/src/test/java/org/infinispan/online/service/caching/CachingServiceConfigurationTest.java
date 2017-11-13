package org.infinispan.online.service.caching;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import org.assertj.core.util.Maps;
import org.infinispan.online.service.caching.assertions.ResultAssertion;
import org.infinispan.online.service.caching.assertions.XmlAssertion;
import org.infinispan.online.service.caching.util.ConfigurationScriptInvoker;
import org.infinispan.online.service.caching.util.TestResourceLocator;
import org.infinispan.online.service.caching.util.TestServerLocator;
import org.junit.Before;
import org.junit.Test;

public class CachingServiceConfigurationTest {

   TestServerLocator testServerLocator = new TestServerLocator();
   ConfigurationScriptInvoker configurationScriptInvoker = new ConfigurationScriptInvoker();
   TestResourceLocator testResourceLocator = new TestResourceLocator();

   Path servicesXml = testServerLocator.locateServer().resolve("standalone/configuration/services.xml");
   Path jbossHome = testServerLocator.locateServer();

   Map<String, String> requiredScriptParameters = Maps.newHashMap("eviction_total_memory_bytes", "1");

   @Before
   public void beforeTest() throws IOException {
      Path baselineConfiguration = testResourceLocator.locateFile("caching-service/services-7.2.xml");
      Files.copy(baselineConfiguration, servicesXml, StandardCopyOption.REPLACE_EXISTING);
   }

   @Test
   public void should_fail_if_no_eviction_total_memory_bytes_is_specified() {
      //when
      ConfigurationScriptInvoker.Result result = configurationScriptInvoker.invokeScript(jbossHome, "caching-service");

      //then
      ResultAssertion.assertThat(result).printResult().isFailed();
   }

   @Test
   public void should_leave_all_endpoints() {
     //when
      ConfigurationScriptInvoker.Result result = configurationScriptInvoker.invokeScript(jbossHome, "caching-service", requiredScriptParameters);

      //then
      ResultAssertion.assertThat(result).printResult().isOk();
      XmlAssertion.assertThat(servicesXml).hasXPath("//*[local-name()='memcached-connector']");
      XmlAssertion.assertThat(servicesXml).hasXPath("//*[local-name()='rest-connector']");
      XmlAssertion.assertThat(servicesXml).hasXPath("//*[local-name()='hotrod-connector']");
   }

   @Test
   public void should_add_kube_ping() {
      //when
      ConfigurationScriptInvoker.Result result = configurationScriptInvoker.invokeScript(jbossHome, "caching-service", requiredScriptParameters);

      //then
      ResultAssertion.assertThat(result).printResult().isOk();

      XmlAssertion.assertThat(servicesXml)
      .hasXPath("//*[local-name()='stack' and @name='kubernetes']")
      .hasXPath("//*[local-name()='transport' and @type='TCP']")
      .hasXPath("//*[local-name()='protocol' and @type='kubernetes.KUBE_PING']")
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
   public void should_modify_default_cache_to_num_of_owners_1() {
      //when
      ConfigurationScriptInvoker.Result result = configurationScriptInvoker.invokeScript(jbossHome, "caching-service", requiredScriptParameters);

      //then
      ResultAssertion.assertThat(result).printResult().isOk();

      XmlAssertion.assertThat(servicesXml)
         .hasXPath("//*[local-name()='distributed-cache-configuration' and @name='caching-service' and @owners='1']");
   }

   @Test
   public void should_add_off_heap_with_eviction() {
      //when
      ConfigurationScriptInvoker.Result result = configurationScriptInvoker.invokeScript(jbossHome, "caching-service", requiredScriptParameters);

      //then
      ResultAssertion.assertThat(result).printResult().isOk();

      XmlAssertion.assertThat(servicesXml)
         .hasXPath("//*[local-name()='distributed-cache-configuration' and @name='caching-service']//*[local-name()='memory']")
         .hasXPath("//*[local-name()='distributed-cache-configuration' and @name='caching-service']//*[local-name()='memory']//*[local-name()='off-heap']")
         .hasXPath("//*[local-name()='distributed-cache-configuration' and @name='caching-service']//*[local-name()='memory']//*[local-name()='off-heap' and @eviction='MEMORY' and @size='1']");
   }

   @Test
   public void should_remove_authentication_from_rest() {
      //when
      ConfigurationScriptInvoker.Result result = configurationScriptInvoker.invokeScript(jbossHome, "caching-service", requiredScriptParameters);

      //then
      ResultAssertion.assertThat(result).printResult().isOk();

      XmlAssertion.assertThat(servicesXml).hasNoXPath("//*[local-name()='rest-connector']//*[local-name()='authentication']");
   }

   @Test
   public void should_adjust_configuration_templates() {
      //when
      ConfigurationScriptInvoker.Result result = configurationScriptInvoker.invokeScript(jbossHome, "caching-service", requiredScriptParameters);

      //then
      ResultAssertion.assertThat(result).printResult().isOk();

      XmlAssertion.assertThat(servicesXml)
         .hasXPath("//*[local-name()='distributed-cache-configuration' and @name='caching-service']")
         .hasNoXPath("//*[local-name()='distributed-cache-configuration' and @name='shared-memory-service']")
         .hasXPath("//*[local-name()='distributed-cache' and @name='default' and @configuration='caching-service']")
         .hasXPath("//*[local-name()='distributed-cache' and @name='memcachedCache' and @configuration='caching-service']")
         .hasNoXPath("//*[local-name()='distributed-cache-configuration' and @name='async']")
         .hasNoXPath("//*[local-name()='distributed-cache-configuration' and @name='indexed']")
         .hasNoXPath("//*[local-name()='distributed-cache-configuration' and @name='memory-bounded']")
         .hasNoXPath("//*[local-name()='distributed-cache-configuration' and @name='persistent-file-store-passivation']")
         .hasNoXPath("//*[local-name()='distributed-cache-configuration' and @name='persistent-file-store']")
         .hasNoXPath("//*[local-name()='distributed-cache-configuration' and @name='persistent-file-store-write-behind']")
         .hasNoXPath("//*[local-name()='distributed-cache-configuration' and @name='persistent-jdbc-binary-keyed']")
         .hasNoXPath("//*[local-name()='distributed-cache-configuration' and @name='persistent-jdbc-string-keyed']")
         .hasNoXPath("//*[local-name()='distributed-cache-configuration' and @name='persistent-leveldb-store']")
         .hasNoXPath("//*[local-name()='distributed-cache-configuration' and @name='transactional']");
//  https://issues.jboss.org/browse/ISPN-8341
//         .hasNoXPath("//*[local-name()='distributed-cache-configuration' and @name='default']")
//         .hasNoXPath("//*[local-name()='distributed-cache-configuration' and @name='memcachedCache']");
   }

}
