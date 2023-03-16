pipeline {
    agent {
        label 'docker-host'
    }
    options {
        disableConcurrentBuilds()
        disableResume()
    }

    parameters {
        string name: 'ENVIRONMENT_NAME', trim: true     
        password defaultValue: '', description: 'Password to use for MySQL container - root user', name: 'MYSQL_PASSWORD'
        string name: 'MYSQL_PORT', trim: true  

        booleanParam(name: 'SKIP_STEP_1', defaultValue: false, description: 'STEP 1 - RE-CREATE DOCKER IMAGE')
    }
  
    stages {
        stage('Checkout GIT repository') {
            steps {     
              script {
                git branch: 'master',
                url: 'https://github.com/JosueRocha24/db-pipeline'
              }
            }
        }
        stage('Create latest Docker image') {
            steps {     
              script {
                if (!params.SKIP_STEP_1){    
                    echo "Creating docker image with name $params.ENVIRONMENT_NAME using port: $params.MYSQL_PORT"
                    sh """
                    sed 's/<PASSWORD>/$params.MYSQL_PASSWORD/g' include/create_developer.template > include/create_developer.sql
                    """

                    sh """
                    docker build . -t $params.ENVIRONMENT_NAME:latest
                    """

                }else{
                    echo "Skipping STEP1"
                }
              }
            }
        }
        stage('Start new container using latest image and create user') {
            steps {     
              script {
                
                def dateTime = (sh(script: "date +%Y%m%d%H%M%S", returnStdout: true).trim())
                def containerName = "${params.ENVIRONMENT_NAME}_${dateTime}"
                sh """
                docker run -itd --name ${containerName} --rm -e MYSQL_ROOT_PASSWORD=$params.MYSQL_PASSWORD -p $params.MYSQL_PORT:3306 $params.ENVIRONMENT_NAME:latest
                """

                sh """
                # A test server is temporarily spun up while running docker-entrypoint.sh and during this time the root user is only accessible with no credential, so we wait for it for the definitive startup
                docker exec ${containerName} /bin/bash -c 'until mysql --user="root" --password="$params.MYSQL_PASSWORD" -e "SELECT 1;"; do sleep 5; done; mysql --user="root" --password="$params.MYSQL_PASSWORD" < /scripts/create_developer.sql'
                """

                echo "Docker container created: $containerName"

              }
            }
        }
    }

}
