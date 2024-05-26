pipeline {
    agent any

    parameters {
        string(name: 'PLUGIN_NAME', defaultValue: 'form_repeater', description: 'Name of the plugin to test and deploy')
    }

    environment {
        DOCKER_CONTAINER = 'Joget-DX8'
        JOGET_USERNAME = credentials('admin')
        JOGET_PASSWORD = credentials('admin')
    }

    stages {
        stage('Checkout') {
            steps {
                git 'https://github.com/oussema-hmaied5/JogetDX8-Plugins'
            }
        }

        stage('Set up Java') {
            steps {
                sh 'java -version'
            }
        }

        stage('Build with Maven') {
            steps {
                sh "mvn clean install -f ${params.PLUGIN_NAME}/pom.xml"
            }
        }

        stage('Run Unit Tests') {
            steps {
                sh "mvn test -f ${params.PLUGIN_NAME}/pom.xml"
            }
        }

        stage('Deploy to Docker Joget') {
            steps {
                sh """
                    docker cp ${params.PLUGIN_NAME}/target/${params.PLUGIN_NAME}.jar ${DOCKER_CONTAINER}:/opt/joget/wflow/app_plugins/${params.PLUGIN_NAME}.jar
                    docker restart ${DOCKER_CONTAINER}
                """
            }
        }

        stage('Upload Plugin to Joget') {
            steps {
                sh """
                    sleep 30  # Attendre que Joget redémarre et déploie le plugin
                    curl -X POST -u ${JOGET_USERNAME}:${JOGET_PASSWORD} -F "file=@${params.PLUGIN_NAME}/target/${params.PLUGIN_NAME}.jar" http://localhost:8080/jw/web/json/plugin/upload
                """
            }
        }

        stage('Verify Deployment') {
            steps {
                sh """
                    sleep 30  # Attendre que Joget redémarre et déploie le plugin
                    docker exec ${DOCKER_CONTAINER} curl -f http://localhost:8080/jw/web/json/plugin/${params.PLUGIN_NAME}/status
                """
            }
        }
    }
}
