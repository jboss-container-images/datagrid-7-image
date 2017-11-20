package org.infinispan.online.service.caching.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConfigurationScriptInvoker {

   public static class Result {
      private final int resultCode;
      private final String output;

      public Result(int resultCode, String output) {
         this.resultCode = resultCode;
         this.output = output;
      }

      public int getResultCode() {
         return resultCode;
      }

      public String getOutput() {
         return output;
      }
   }

   public Result invokeScript(Path jbossHome, String profile, Map<String, String> parameters, Map<String, String> envVariables) {
      Path script = Paths.get(".", "src/main/bash/jdg-online-configuration.sh");
      Path profiles = Paths.get(".", "src/main/bash/profiles");
      if (!Files.exists(script)) {
         throw new IllegalStateException("The script file does not exist");
      }

      List<String> command = new ArrayList<>();
      command.add(script.toAbsolutePath().toString());
      command.add("--profile");
      command.add(profile);
      command.add("--jboss-home");
      command.add(jbossHome.toAbsolutePath().toString());
      command.add("--profiles-directory");
      command.add(profiles.toAbsolutePath().toString());
      parameters.forEach((k, v) -> command.add(k + "=" + v));

      try {
         ProcessBuilder pb = new ProcessBuilder(command);
         pb.environment().putAll(envVariables);

         Process p = pb.start();
         int resultCode = p.waitFor();
         BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));

         StringBuilder sb = new StringBuilder();
         String line;
         while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
         }
         String output = sb.toString();
         return new Result(resultCode, output);
      } catch (Exception e) {
         throw new IllegalStateException("Caught exception in child process", e);
      }
   }

}
