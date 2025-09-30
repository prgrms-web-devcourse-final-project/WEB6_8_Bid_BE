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

# Nginx 설치
amazon-linux-extras install nginx1 -y
systemctl start nginx
systemctl enable nginx

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

# Nginx 설정
cat > /etc/nginx/conf.d/api.conf <<'EOF'
server {
    listen 80;
    server_name api.bid-market.shop;

    # 클라이언트 최대 업로드 크기 (파일 업로드용)
    client_max_body_size 25M;

    # 로그 설정
    access_log /var/log/nginx/api-access.log;
    error_log /var/log/nginx/api-error.log;

    # 정적 파일 (업로드된 파일)
    location /uploads/ {
        alias /app/uploads/;
        expires 30d;
        add_header Cache-Control "public, immutable";
    }

    # API 프록시
    location / {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        
        # 헤더 설정
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # WebSocket 지원
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        
        # 타임아웃 설정
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # WebSocket 전용 경로
    location /ws {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # WebSocket 타임아웃 (1시간)
        proxy_read_timeout 3600s;
    }
}
EOF

# Nginx 재시작
nginx -t && systemctl restart nginx

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
      - DB_HOST=${rds_endpoint}
      - DB_NAME=auction_app
      - DB_USERNAME=${db_username}
      - DB_PASSWORD=${db_password}
      - JWT_SECRET=${jwt_secret}
      - REDIS_HOST=localhost
      - REDIS_PORT=6379
      - FILE_UPLOAD_PATH=/app/uploads
    volumes:
      - /var/log/auction-app:/app/logs
      - /app/uploads:/app/uploads
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

  redis:
    image: redis:7.2.5
    container_name: auction-redis
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    command: >
      redis-server --maxmemory 256mb --maxmemory-policy allkeys-lru
    restart: unless-stopped

volumes:
  redis-data:
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
