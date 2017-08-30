package org.infinispan.configuration.converter.assertions;

import java.nio.file.Path;

import javax.xml.transform.Source;

import org.assertj.core.api.AbstractAssert;
import org.w3c.dom.Node;
import org.xmlunit.builder.Input;
import org.xmlunit.xpath.JAXPXPathEngine;
import org.xmlunit.xpath.XPathEngine;

public class XmlAssertion extends AbstractAssert<XmlAssertion, Source> {

   private XmlAssertion(Source source) {
      super(source, XmlAssertion.class);
   }

   public static XmlAssertion assertThat(Path xmlFile) {
      return new XmlAssertion(Input.fromFile(xmlFile.toFile()).build());
   }

   public void hasXPath(String xpath) {
      XPathEngine xpathEngine = new JAXPXPathEngine();
      Iterable<Node> nodes = xpathEngine.selectNodes(xpath, actual);
      if (!nodes.iterator().hasNext()) {
         failWithMessage("XPath %s is not present in the XML file (but should be)", xpath);
      }
   }

   public void hasNoXPath(String xpath) {
      XPathEngine xpathEngine = new JAXPXPathEngine();
      Iterable<Node> nodes = xpathEngine.selectNodes(xpath, actual);
      if (nodes.iterator().hasNext()) {
         failWithMessage("XPath %s is present in the XML file (but should be)", xpath);
      }
   }
}
