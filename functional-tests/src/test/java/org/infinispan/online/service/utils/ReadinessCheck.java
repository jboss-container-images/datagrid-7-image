package org.infinispan.online.service.utils;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.openshift.client.OpenShiftClient;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

public class ReadinessCheck {

   public void waitUntilAllPodsAreReady(OpenShiftClient client, long timeout, TimeUnit unit) {
      long endTime = TimeUnit.MILLISECONDS.convert(timeout, unit) + System.currentTimeMillis();
      long backoffCounter = 0;
      while (System.currentTimeMillis() - endTime < 0) {
         Set<Pod> notReadyPods = getNotReadyPods(client);
         if (notReadyPods.isEmpty()) {
            break;
         }
         System.out.println("Some pods are not ready: " + notReadyPods);
         LockSupport.parkNanos(++backoffCounter * 10_000_000);
      }
   }

   private Set<Pod> getNotReadyPods(OpenShiftClient client) {
      return client.pods().list().getItems().stream()
         .filter(pod -> pod.getStatus().getConditions().stream()
            .filter(condition -> "Ready".equals(condition.getType()))
            .filter(condition -> "False".equals(condition.getStatus()))
            .findAny().isPresent())
         .collect(Collectors.toSet());
   }
}
