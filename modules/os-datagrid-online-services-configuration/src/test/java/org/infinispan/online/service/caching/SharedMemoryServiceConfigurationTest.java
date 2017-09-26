package org.infinispan.online.service.caching;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

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

   Path cloudXml = testServerLocator.locateServer().resolve("standalone/configuration/cloud.xml");
   Path cli = testServerLocator.locateCLI();

   @Before
   public void beforeTest() throws IOException {
      Path baselineConfiguration = testResourceLocator.locateFile("caching-service/cloud-7.1.xml");
      Files.copy(baselineConfiguration, cloudXml, StandardCopyOption.REPLACE_EXISTING);
   }

   @Test
   public void should_leave_all_endpoints() {
     //when
      ConfigurationScriptInvoker.Result result = configurationScriptInvoker.invokeScript(cli, "shared-memory-service");

      //then
      ResultAssertion.assertThat(result).printResult().isOk();
      XmlAssertion.assertThat(cloudXml).hasXPath("//*[local-name()='memcached-connector']");
      XmlAssertion.assertThat(cloudXml).hasXPath("//*[local-name()='rest-connector']");
      XmlAssertion.assertThat(cloudXml).hasXPath("//*[local-name()='hotrod-connector']");
   }

   @Test
   public void should_add_kube_ping() {
      //when
      ConfigurationScriptInvoker.Result result = configurationScriptInvoker.invokeScript(cli, "shared-memory-service");

      //then
      ResultAssertion.assertThat(result).printResult().isOk();

      XmlAssertion.assertThat(cloudXml)
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
   public void should_adjust_configuration_templates() {
      //when
      ConfigurationScriptInvoker.Result result = configurationScriptInvoker.invokeScript(cli, "shared-memory-service");

      //then
      ResultAssertion.assertThat(result).printResult().isOk();

      XmlAssertion.assertThat(cloudXml)
         .hasXPath("//*[local-name()='distributed-cache-configuration' and @name='persistent-file-store-write-behind']")
         .hasNoXPath("//*[local-name()='distributed-cache-configuration' and @name='async']")
         .hasNoXPath("//*[local-name()='distributed-cache-configuration' and @name='indexed']")
         .hasNoXPath("//*[local-name()='distributed-cache-configuration' and @name='memory-bounded']")
         .hasNoXPath("//*[local-name()='distributed-cache-configuration' and @name='persistent-file-store-passivation']")
         .hasNoXPath("//*[local-name()='distributed-cache-configuration' and @name='persistent-file-store']")
         .hasNoXPath("//*[local-name()='distributed-cache-configuration' and @name='persistent-jdbc-binary-keyed']")
         .hasNoXPath("//*[local-name()='distributed-cache-configuration' and @name='persistent-jdbc-string-keyed']")
         .hasNoXPath("//*[local-name()='distributed-cache-configuration' and @name='persistent-leveldb-store']")
         .hasNoXPath("//*[local-name()='distributed-cache-configuration' and @name='transactional']")
//  https://issues.jboss.org/browse/ISPN-8341
//         .hasNoXPath("//*[local-name()='distributed-cache-configuration' and @name='default']")
//         .hasNoXPath("//*[local-name()='distributed-cache-configuration' and @name='memcachedCache']")
         .hasXPath("//*[local-name()='distributed-cache' and @name='default' and @configuration='persistent-file-store-write-behind']")
         .hasXPath("//*[local-name()='distributed-cache' and @name='memcachedCache' and @configuration='persistent-file-store-write-behind']");
   }
}
