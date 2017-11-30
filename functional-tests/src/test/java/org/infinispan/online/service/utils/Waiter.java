package org.infinispan.online.service.utils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;

public class Waiter {

   public static final int DEFAULT_TIMEOUT = 30;
   public static final TimeUnit DEFAULT_TIMEOUT_UNIT = TimeUnit.SECONDS;

   public void waitFor(BooleanSupplier condition) {
      waitFor(condition, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT);
   }

   public void waitFor(BooleanSupplier condition, long timeout, TimeUnit unit) {
      long endTime = TimeUnit.MILLISECONDS.convert(timeout, unit) + System.currentTimeMillis();
      long backoffCounter = 0;
      while (System.currentTimeMillis() - endTime < 0) {
         if (condition.getAsBoolean()) {
            break;
         }
         LockSupport.parkNanos(++backoffCounter * 100_000_000);
      }
   }

}
