package org.infinispan.online.service.sharedmemory;

import io.fabric8.openshift.client.OpenShiftClient;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.infinispan.online.service.endpoint.HotRodTester;
import org.infinispan.online.service.scaling.ScalingTester;
import org.infinispan.online.service.utils.DeploymentHelper;
import org.infinispan.online.service.utils.OpenShiftClientCreator;
import org.infinispan.online.service.utils.OpenShiftCommandlineClient;
import org.infinispan.online.service.utils.OpenShiftHandle;
import org.infinispan.online.service.utils.ReadinessCheck;
import org.infinispan.online.service.utils.TrustStore;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.net.URL;

@RunWith(ArquillianConditionalRunner.class)
@RequiresOpenshift
public class ScalingTest {

   private static final String SERVICE_NAME = "shared-memory-service";

   String trustStoreDir = System.getProperty("jboss.server.base.dir") +  "/data";
   HotRodTester hotRodTester = new HotRodTester(SERVICE_NAME, trustStoreDir);
   ScalingTester scalingTester = new ScalingTester();
   OpenShiftCommandlineClient commandlineClient = new OpenShiftCommandlineClient();

   ReadinessCheck readinessCheck = new ReadinessCheck();
   OpenShiftClient client = OpenShiftClientCreator.getClient();
   OpenShiftHandle handle = new OpenShiftHandle(client);

   @Deployment
   public static Archive<?> deploymentApp() {
      return ShrinkWrap
         .create(WebArchive.class, "test.war")
         .addAsLibraries(DeploymentHelper.testLibs())
         .addPackage(ScalingTest.class.getPackage())
         .addPackage(ReadinessCheck.class.getPackage())
         .addPackage(ScalingTester.class.getPackage())
         .addPackage(HotRodTester.class.getPackage());
   }

   //must be run from the client where "oc" is installed
   @RunAsClient
   @InSequence(1)
   @Test
   public void scale_up() {
      scalingTester.scaleUpStatefulSet("shared-memory-service-app", client, commandlineClient, readinessCheck);
   }

   @InSequence(2)
   @Test
   public void should_discover_new_cluster_members_when_scaling_up() throws Exception {
      TrustStore.create(trustStoreDir, SERVICE_NAME, client);
      URL hotRodService = handle.getServiceWithName("shared-memory-service-app-hotrod");
      scalingTester.waitForClusterToForm(hotRodService, hotRodTester);
   }

   //must be run from the client where "oc" is installed
   @RunAsClient
   @InSequence(3)
   @Test
   public void scale_down() {
      scalingTester.scaleDownStatefulSet("shared-memory-service-app", client, commandlineClient, readinessCheck);
   }
}
