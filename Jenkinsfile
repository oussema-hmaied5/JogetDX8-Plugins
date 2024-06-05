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
