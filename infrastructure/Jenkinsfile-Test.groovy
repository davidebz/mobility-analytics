pipeline {
    agent {
        dockerfile {
            filename 'docker/dockerfile-java'
            additionalBuildArgs '--build-arg JENKINS_USER_ID=`id -u jenkins` --build-arg JENKINS_GROUP_ID=`id -g jenkins`'
        }
    }

    environment {
        TESTSERVER_TOMCAT_ENDPOINT=credentials('testserver-tomcat8-url')
        TESTSERVER_TOMCAT_CREDENTIALS=credentials('testserver-tomcat8-credentials')
        THUNDERFOREST_MAPS=credentials('thunderforest_api_key')
    }

    stages {
        stage('Configure') {
            steps {
                sh 'sed -i -e "s/<\\/settings>$//g\" ~/.m2/settings.xml'
                sh 'echo "    <servers>" >> ~/.m2/settings.xml'
                sh 'echo "        ${TESTSERVER_TOMCAT_CREDENTIALS}" >> ~/.m2/settings.xml'
                sh 'echo "    </servers>" >> ~/.m2/settings.xml'
                sh 'echo "</settings>" >> ~/.m2/settings.xml'
                sh 'jq \'.endpoints[0].url="http://tomcat.testingmachine.eu"\' src/main/webapp/WEB-INF/config.json > tmpFile && mv tmpFile src/main/webapp/WEB-INF/config.json'
                sh '''sed -i -e "s/\\(var thunderforest_api_key =\\).*/\\1'${THUNDERFOREST_MAPS}'/g" src/main/webapp/config.js'''

            }
        }
        stage('Test') {
            steps {
                sh 'mvn -B clean test'
            }
        }
        stage('Build') {
            steps {
                sh 'mvn -B clean package'
            }
        }
        stage('Deploy') {
            steps {
                sh 'mvn -B tomcat:redeploy -Dmaven.tomcat.url=${TESTSERVER_TOMCAT_ENDPOINT} -Dmaven.tomcat.server=testServer'
            }
        }
    }
}
