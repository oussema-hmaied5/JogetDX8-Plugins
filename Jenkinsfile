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
        MAVEN_SETTINGS = 'maven-settings'
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
                bat "mvn clean install -f ${params.PLUGIN_NAME}/pom.xml"
            }
        }

        stage('Deploy to Docker Joget') {
            steps {
                script {
                    try {
                        bat """
                            docker cp ${params.PLUGIN_NAME}/target/${params.PLUGIN_NAME}.jar ${DOCKER_CONTAINER}:/opt/joget/wflow/app_plugins/${params.PLUGIN_NAME}.jar
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
                    try {
                        sleep(30)
                        bat """
                            docker exec ${DOCKER_CONTAINER} curl -f ${JOGET_URL}/web/json/plugin/${params.PLUGIN_NAME}/status
                        """
                        echo 'Plugin deployment verified successfully.'
                    } catch (Exception e) {
                        error 'Plugin deployment verification failed.'
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
