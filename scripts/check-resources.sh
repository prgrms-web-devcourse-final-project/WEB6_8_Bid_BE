#!/bin/bash

# AWS 리소스 추적 스크립트
# 사용법: ./scripts/check-resources.sh

set -e

TEAM_NAME="team12"
REGION="ap-northeast-2"

echo "=================================="
echo "Team12 AWS 리소스 현황"
echo "리전: ${REGION}"
echo "=================================="
echo ""

# VPC
echo "📡 VPC"
aws ec2 describe-vpcs \
  --filters "Name=tag:Team,Values=${TEAM_NAME}" \
  --region ${REGION} \
  --query 'Vpcs[*].[Tags[?Key==`Name`].Value|[0], VpcId, State]' \
  --output table

# EC2
echo ""
echo "💻 EC2 Instances"
aws ec2 describe-instances \
  --filters "Name=tag:Team,Values=${TEAM_NAME}" \
  --region ${REGION} \
  --query 'Reservations[*].Instances[*].[Tags[?Key==`Name`].Value|[0], InstanceId, InstanceType, State.Name, PublicIpAddress]' \
  --output table

# RDS
echo ""
echo "🗄️  RDS Instances"
aws rds describe-db-instances \
  --region ${REGION} \
  --query "DBInstances[?contains(DBInstanceIdentifier, '${TEAM_NAME}')].[DBInstanceIdentifier, DBInstanceClass, DBInstanceStatus, Endpoint.Address]" \
  --output table

# ALB
echo ""
echo "⚖️  Load Balancers"
aws elbv2 describe-load-balancers \
  --region ${REGION} \
  --query "LoadBalancers[?contains(LoadBalancerName, '${TEAM_NAME}')].[LoadBalancerName, State.Code, DNSName]" \
  --output table

# Security Groups
echo ""
echo "🔒 Security Groups"
aws ec2 describe-security-groups \
  --filters "Name=tag:Team,Values=${TEAM_NAME}" \
  --region ${REGION} \
  --query 'SecurityGroups[*].[Tags[?Key==`Name`].Value|[0], GroupId, VpcId]' \
  --output table

# ECR
echo ""
echo "🐳 ECR Repositories"
aws ecr describe-repositories \
  --region ${REGION} \
  --query "repositories[?contains(repositoryName, '${TEAM_NAME}')].[repositoryName, repositoryUri]" \
  --output table

# 비용 확인
echo ""
echo "💰 이번 달 예상 비용"
START_DATE=$(date -d "$(date +%Y-%m-01)" +%Y-%m-%d)
END_DATE=$(date +%Y-%m-%d)

aws ce get-cost-and-usage \
  --time-period Start=${START_DATE},End=${END_DATE} \
  --granularity MONTHLY \
  --metrics BlendedCost \
  --region us-east-1 \
  --query 'ResultsByTime[*].[TimePeriod.Start, Total.BlendedCost.Amount, Total.BlendedCost.Unit]' \
  --output table

echo ""
echo "=================================="
echo "✅ 리소스 확인 완료"
echo "=================================="
