pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                sh 'mvn clean package'
            }
        }
        stage('Test') {
            steps {
                sh 'rm -f bin/original-robot.jar'
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'bin/*.jar', fingerprint: true
        }
    }
}
