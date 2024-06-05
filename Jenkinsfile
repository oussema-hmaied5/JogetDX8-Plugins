pipeline {
    agent any

    parameters {
        string(name: 'PLUGIN_NAME', defaultValue: 'form_repeater', description: 'Name of the plugin to test and deploy')
    }

    environment {
        DOCKER_CONTAINER = 'Joget-DX8'
        JOGET_URL = 'http://localhost:8067/jw'
        JOGET_CREDENTIALS_ID = 'joget-credentials' // The ID of the stored credentials
    }

    triggers {
        githubPush()
    }

    stages {
        stage('Checkout SCM') {
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
                bat "mvn clean install -X -f ${params.PLUGIN_NAME}/pom.xml" // Added -X for debug output
            }
        }

        stage('Deploy to Docker Joget') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: env.JOGET_CREDENTIALS_ID, passwordVariable: 'JOGET_PASSWORD', usernameVariable: 'JOGET_USERNAME')]) {
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
        }

        stage('Verify Deployment') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: env.JOGET_CREDENTIALS_ID, passwordVariable: 'JOGET_PASSWORD', usernameVariable: 'JOGET_USERNAME')]) {
                        def retries = 5
                        def waitTime = 30 // seconds
                        for (int i = 0; i < retries; i++) {
                            try {
                                bat """
                                    docker exec ${DOCKER_CONTAINER} curl -u ${JOGET_USERNAME}:${JOGET_PASSWORD} -f ${JOGET_URL}/web/json/plugin/${params.PLUGIN_NAME}/status
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
