#!/usr/bin/env groovy

pipeline {
   agent any
   stages {
      stage('SCM Checkout') {
         steps {
            checkout scm
         }
      }

      stage('Unit tests') {
         steps {
            script {
               configFileProvider([configFile(fileId: 'maven-settings-with-prod', variable: 'MAVEN_SETTINGS')]) {
                  script {
                     def mvnHome = tool 'Maven'
                     sh 'make MVN_COMMAND="${mvnHome}/bin/mvn -s $MAVEN_SETTINGS" test-unit'
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
                     def mvnHome = tool 'Maven'
                     try {
                        sh 'make MVN_COMMAND="${mvnHome}/bin/mvn -s $MAVEN_SETTINGS" start-openshift-with-catalog build-image push-image-to-local-openshift test-functional'
                     } finally {
                        sh 'make MVN_COMMAND="${mvnHome}/bin/mvn -s $MAVEN_SETTINGS" stop-openshift clean-docker'
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
