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

        stage('Frontend E2E') {
            steps {
                withEnv([
                        'CI=true',
                        'BUSINESS_LOAN_MYSQL_ROOT_PASSWORD=businessRoot#2026',
                        'BUSINESS_LOAN_DB_PASSWORD=businessDb#2026',
                        'BUSINESS_LOAN_JWT_SECRET=business-jwt-secret-for-ci-automation-2026',
                        'BUSINESS_LOAN_ADMIN_PASSWORD=Admin#Biz2026',
                        'BUSINESS_LOAN_OFFICER_PASSWORD=Officer#Biz2026',
                        'BUSINESS_LOAN_REVIEWER_PASSWORD=Reviewer#Biz2026',
                        'BUSINESS_LOAN_BORROWER_PASSWORD=Borrower#Biz2026',
                        'BUSINESS_LOAN_FRONTEND_HOST_PORT=4300',
                        'BUSINESS_LOAN_MYSQL_HOST_PORT=33061'
                ]) {
                    sh '''
                        set -euo pipefail
                        rm -rf frontend/playwright-report frontend/test-results
                        docker compose down -v --remove-orphans || true
                        docker compose up -d --build
                        ready=0
                        for attempt in $(seq 1 30); do
                          if curl -fsS http://127.0.0.1:8010/actuator/health/readiness >/dev/null && curl -fsS http://127.0.0.1:4300/ >/dev/null; then
                            ready=1
                            break
                          fi
                          sleep 5
                        done
                        if [ "$ready" -ne 1 ]; then
                          docker compose logs
                          exit 1
                        fi
                        docker run --rm --add-host=host.docker.internal:host-gateway \
                          -e CI=true \
                          -e PLAYWRIGHT_JUNIT_OUTPUT_NAME=test-results/e2e-results.xml \
                          -e BUSINESS_E2E_BASE_URL=http://host.docker.internal:4300 \
                          -e BUSINESS_E2E_API_BASE_URL=http://host.docker.internal:8010 \
                          -e BUSINESS_E2E_PASSWORD="$BUSINESS_LOAN_ADMIN_PASSWORD" \
                          -v "$PWD/frontend:/work" \
                          -w /work \
                          mcr.microsoft.com/playwright:v1.59.1-noble \
                          sh -lc "npm ci && npx playwright test tests/golden-path.spec.ts --reporter=line,junit,html"
                    '''
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'frontend/test-results/e2e-results.xml'
                    archiveArtifacts allowEmptyArchive: true, artifacts: 'frontend/playwright-report/**,frontend/test-results/**'
                    sh 'docker compose down -v --remove-orphans || true'
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
                sh 'kubectl apply -f k8s/07-ingress.yaml'
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
