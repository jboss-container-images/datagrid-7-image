#!/usr/bin/env groovy

pipeline {
   agent any

   options {
      timeout(time: 1, unit: 'HOURS')
   }

   stages {
      stage('Prepare') {
         steps {
            // Workaround for JENKINS-47230
            script {
               env.MAVEN_HOME = tool('Maven')

               // See https://github.com/openshift/origin/issues/15038#issuecomment-345252400
               sh 'sudo rm -rf /usr/share/rhel/secrets'
            }

            checkout scm
         }
      }

      stage('Unit tests') {
         steps {
            script {
               configFileProvider([configFile(fileId: 'maven-settings-with-prod', variable: 'MAVEN_SETTINGS')]) {
                  script {
                     sh 'make MVN_COMMAND="$MAVEN_HOME/bin/mvn -s $MAVEN_SETTINGS" test-unit'
                  }
               }
            }
         }
      }

      stage('Functional and OpenShift tests') {
         steps {
            script {
               configFileProvider([configFile(fileId: 'maven-settings-with-prod', variable: 'MAVEN_SETTINGS')]) {
                  script {
                     try {
                        sh 'make MVN_COMMAND="$MAVEN_HOME/bin/mvn -s $MAVEN_SETTINGS" start-openshift-with-catalog build-image push-image-to-local-openshift test-functional'
                     } finally {
                        sh 'make MVN_COMMAND="$MAVEN_HOME/bin/mvn -s $MAVEN_SETTINGS" stop-openshift clean-docker'
                     }
                  }
               }
            }
         }
      }

      stage('Gather test results') {
         steps {
            script {
               junit allowEmptyResults: true, testResults: '**/target/*-reports/*.xml'
            }
         }
      }
   }
}
