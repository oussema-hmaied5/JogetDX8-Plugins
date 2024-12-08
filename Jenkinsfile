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
        NOTIFY_EMAIL = 'Oussama.hamaied@hotmail.fr'
    }

    triggers {
        githubPush()
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                    currentBuild.description = "Checkout Stage"
                }
                git branch: 'main', credentialsId: 'github-credentials', url: 'https://github.com/Oussema-hmaied5/JogetDX8-Plugins.git'
            }
        }

        stage('Set up Java') {
            steps {
                script {
                    currentBuild.description = "Set up Java Stage"
                }
                bat 'java -version'
            }
        }

        stage('Build and Test with Maven') {
            steps {
                script {
                    currentBuild.description = "Build and Test Stage"
                    withEnv(["MAVEN_OPTS=-Dmaven.repo.local=C:\\Jenkins\\repository"]) {
                        bat "mvn clean install -X -f ${params.PLUGIN_NAME}/pom.xml"
                        bat "mvn test -f ${params.PLUGIN_NAME}/pom.xml"
                    }
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                script {
                    currentBuild.description = "SonarQube Analysis Stage"
                    withSonarQubeEnv('SonarQube') {
                        bat """
                            set MAVEN_OPTS=-Dmaven.repo.local=C:\\Jenkins\\repository
                            mvn sonar:sonar -Dsonar.login=admin -Dsonar.password=admin1 -Dsonar.projectKey=${params.PLUGIN_NAME} -Dsonar.host.url=${SONARQUBE_SERVER} -Dsonar.coverage.jacoco.xmlReportPaths=${params.PLUGIN_NAME}/target/site/jacoco/jacoco.xml -f ${params.PLUGIN_NAME}/pom.xml
                        """
                    }
                }
            }
        }

        stage('Quality Gate') {
            steps {
                script {
                    currentBuild.description = "Quality Gate Stage"
                    def qg = waitForQualityGate()
                    if (qg.status != 'OK') {
                        error "Failed to pass the quality gate. Status: ${qg.status}"
                    }
                }
            }
        }

        stage('Verify Docker Setup') {
            steps {
                script {
                    currentBuild.description = "Verify Docker Setup Stage"
                    bat "docker ps | findstr ${DOCKER_CONTAINER}"
                }
            }
        }

        stage('Deploy to Docker Joget') {
            steps {
                script {
                    currentBuild.description = "Deploy to Docker Joget Stage"
                    retry(3) {
                        bat """
                            docker cp ${params.PLUGIN_NAME}\\target\\${params.PLUGIN_NAME}-0.0.1-SNAPSHOT.jar ${DOCKER_CONTAINER}:/opt/joget/wflow/app_plugins/${params.PLUGIN_NAME}.jar
                            docker restart ${DOCKER_CONTAINER}
                        """
                    }
                }
            }
        }

        stage('Run UI Tests') {
            steps {
                script {
                    currentBuild.description = "Run UI Tests Stage"
                    bat "mvn clean test -f ui-tests/pom.xml -Dtest=${params.PLUGIN_NAME}"
                }
            }
        }

        stage('Archive Artifacts') {
            steps {
                script {
                    currentBuild.description = "Archive Artifacts Stage"
                }
                archiveArtifacts artifacts: "${params.PLUGIN_NAME}/target/*.jar", allowEmptyArchive: true
            }
        }
    }

    post {
        always {
            cleanWs()
            script {
                echo 'This will always run'
            }
        }

        success {
            script {
                def successMessage = """
                Build Success: ${env.JOB_NAME} #${env.BUILD_NUMBER}
                Last Executed Stage: ${currentBuild.description}
                Please check the build details for more information.
                """

                emailext(
                    to: "${NOTIFY_EMAIL}",
                    subject: "Jenkins Build Success: ${currentBuild.fullDisplayName}",
                    body: successMessage
                )
            }
        }

        failure {
            script {
                def failureMessage = """
                Build Failure: ${env.JOB_NAME} #${env.BUILD_NUMBER}
                Failed Stage: ${currentBuild.description}
                Please check the Jenkins logs for more details.
                """

                emailext(
                    to: "${NOTIFY_EMAIL}",
                    subject: "Jenkins Build Failure: ${currentBuild.fullDisplayName}",
                    body: failureMessage
                )
            }
        }
    }
}
