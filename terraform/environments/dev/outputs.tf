output "vpc_id" {
  description = "VPC ID"
  value       = aws_vpc.main.id
}

output "public_subnet_ids" {
  description = "Public subnet IDs"
  value       = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  description = "Private subnet IDs"
  value       = aws_subnet.private[*].id
}

output "elastic_ip" {
  description = "Elastic IP for EC2 (API endpoint)"
  value       = aws_eip.main.public_ip
}

output "api_endpoint" {
  description = "API endpoint URL (http://ELASTIC_IP:8080)"
  value       = "http://${aws_eip.main.public_ip}:8080"
}

output "rds_endpoint" {
  description = "RDS endpoint"
  value       = aws_db_instance.main.endpoint
  sensitive   = true
}

output "rds_address" {
  description = "RDS address"
  value       = aws_db_instance.main.address
  sensitive   = true
}

output "ecr_repository_url" {
  description = "ECR repository URL"
  value       = aws_ecr_repository.main.repository_url
}

output "ecr_repository_arn" {
  description = "ECR repository ARN"
  value       = aws_ecr_repository.main.arn
}

output "asg_name" {
  description = "Auto Scaling Group name"
  value       = aws_autoscaling_group.main.name
}

output "ssh_command" {
  description = "SSH command to connect to EC2"
  value       = "ssh -i ${var.key_pair_name}.pem ec2-user@${aws_eip.main.public_ip}"
}
