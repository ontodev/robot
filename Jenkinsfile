pipeline {
    agent any
    environment {
        TARGET_ADMIN_EMAILS = 'james@overton.ca'
    }

    stages {
        stage('Build') {
            steps {
                sh 'mvn clean package'
            }
        }

        stage('Test') {
            steps {
                script {
                    if (env.BRANCH_NAME == 'master') {
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
        failure {
        echo "There has been a failure in the ${env.BRANCH_NAME} pipeline."
        mail bcc: '', body: "There has been a pipeline failure in ${env.BRANCH_NAME}. Please see: https://build.obolibrary.io/job/ontodev/job/robot/job/${env.BRANCH_NAME}", cc: '', from: '', replyTo: '', subject: "ROBOT Integration Test FAIL", to: "${TARGET_ADMIN_EMAILS}"
        }
    }
}
