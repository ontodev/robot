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
                sh 'mkdir -p obi/build'
                sh 'cp bin/robot.jar obi/build/robot.jar'
                sh 'cd obi && make test'
                sh 'rm -rf obi'
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'bin/*.jar', fingerprint: true
        }
    }
}
