package org.infinispan.online.service.caching.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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

   public Result invokeScript(Path cli, String profile) {
      Path script = Paths.get(".", "src/main/bash/jdg-online-configuration.sh");
      Path profiles = Paths.get(".", "src/main/bash/profiles");
      if (!Files.exists(script)) {
         throw new IllegalStateException("The script file does not exist");
      }

      List<String> command = new ArrayList<>();
      command.add(script.toAbsolutePath().toString());
      command.add("--profile=" + profile);
      command.add("--cli-bin=" + cli.toAbsolutePath().toString());
      command.add("--profiles-directory=" + profiles.toAbsolutePath().toString());

      try {
         Process p = new ProcessBuilder(command).start();
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
