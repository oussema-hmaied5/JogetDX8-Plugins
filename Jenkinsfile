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
        SONARQUBE_SERVER = 'http://localhost:9000'
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

        stage('Build and Test with Maven') {
            steps {
                script {
                    withEnv(["MAVEN_OPTS=-Dmaven.repo.local=C:\\Jenkins\\repository"]) {
                        bat "mvn clean install -X -f ${params.PLUGIN_NAME}/pom.xml"
                        bat "mvn test -f ${params.PLUGIN_NAME}/pom.xml"
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
                scannerHome = tool 'SonarScanner'
            }
            steps {
                script {
                    try {
                        bat """
                            set MAVEN_OPTS=-Dmaven.repo.local=C:\\Jenkins\\repository
                            mvn sonar:sonar -Dsonar.login=admin -Dsonar.password=admin1 -Dsonar.projectKey=${params.PLUGIN_NAME} -Dsonar.host.url=${SONARQUBE_SERVER} -Dsonar.coverage.jacoco.xmlReportPaths=${params.PLUGIN_NAME}/target/site/jacoco/jacoco.xml -f ${params.PLUGIN_NAME}/pom.xml
                        """
                    } catch (Exception e) {
                        echo "Failed to execute SonarQube analysis: ${e.message}"
                        throw e
                    }
                }
            }
        }

        stage("Quality Gate") {
            steps {
                timeout(time: 30, unit: 'MINUTES') {
                    script {
                        def qg = waitForQualityGate()
                        if (qg.status != 'OK') {
                            error "Failed to pass the quality gate. Status: ${qg.status}"
                        }
                    }
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
