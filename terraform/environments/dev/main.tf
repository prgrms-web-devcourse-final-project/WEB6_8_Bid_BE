terraform {
  required_version = ">= 1.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
  
  backend "s3" {
    bucket         = "team12-terraform-state"
    key            = "dev/terraform.tfstate"
    region         = "ap-northeast-2"
    encrypt        = true
  }
}

provider "aws" {
  region = var.aws_region
  
  default_tags {
    tags = merge(local.common_tags, {
      Team = "team12"
    })
  }
}

# AMI 데이터 소스
data "aws_ami" "amazon_linux" {
  most_recent = true
  owners      = ["amazon"]
  
  filter {
    name   = "name"
    values = ["amzn2-ami-hvm-*-x86_64-gp2"]
  }
  
  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

# VPC
resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true
  
  tags = {
    Name = local.vpc_name
  }
}

# Internet Gateway
resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id
  
  tags = {
    Name = local.igw_name
  }
}

# Public Subnets
resource "aws_subnet" "public" {
  count = length(var.availability_zones)
  
  vpc_id                  = aws_vpc.main.id
  cidr_block              = var.public_subnet_cidrs[count.index]
  availability_zone       = var.availability_zones[count.index]
  map_public_ip_on_launch = true
  
  tags = {
    Name = "${local.public_subnet_prefix}-${count.index + 1}"
  }
}

# Private Subnets
resource "aws_subnet" "private" {
  count = length(var.availability_zones)
  
  vpc_id            = aws_vpc.main.id
  cidr_block        = var.private_subnet_cidrs[count.index]
  availability_zone = var.availability_zones[count.index]
  
  tags = {
    Name = "${local.private_subnet_prefix}-${count.index + 1}"
  }
}

# Route Table for Public Subnets
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id
  
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }
  
  tags = {
    Name = "${local.team_prefix}-public-rt-1"
  }
}

