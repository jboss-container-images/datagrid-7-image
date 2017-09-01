package org.infinispan.configuration.converter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.infinispan.configuration.converter.util.ConfigurationScriptInvoker;
import org.infinispan.configuration.converter.util.TestResourceLocator;
import org.infinispan.configuration.converter.util.TestServerLocator;
import org.infinispan.configuration.converter.assertions.ResultAssertion;
import org.infinispan.configuration.converter.assertions.XmlAssertion;
import org.junit.Before;
import org.junit.Test;

public class CachingServiceTest {

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
   public void should_successfully_transform_cloud_xml() {
     //when
      ConfigurationScriptInvoker.Result result = configurationScriptInvoker.invokeScript(cli, "caching-service");

      //then
      ResultAssertion.assertThat(result).printResult().isOk();
      XmlAssertion.assertThat(cloudXml).hasNoXPath("//*[local-name()='memcached-connector']");
      XmlAssertion.assertThat(cloudXml).hasNoXPath("//*[local-name()='rest-connector']");
      XmlAssertion.assertThat(cloudXml).hasXPath("//*[local-name()='hotrod-connector']");

      XmlAssertion.assertThat(cloudXml).hasXPath("//*[local-name()='stack' and @name='kubernetes']");
   }
}
