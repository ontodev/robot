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
                sh 'git clone https://github.com/obi-ontology/obi.git'
                sh 'mkdir -p obi/build'
                sh 'cp bin/robot.jar obi/build/robot.jar'
                sh 'cd obi'
                sh 'make test'
                sh 'cd ..'
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