# Route Table Association
resource "aws_route_table_association" "public" {
  count = length(aws_subnet.public)
  
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

# Security Group for EC2
resource "aws_security_group" "ec2" {
  name        = local.ec2_sg_name
  description = "Security group for EC2 instances - Team12"
  vpc_id      = aws_vpc.main.id
  
  ingress {
    description = "HTTP from anywhere"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  
  ingress {
    description = "HTTPS from anywhere"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  
  ingress {
    description = "Application port"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  
  ingress {
    description = "SSH from anywhere"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  
  egress {
    description = "Allow all outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
  
  tags = {
    Name = local.ec2_sg_name
  }
}

# Security Group for RDS
resource "aws_security_group" "rds" {
  name        = local.rds_sg_name
  description = "Security group for RDS MySQL - Team12"
  vpc_id      = aws_vpc.main.id
  
  ingress {
    description     = "MySQL from EC2"
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [aws_security_group.ec2.id]
  }
  
  egress {
    description = "Allow all outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
  
  tags = {
    Name = local.rds_sg_name
  }
}

# Elastic IP for EC2
resource "aws_eip" "main" {
  domain = "vpc"
  
  tags = {
    Name = "${local.team_prefix}-eip-1"
  }
}

# IAM Role for EC2
resource "aws_iam_role" "ec2" {
  name = "${local.team_prefix}-ec2-role-1"
  
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })
  
  tags = {
    Name = "${local.team_prefix}-ec2-role-1"
  }
}

resource "aws_iam_role_policy_attachment" "ec2_ecr" {
  role       = aws_iam_role.ec2.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}

resource "aws_iam_role_policy_attachment" "ec2_ssm" {
  role       = aws_iam_role.ec2.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_role_policy_attachment" "ec2_cloudwatch" {
  role       = aws_iam_role.ec2.name
  policy_arn = "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"
}

resource "aws_iam_instance_profile" "ec2" {
  name = "${local.team_prefix}-ec2-profile-1"
  role = aws_iam_role.ec2.name
  
  tags = {
    Name = "${local.team_prefix}-ec2-profile-1"
  }
}

# Launch Template
resource "aws_launch_template" "main" {
  name_prefix   = "${local.launch_template_name}-"
  image_id      = data.aws_ami.amazon_linux.id
  instance_type = var.instance_type
  key_name      = var.key_pair_name
  
  iam_instance_profile {
    name = aws_iam_instance_profile.ec2.name
  }
  
  network_interfaces {
    associate_public_ip_address = true
    security_groups             = [aws_security_group.ec2.id]
    delete_on_termination       = true
  }
  
  user_data = base64encode(templatefile("${path.module}/user-data.sh", {
    environment    = var.environment
    ecr_repository = aws_ecr_repository.main.repository_url
    aws_region     = var.aws_region
    rds_endpoint   = aws_db_instance.main.endpoint
    db_username    = var.db_username
    db_password    = var.db_password
    jwt_secret     = var.jwt_secret
  }))
  
  tag_specifications {
    resource_type = "instance"
    tags = merge(local.common_tags, {
      Name = "${local.team_prefix}-instance-1"
    })
  }
  
  tag_specifications {
    resource_type = "volume"
    tags = merge(local.common_tags, {
      Name = "${local.team_prefix}-volume-1"
    })
  }
}

# Auto Scaling Group
resource "aws_autoscaling_group" "main" {
  name                = local.asg_name
  vpc_zone_identifier = aws_subnet.public[*].id
  health_check_type   = "EC2"  # ALB 없으므로 EC2 헬스체크
  health_check_grace_period = 300
  
  min_size         = var.asg_min_size
  max_size         = var.asg_max_size
  desired_capacity = var.asg_desired_capacity
  
  launch_template {
    id      = aws_launch_template.main.id
    version = "$Latest"
  }
  
  tag {
    key                 = "Name"
    value               = "${local.team_prefix}-instance-1"
    propagate_at_launch = true
  }
  
  tag {
    key                 = "Team"
    value               = "team12"
    propagate_at_launch = true
  }
  
  tag {
    key                 = "Environment"
    value               = var.environment
    propagate_at_launch = true
  }
}

# RDS Subnet Group
resource "aws_db_subnet_group" "main" {
  name       = "${local.team_prefix}-db-subnet-1"
  subnet_ids = aws_subnet.private[*].id
  
  tags = {
    Name = "${local.team_prefix}-db-subnet-1"
  }
}

# RDS Instance
resource "aws_db_instance" "main" {
  identifier     = local.rds_name
  engine         = "mysql"
  engine_version = "8.0"
  instance_class = var.db_instance_class
  
  allocated_storage     = 20
  storage_type          = "gp3"
  storage_encrypted     = true
  
  db_name  = var.db_name
  username = var.db_username
  password = var.db_password
  
  vpc_security_group_ids = [aws_security_group.rds.id]
  db_subnet_group_name   = aws_db_subnet_group.main.name
  
  backup_retention_period = 7
  backup_window          = "03:00-04:00"
  maintenance_window     = "mon:04:00-mon:05:00"
  
  skip_final_snapshot       = var.environment == "dev" ? true : false
  final_snapshot_identifier = var.environment == "dev" ? null : "${local.rds_name}-final-snapshot"
  
  publicly_accessible = false
  
  tags = {
    Name = local.rds_name
  }
}

# ECR Repository
resource "aws_ecr_repository" "main" {
  name                 = local.ecr_name
  image_tag_mutability = "MUTABLE"
  
  image_scanning_configuration {
    scan_on_push = true
  }
  
  tags = {
    Name = local.ecr_name
  }
}

# ECR Lifecycle Policy
resource "aws_ecr_lifecycle_policy" "main" {
  repository = aws_ecr_repository.main.name
  
  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Keep last 10 images"
        selection = {
          tagStatus   = "any"
          countType   = "imageCountMoreThan"
          countNumber = 10
        }
        action = {
          type = "expire"
        }
      }
    ]
  })
}

# CloudWatch Log Group
resource "aws_cloudwatch_log_group" "app" {
  name              = "/aws/ec2/${local.team_prefix}-app"
  retention_in_days = var.environment == "prod" ? 30 : 7
  
  tags = {
    Name = "${local.team_prefix}-logs-1"
  }
}
