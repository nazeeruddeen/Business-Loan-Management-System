pipeline {
    agent any

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
    }

    environment {
        APP_NAME            = 'business-loan-management-system'
        IMAGE_TAG           = "${env.BUILD_NUMBER ?: 'latest'}"
        DOCKER_REGISTRY_URL = "${env.DOCKER_REGISTRY_URL ?: ''}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                echo "Building branch: ${env.BRANCH_NAME ?: 'unknown'}, commit: ${env.GIT_COMMIT ?: 'unknown'}"
            }
        }

        stage('Build') {
            steps {
                dir('backend') {
                    sh 'mvn -B -DskipTests package'
                }
            }
        }

        stage('Test') {
            steps {
                dir('backend') {
                    sh 'mvn -B test'
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'backend/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh "docker build -t ${APP_NAME}:${IMAGE_TAG} -f backend/Dockerfile backend"
                sh "docker tag ${APP_NAME}:${IMAGE_TAG} ${APP_NAME}:latest"
            }
        }

        stage('Docker Push') {
            when {
                expression { return env.DOCKER_REGISTRY_URL?.trim() }
            }
            steps {
                withCredentials([usernamePassword(
                        credentialsId: 'docker-registry-credentials',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS')]) {
                    sh "echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin ${DOCKER_REGISTRY_URL}"
                    sh "docker tag ${APP_NAME}:${IMAGE_TAG} ${DOCKER_REGISTRY_URL}/${APP_NAME}:${IMAGE_TAG}"
                    sh "docker push ${DOCKER_REGISTRY_URL}/${APP_NAME}:${IMAGE_TAG}"
                }
            }
        }

        stage('Kubernetes Apply') {
            when {
                expression { return env.KUBECONFIG?.trim() }
            }
            steps {
                sh 'kubectl apply -f k8s/00-namespace.yaml'
                sh 'kubectl apply -f k8s/01-configmap.yaml'
                sh 'kubectl apply -f k8s/02-secret.yaml'
                sh 'kubectl apply -f k8s/04-backend.yaml'
                sh 'kubectl rollout status deployment/business-loan-backend -n business-loan --timeout=180s'
            }
        }
    }

    post {
        failure {
            echo "Pipeline failed! Check logs for details."
        }
        success {
            echo "Deploy of ${APP_NAME}:${IMAGE_TAG} succeeded."
        }
    }
}
