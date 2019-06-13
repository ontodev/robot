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
}
