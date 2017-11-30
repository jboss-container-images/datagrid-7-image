package org.infinispan.online.service.scaling;

import io.fabric8.openshift.client.OpenShiftClient;
import org.infinispan.online.service.endpoint.HotRodTester;
import org.infinispan.online.service.utils.OpenShiftCommandlineClient;
import org.infinispan.online.service.utils.ReadinessCheck;

import java.net.URL;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ScalingTester {

   public void testFormingAClusterAfterScalingUp(String statefulSetName, URL hotRodService, OpenShiftCommandlineClient commandlineClient, ReadinessCheck readinessCheck, OpenShiftClient client, HotRodTester hotRodTester) {
      //when
      commandlineClient.scaleStatefulSet(statefulSetName, 2);
      readinessCheck.waitUntilTargetNumberOfReplicasAreReady(statefulSetName, 2, client);

      int numberOfNodesInCluster = hotRodTester.getNumberOfNodesInTheCluster(hotRodService);

      commandlineClient.scaleStatefulSet(statefulSetName, 1);
      readinessCheck.waitUntilTargetNumberOfReplicasAreReady(statefulSetName, 1, client);

      //then
      assertThat(numberOfNodesInCluster).isEqualTo(2);
   }

}
