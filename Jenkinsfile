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
                script {
                    if (env.BRANCH_NAME == 'obi-test') {
                        try {
                            sh 'git clone https://github.com/obi-ontology/obi.git'
                            sh 'mkdir -p obi/build'
                            sh 'cp bin/robot.jar obi/build/robot.jar'
                            sh 'cd obi && make test'
                        } finally {
                            sh 'rm -rf obi'
                        }
                    } else {
                        sh 'java -jar bin/robot.jar help'
                    }
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'bin/*.jar', fingerprint: true
        }
    }
}
