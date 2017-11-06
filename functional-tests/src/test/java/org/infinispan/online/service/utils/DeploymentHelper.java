package org.infinispan.online.service.utils;

import org.jboss.shrinkwrap.resolver.api.maven.Maven;

import java.io.File;

public class DeploymentHelper {

   public static File[] testLibs() {
      return Maven.resolver().loadPomFromFile("pom.xml")
         .resolve("org.infinispan:infinispan-client-hotrod",
            "io.fabric8:openshift-client",
            "org.assertj:assertj-core",
            "io.rest-assured:rest-assured")
         .withTransitivity().asFile();
   }
}
