#!/bin/bash
set -e

# 로그 파일 설정
exec > >(tee /var/log/user-data.log)
exec 2>&1

echo "Starting user-data script..."

# 시스템 업데이트
yum update -y

# Docker 설치
amazon-linux-extras install docker -y
systemctl start docker
systemctl enable docker
usermod -a -G docker ec2-user

# Docker Compose 설치
curl -L "https://github.com/docker/compose/releases/download/v2.20.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# AWS CLI 업데이트
pip3 install --upgrade awscli

# CloudWatch Logs Agent 설치
wget https://s3.amazonaws.com/amazoncloudwatch-agent/amazon_linux/amd64/latest/amazon-cloudwatch-agent.rpm
rpm -U ./amazon-cloudwatch-agent.rpm

# CloudWatch Logs 설정
cat > /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json <<'EOF'
{
  "logs": {
    "logs_collected": {
      "files": {
        "collect_list": [
          {
            "file_path": "/var/log/auction-app/application.log",
            "log_group_name": "/aws/ec2/auction-app-${environment}",
            "log_stream_name": "{instance_id}/application"
          }
        ]
      }
    }
  }
}
EOF

# CloudWatch Agent 시작
/opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \
  -a fetch-config \
  -m ec2 \
  -s \
  -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json

# 애플리케이션 디렉토리 생성
mkdir -p /app
mkdir -p /var/log/auction-app

# ECR 로그인 스크립트 생성
cat > /app/ecr-login.sh <<'EOF'
#!/bin/bash
aws ecr get-login-password --region ${aws_region} | docker login --username AWS --password-stdin ${ecr_repository}
EOF
chmod +x /app/ecr-login.sh

# Docker Compose 파일 생성
cat > /app/docker-compose.yml <<'EOF'
version: '3.8'

services:
  app:
    image: ${ecr_repository}:latest
    container_name: auction-app
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=${environment}
      - SPRING_DATASOURCE_URL=jdbc:mysql://RDS_ENDPOINT:3306/auction_app
      - SPRING_DATASOURCE_USERNAME=admin
      - SPRING_DATASOURCE_PASSWORD=DB_PASSWORD
    volumes:
      - /var/log/auction-app:/app/logs
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
EOF

# 배포 스크립트 생성
cat > /app/deploy.sh <<'EOF'
#!/bin/bash
set -e

echo "Starting deployment..."

# ECR 로그인
/app/ecr-login.sh

# 최신 이미지 pull
cd /app
docker-compose pull

# 애플리케이션 재시작
docker-compose down
docker-compose up -d

# 헬스 체크
echo "Waiting for application to be healthy..."
for i in {1..30}; do
  if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "Application is healthy!"
    exit 0
  fi
  echo "Waiting... ($i/30)"
  sleep 10
done

echo "Health check failed!"
exit 1
EOF
chmod +x /app/deploy.sh

# 초기 배포 (ECR에 이미지가 있을 경우)
/app/ecr-login.sh || true
cd /app
docker-compose pull || true
docker-compose up -d || true

echo "User-data script completed!"
