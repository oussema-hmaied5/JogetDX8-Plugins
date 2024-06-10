pipeline {
    agent any

    environment {
        DOCKER_CONTAINER = 'joget-container'
        JOGET_URL = 'http://localhost:8080/jw'
        SONAR_HOST_URL = 'http://localhost:9000'
        SONAR_TOKEN = credentials('sonar-token')
    }

    stages {
        stage('Build') {
            steps {
                script {
                    bat 'mvn clean install'
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    bat "mvn sonar:sonar -Dsonar.host.url=${env.SONAR_HOST_URL} -Dsonar.login=${env.SONAR_TOKEN}"
                }
            }
            post {
                success {
                    script {
                        echo 'SonarQube analysis completed successfully'
                    }
                }
                failure {
                    script {
                        echo 'SonarQube analysis failed'
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
                            sleep waitTime
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
