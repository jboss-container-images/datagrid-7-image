package org.infinispan.online.service.scaling;

import io.fabric8.openshift.client.OpenShiftClient;
import org.infinispan.online.service.endpoint.HotRodTester;
import org.infinispan.online.service.utils.OpenShiftCommandlineClient;
import org.infinispan.online.service.utils.ReadinessCheck;
import org.infinispan.online.service.utils.Waiter;

import java.net.URL;

public class ScalingTester {

   public void testFormingAClusterAfterScalingUp(String statefulSetName, URL hotRodService, OpenShiftCommandlineClient commandlineClient, ReadinessCheck readinessCheck, OpenShiftClient client, HotRodTester hotRodTester) {
      //given
      Waiter waiter = new Waiter();

      //when
      commandlineClient.scaleStatefulSet(statefulSetName, 2);
      readinessCheck.waitUntilTargetNumberOfReplicasAreReady(statefulSetName, 2, client);

      //then
      //Even though the Pods are ready, there might be bad timing in the discovery protocol
      //and Pods didn't manage to form a cluster yet.
      //We need to wait in a loop to see it that really happened.
      try {
         waiter.waitFor(() -> hotRodTester.getNumberOfNodesInTheCluster(hotRodService) == 2);
      } finally {
         //cleanup
         commandlineClient.scaleStatefulSet(statefulSetName, 1);
         readinessCheck.waitUntilTargetNumberOfReplicasAreReady(statefulSetName, 1, client);
      }
   }

}
