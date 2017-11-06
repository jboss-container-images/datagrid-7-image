package org.infinispan.online.service.utils;


import java.util.Random;

public class TestObjectCreator {

   private static final Random random = new Random();

   public String getRandomString(int length) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < length; i++) {
         char c = (char) (random.nextInt((int) (Character.MAX_VALUE)));
         sb.append(c);
      }
      return sb.toString();
   }

}
