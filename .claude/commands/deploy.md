# Deploy to AWS

Deploy the application to AWS ECS Fargate.

## Instructions

Run the deploy script to build and deploy the application:

```bash
./deploy.sh
```

This will:
1. Validate AWS credentials and prerequisites
2. Login to ECR (Elastic Container Registry)
3. Build the Docker image
4. Push the image to ECR
5. Force ECS to restart with the new image

## Optional: Deploy with a specific tag

To deploy with a custom image tag instead of `latest`:

```bash
./deploy.sh --tag v1.0.0
```

## After deployment

Monitor the deployment progress with:
```bash
aws ecs describe-services --cluster <cluster-name> --services <service-name> --query 'services[0].deployments'
```

View application logs with:
```bash
aws logs tail /ecs/<cluster-name> --follow
```
