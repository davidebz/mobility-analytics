pipeline {
    agent {
        dockerfile {
            filename 'docker/dockerfile-java'
            additionalBuildArgs '--build-arg JENKINS_USER_ID=`id -u jenkins` --build-arg JENKINS_GROUP_ID=`id -g jenkins`'
        }
    }
    environment{
        THUNDERFOREST_MAPS=credentials('thunderforest_api_key')
    }
    stages {
        stage('Configure'){
            steps{
                sh 'jq \'.endpoints[0].url="https://ipchannels.integreen-life.bz.it"\' src/main/webapp/WEB-INF/config.json > tmpFile && mv tmpFile src/main/webapp/WEB-INF/config.json'
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
	stage('Archive') {
            steps {
                archiveArtifacts artifacts: 'target/analytics.war', onlyIfSuccessful: true
            }
        }
    }
}
