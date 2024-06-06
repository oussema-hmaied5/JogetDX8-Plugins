pipeline {
    agent any

    parameters {
        string(name: 'PLUGIN_NAME', defaultValue: 'form_repeater', description: 'Name of the plugin to test and deploy')
    }

    environment {
        DOCKER_CONTAINER = 'Joget-DX8'
        JOGET_URL = 'http://localhost:8067/jw'
        JOGET_USERNAME = 'admin'
        JOGET_PASSWORD = 'admin'
        SONARQUBE_SERVER = 'http://localhost:9099' // Your SonarQube server URL
        SONARQUBE_TOKEN = 'your_generated_token'  // Your SonarQube token
    }

    triggers {
        githubPush()
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', credentialsId: 'github-credentials', url: 'https://github.com/Oussema-hmaied5/JogetDX8-Plugins.git'
            }
        }

        stage('Set up Java') {
            steps {
                bat 'java -version'
            }
        }

        stage('Build with Maven') {
            steps {
                script {
                    withEnv(["MAVEN_OPTS=-Dmaven.repo.local=C:\\Jenkins\\repository"]) {
                        bat "mvn clean install -X -f ${params.PLUGIN_NAME}/pom.xml"
                    }
                }
            }
        }

        stage('Verify Build Output') {
            steps {
                script {
                    bat "dir ${params.PLUGIN_NAME}\\target"
                }
            }
        }

         stage('SonarQube Analysis') {
                  environment {
                      scannerHome = tool 'SonarQube Scanner' // Name of the SonarQube scanner tool installed in Jenkins
                  }
                  steps {
                      withCredentials([string(credentialsId: 'jenkins-sonarQube', variable: 'SONARQUBE_TOKEN')]) {
                          withSonarQubeEnv('sq1') { // 'sq1' is the name of the SonarQube server configured in Jenkins
                              bat "${scannerHome}/bin/sonar-scanner -Dsonar.projectKey=${params.PLUGIN_NAME} -Dsonar.sources=. -Dsonar.host.url=${SONARQUBE_SERVER} -Dsonar.login=${SONARQUBE_TOKEN}"
                          }
                      }
                  }
              }

        stage('Quality Gate') {
            steps {
                timeout(time: 1, unit: 'HOURS') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Deploy to Docker Joget') {
            steps {
                script {
                    try {
                        bat """
                            docker cp ${params.PLUGIN_NAME}\\target\\${params.PLUGIN_NAME}-0.0.1-SNAPSHOT.jar ${DOCKER_CONTAINER}:/opt/joget/wflow/app_plugins/${params.PLUGIN_NAME}.jar
                            docker restart ${DOCKER_CONTAINER}
                        """
                        echo 'Plugin copied and Joget restarted successfully.'
                    } catch (Exception e) {
                        error 'Failed to copy plugin or restart Joget.'
                    }
                }
            }
        }

        stage('Verify Deployment') {
            steps {
                script {
                    def retries = 5
                    def waitTime = 30 // seconds
                    for (int i = 0; i < retries; i++) {
                        try {
                            bat """
                                docker exec ${DOCKER_CONTAINER} curl -f ${JOGET_URL}/web/json/plugin/${params.PLUGIN_NAME}/status
                            """
                            echo 'Plugin deployment verified successfully.'
                            break
                        } catch (Exception e) {
                            echo 'Retrying...'
                            sleep(waitTime)
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                echo 'This will always run'
            }
        }
        success {
            script {
                echo 'This will run only if the pipeline succeeds'
            }
        }
        failure {
            script {
                echo 'This will run only if the pipeline fails'
            }
        }
    }
}
