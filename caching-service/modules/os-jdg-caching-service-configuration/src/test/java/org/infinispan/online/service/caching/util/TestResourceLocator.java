package org.infinispan.online.service.caching.util;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestResourceLocator {

   public Path locateFile(String testResource) {
      URL location = this.getClass().getResource("/" + testResource);
      try {
         return Paths.get(location.toURI());
      } catch (URISyntaxException e) {
         throw new AssertionError(e);
      }
   }

}
