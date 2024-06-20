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
            steps {
                script {
                    withSonarQubeEnv('SonarQube') {
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
        }

        stage('Quality Gate') {
            steps {
                script {
                    def qg = waitForQualityGate()
                    if (qg.status != 'OK') {
                        error "Failed to pass the quality gate. Status: ${qg.status}"
                    } else {
                        echo "Test Passed Successfully! Status: ${qg.status}"
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
                    def pluginFound = false

                    for (int i = 0; i < retries; i++) {
                        try {
                            def response = bat(
                                script: """
                                    curl -s -u ${JOGET_USERNAME}:${JOGET_PASSWORD} "${JOGET_URL}/web/json/plugin/list?start=0&rows=200"
                                """,
                                returnStdout: true
                            ).trim()

                            def jsonResponse = readJSON text: response
                            def plugins = jsonResponse?.data?.collect { it.id }

                            if (plugins.contains(params.PLUGIN_NAME)) {
                                echo 'Plugin deployment verified successfully.'
                                pluginFound = true
                                break
                            } else {
                                echo 'Plugin not found in Joget. Retrying...'
                                sleep(waitTime)
                            }
                        } catch (Exception e) {
                            echo "Failed to verify deployment: ${e.message}. Retrying..."
                            sleep(waitTime)
                        }
                    }

                    if (!pluginFound) {
                        error 'Failed to verify deployment after multiple retries.'
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
