# 로컬 변수 정의 - 리소스 네이밍을 일관되게 관리
locals {
  team_prefix = "team12"
  env         = var.environment
  
  # 네이밍 규칙: team12-{resource-type}-{number}
  vpc_name              = "${local.team_prefix}-vpc-1"
  igw_name              = "${local.team_prefix}-igw-1"
  public_subnet_prefix  = "${local.team_prefix}-public-subnet"
  private_subnet_prefix = "${local.team_prefix}-private-subnet"
  alb_sg_name          = "${local.team_prefix}-alb-sg-1"
  ec2_sg_name          = "${local.team_prefix}-ec2-sg-1"
  rds_sg_name          = "${local.team_prefix}-rds-sg-1"
  alb_name             = "${local.team_prefix}-alb-1"
  target_group_name    = "${local.team_prefix}-tg-1"
  launch_template_name = "${local.team_prefix}-lt-1"
  asg_name             = "${local.team_prefix}-asg-1"
  rds_name             = "${local.team_prefix}-rds-1"
  ecr_name             = "${local.team_prefix}-ecr-1"
  
  # 공통 태그
  common_tags = {
    Team        = "team12"
    Environment = var.environment
    Project     = "auction-app"
    ManagedBy   = "terraform"
  }
}
