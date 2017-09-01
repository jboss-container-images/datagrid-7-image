package org.infinispan.configuration.converter.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestServerLocator {

   public Path locateServer() {
      Path serverPath = Paths.get(".");

      //This just for convenience of IntelliJ users.
      boolean usesProjectRoot = Files.exists(serverPath.resolve("target"));
      if (usesProjectRoot) {
         serverPath = serverPath.resolve("target");
      }

      serverPath = serverPath.resolve("jdg-caching-service-server");
      if (!Files.exists(serverPath)) {
         throw new IllegalStateException("jdg-caching-service-server doesn't exist in target. Make sure the server has been properly downloaded");
      }

      try {
         serverPath = Files.list(serverPath).findFirst().get();
      } catch (Exception e) {
         throw new IllegalStateException("Error while searching for JDG Caching server", e);
      }

      return serverPath;
   }

   public Path locateCLI() {
      Path cliPath = locateServer();

      cliPath = cliPath.resolve("bin");

      Path ispnCli = cliPath.resolve("ispn-cli.sh");
      Path jdgCli = cliPath.resolve("cli.sh");
      if (Files.exists(jdgCli)) {
         return jdgCli;
      } else if (Files.exists(ispnCli)) {
         return ispnCli;
      }

      throw new IllegalStateException("No cli has been found");
   }

}
