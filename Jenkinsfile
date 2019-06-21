pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                sh 'mvn clean package'
                sh 'ls ./bin'
            }
        }
    }
  
    post {
        always {
            archiveArtifacts artifacts: 'bin/robot.jar', fingerprint: true
        }
    }
}
