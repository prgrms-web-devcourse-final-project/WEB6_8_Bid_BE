# WEB6_8_Bid_BE

프로그래머스 6기 8회차 최종 프로젝트 12팀(Bid) - 백엔드

# 🚀 배포 플로우

### 자동 배포 (CI/CD)
```
1. 개발자가 main 브랜치에 코드 push
   ↓
2. GitHub Actions 워크플로우 자동 실행
   ↓
3. Gradle로 프로젝트 빌드
   ↓
4. Docker 이미지 생성 및 ghcr.io에 푸시
   ↓
5. EC2 인스턴스에 SSH 접속
   ↓
6. 기존 컨테이너 중지/제거 → 새 이미지 pull → 새 컨테이너 실행
   ↓
7. 배포 완료
```

### 인프라 구조
```
AWS EC2 (인스턴스)
├─ Nginx (Docker)
├─ MySQL (Docker)
├─ Redis (Docker)
├─ Elasticsearch (Docker)
└─ Spring Boot App (Docker)
```

### 인프라 세부사항
- **배포 환경**: AWS EC2 1대 (ap-northeast-2b)
- **컨테이너화**: Docker 기반
- **이미지 저장소**: GitHub Container Registry (ghcr.io)
- **배포 자동화**: GitHub Actions
- **배포 트리거**: main 브랜치 push
