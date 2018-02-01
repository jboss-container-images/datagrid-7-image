package org.infinispan.online.service.utils;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Base64;

import io.fabric8.openshift.client.OpenShiftClient;

public class TrustStore {
   public static final char[] TRUSTSTORE_PASSWORD = "test99".toCharArray();

   private OpenShiftClient client;
   private String trustStoreDir;
   private String serviceName;

   public TrustStore(OpenShiftClient client, String serviceName) {
      this.client = client;
      this.serviceName = serviceName;
      String jbossBaseDir = System.getProperty("jboss.server.base.dir");
      if (jbossBaseDir != null) {
         trustStoreDir = jbossBaseDir + "/data";
         create();
      }
   }

   public TrustStore(OpenShiftClient client, String trustStoreDir, String serviceName) {
      this.client = client;
      this.serviceName = serviceName;
      this.trustStoreDir = trustStoreDir;
      create();
   }

   public String getPath() {
      Path currentRelativePath = Paths.get(trustStoreDir);
      String absolutePath = currentRelativePath.toAbsolutePath().toString();
      return String.format("%s/%s.jks", absolutePath, serviceName);
   }

   private KeyStore create() {
      String certSecret = client.secrets().withName("service-certs").get().getData().get("tls.crt");
      assert certSecret != null;

      try (InputStream input = Base64.getDecoder().wrap(new ByteArrayInputStream(certSecret.getBytes(StandardCharsets.UTF_8)));
           FileOutputStream output = new FileOutputStream(getPath())) {

         KeyStore trustStore = KeyStore.getInstance("JKS");
         CertificateFactory cf = CertificateFactory.getInstance("X.509");
         Certificate certificate = cf.generateCertificate(input);
         trustStore.load(null, null);
         trustStore.setCertificateEntry(serviceName, certificate);
         trustStore.store(output, TRUSTSTORE_PASSWORD);
         return trustStore;
      } catch (Exception e) {
         throw new IllegalStateException(e);
      }
   }
}
