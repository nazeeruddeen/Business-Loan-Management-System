pipeline {
    agent any

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
    }

    environment {
        BACKEND_IMAGE       = 'business-loan-management-system'
        FRONTEND_IMAGE      = 'business-loan-management-system-frontend'
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

        stage('Package') {
            steps {
                dir('backend') {
                    sh 'mvn -B -DskipTests package'
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh "docker build -t ${BACKEND_IMAGE}:${IMAGE_TAG} -f backend/Dockerfile backend"
                sh "docker tag ${BACKEND_IMAGE}:${IMAGE_TAG} ${BACKEND_IMAGE}:latest"
                sh "docker build -t ${FRONTEND_IMAGE}:${IMAGE_TAG} -f frontend/Dockerfile frontend"
                sh "docker tag ${FRONTEND_IMAGE}:${IMAGE_TAG} ${FRONTEND_IMAGE}:latest"
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
                    sh "docker tag ${BACKEND_IMAGE}:${IMAGE_TAG} ${DOCKER_REGISTRY_URL}/${BACKEND_IMAGE}:${IMAGE_TAG}"
                    sh "docker push ${DOCKER_REGISTRY_URL}/${BACKEND_IMAGE}:${IMAGE_TAG}"
                    sh "docker tag ${BACKEND_IMAGE}:latest ${DOCKER_REGISTRY_URL}/${BACKEND_IMAGE}:latest"
                    sh "docker push ${DOCKER_REGISTRY_URL}/${BACKEND_IMAGE}:latest"
                    sh "docker tag ${FRONTEND_IMAGE}:${IMAGE_TAG} ${DOCKER_REGISTRY_URL}/${FRONTEND_IMAGE}:${IMAGE_TAG}"
                    sh "docker push ${DOCKER_REGISTRY_URL}/${FRONTEND_IMAGE}:${IMAGE_TAG}"
                    sh "docker tag ${FRONTEND_IMAGE}:latest ${DOCKER_REGISTRY_URL}/${FRONTEND_IMAGE}:latest"
                    sh "docker push ${DOCKER_REGISTRY_URL}/${FRONTEND_IMAGE}:latest"
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
                sh 'kubectl apply -f k8s/03-mysql.yaml'
                sh 'kubectl apply -f k8s/04-backend.yaml'
                sh 'kubectl apply -f k8s/05-redis.yaml'
                sh 'kubectl apply -f k8s/06-frontend.yaml'
                sh 'kubectl rollout status deployment/business-loan-backend -n business-loan --timeout=180s'
                sh 'kubectl rollout status deployment/business-loan-frontend -n business-loan --timeout=180s'
            }
        }
    }

    post {
        failure {
            echo "Pipeline failed! Check logs for details."
        }
        success {
            echo "Deploy of ${BACKEND_IMAGE}:${IMAGE_TAG} and ${FRONTEND_IMAGE}:${IMAGE_TAG} succeeded."
        }
    }
}
