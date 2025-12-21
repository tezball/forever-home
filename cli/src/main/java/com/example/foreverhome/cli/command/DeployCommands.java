package com.example.foreverhome.cli.command;

import com.example.foreverhome.cli.service.ConfigManager;
import com.example.foreverhome.cli.service.ProcessExecutor;
import com.example.foreverhome.cli.ui.InteractiveMenu;
import com.example.foreverhome.cli.ui.ProgressReporter;
import com.example.foreverhome.cli.ui.TerminalOutput;
import org.springframework.context.annotation.Lazy;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.stereotype.Component;

/**
 * AWS deployment commands.
 * Ports functionality from deploy.sh for AWS ECS deployment.
 */
@Component
@Command(command = "deploy", group = "Deployment", description = "AWS deployment commands")
public class DeployCommands {

    private final ProcessExecutor executor;
    private final ConfigManager config;
    private final InteractiveMenu menu;
    private final TerminalOutput output;
    private final ProgressReporter progress;

    public DeployCommands(ProcessExecutor executor, ConfigManager config,
                          @Lazy InteractiveMenu menu, TerminalOutput output,
                          ProgressReporter progress) {
        this.executor = executor;
        this.config = config;
        this.menu = menu;
        this.output = output;
        this.progress = progress;
    }

    @Command(command = "aws", description = "Full AWS deployment (Terraform + Docker + ECS)")
    public void aws(
            @Option(longNames = "auto-approve", shortNames = 'y',
                    description = "Auto-approve Terraform changes")
            boolean autoApprove,
            @Option(longNames = "tag", shortNames = 't',
                    defaultValue = "latest",
                    description = "Docker image tag")
            String tag,
            @Option(longNames = "skip-infra",
                    description = "Skip Terraform infrastructure deployment")
            boolean skipInfra,
            @Option(longNames = "skip-build",
                    description = "Skip Docker image build")
            boolean skipBuild) {

        output.header("Forever Home - AWS Deployment");
        output.println();

        // Check prerequisites
        checkPrerequisites();

        // Terraform infrastructure
        if (!skipInfra) {
            deployInfrastructure(autoApprove);
        } else {
            output.info("Skipping infrastructure deployment");
        }

        // Get Terraform outputs
        String ecrUrl = getTerraformOutput("ecr_repository_url");
        String ecsCluster = getTerraformOutput("ecs_cluster_name");
        String ecsService = getTerraformOutput("ecs_service_name");
        String albDns = getTerraformOutput("alb_dns_name");

        if (ecrUrl.isBlank() || ecsCluster.isBlank()) {
            output.error("Could not get Terraform outputs");
            output.info("Run 'fh deploy aws' without --skip-infra first");
            return;
        }

        // Build Docker image
        if (!skipBuild) {
            build(tag);
        } else {
            output.info("Skipping Docker build");
        }

        // Push to ECR (skip auto-deploy since we trigger manually below)
        push(ecrUrl, tag, true);

        // Trigger ECS deployment
        triggerDeployment(ecsCluster, ecsService);

        // Show result
        output.println();
        output.header("Deployment Complete!");
        output.println();
        output.println("  ALB DNS: " + output.cyan(albDns));
        output.println("  URL:     " + output.cyan("http://" + albDns));
        output.println();
        output.info("Note: DNS propagation may take a few minutes");
    }

    @Command(command = "build", description = "Build Docker image")
    public void build(
            @Option(longNames = "tag", shortNames = 't',
                    defaultValue = "latest",
                    description = "Docker image tag")
            String tag) {

        progress.start("Building Docker image");

        int result = executor.run("docker", "build", "-t",
            "forever-home:" + tag, ".");

        if (result != 0) {
            progress.fail("Docker build failed");
            return;
        }

        progress.complete();
        output.success("Image built: forever-home:%s", tag);
    }

    @Command(command = "push", description = "Push image to ECR and trigger ECS deployment")
    public void push(
            @Option(longNames = "ecr-url",
                    description = "ECR repository URL (auto-detected from Terraform)")
            String ecrUrl,
            @Option(longNames = "tag", shortNames = 't',
                    defaultValue = "latest",
                    description = "Docker image tag")
            String tag,
            @Option(longNames = "no-deploy",
                    description = "Skip triggering ECS deployment after push")
            boolean noDeploy) {

        // Auto-detect ECR URL if not provided
        if (ecrUrl == null || ecrUrl.isBlank()) {
            ecrUrl = getTerraformOutput("ecr_repository_url");
            if (ecrUrl.isBlank()) {
                output.error("ECR URL not found. Deploy infrastructure first or provide --ecr-url");
                return;
            }
        }

        String region = extractRegion(ecrUrl);
        String registry = ecrUrl.split("/")[0];

        // ECR login
        progress.start("Logging into ECR");

        String password = executor.capture("aws", "ecr", "get-login-password",
            "--region", region);

        if (password.isBlank()) {
            progress.fail("Failed to get ECR login");
            return;
        }

        int loginResult = executor.runWithInput(password.trim(),
            "docker", "login", "--username", "AWS", "--password-stdin", registry);

        if (loginResult != 0) {
            progress.fail("ECR login failed");
            return;
        }
        progress.complete();

        // Tag image
        String fullTag = ecrUrl + ":" + tag;
        progress.start("Tagging image");
        int tagResult = executor.runQuiet("docker", "tag", "forever-home:" + tag, fullTag);
        if (tagResult != 0) {
            progress.fail("Failed to tag image");
            return;
        }
        progress.complete();

        // Push to ECR
        progress.start("Pushing to ECR");
        int pushResult = executor.run("docker", "push", fullTag);
        if (pushResult != 0) {
            progress.fail("Failed to push image");
            return;
        }
        progress.complete();

        output.success("Pushed: %s", fullTag);

        // Trigger ECS deployment
        if (!noDeploy) {
            String ecsCluster = getTerraformOutput("ecs_cluster_name");
            String ecsService = getTerraformOutput("ecs_service_name");

            if (!ecsCluster.isBlank() && !ecsService.isBlank()) {
                triggerDeployment(ecsCluster, ecsService);
            } else {
                output.warning("Could not find ECS cluster/service. Skipping deployment trigger.");
            }
        }
    }

    @Command(command = "status", description = "Check deployment status")
    public void status() {
        output.header("AWS Deployment Status");
        output.println();

        // Check Terraform state
        boolean hasTfState = executor.fileExists("terraform/terraform.tfstate");
        output.println("  Terraform state: " +
            (hasTfState ? output.green("Found") : output.red("Not found")));

        if (!hasTfState) {
            output.println();
            output.info("Run 'fh deploy aws' to deploy infrastructure");
            return;
        }

        // Get Terraform outputs
        String albDns = getTerraformOutput("alb_dns_name");
        String ecsCluster = getTerraformOutput("ecs_cluster_name");
        String ecsService = getTerraformOutput("ecs_service_name");
        String dbEndpoint = getTerraformOutput("db_endpoint");
        String region = getAwsRegion();

        output.println("  Region:      " + region);
        output.println("  ALB DNS:     " + (albDns.isBlank() ? output.dim("N/A") : albDns));
        output.println("  ECS Cluster: " + (ecsCluster.isBlank() ? output.dim("N/A") : ecsCluster));
        output.println("  ECS Service: " + (ecsService.isBlank() ? output.dim("N/A") : ecsService));
        output.println("  DB Endpoint: " + (dbEndpoint.isBlank() ? output.dim("N/A") : dbEndpoint));

        // Check ECS service status
        if (!ecsCluster.isBlank() && !ecsService.isBlank()) {
            output.println();
            checkEcsServiceStatus(ecsCluster, ecsService, region);
        }

        if (!albDns.isBlank()) {
            output.println();
            output.println("  Application URL: " + output.cyan("http://" + albDns));
        }
    }

    @Command(command = "logs", description = "View ECS service logs")
    public void logs(
            @Option(longNames = "follow", shortNames = 'f',
                    description = "Follow log output")
            boolean follow,
            @Option(longNames = "tail", shortNames = 'n',
                    defaultValue = "100",
                    description = "Number of lines to show")
            int tail) {

        String logGroup = getTerraformOutput("log_group_name");
        String region = getAwsRegion();

        if (logGroup.isBlank()) {
            output.error("Log group not found. Is infrastructure deployed?");
            return;
        }

        output.info("Fetching logs from: %s", logGroup);
        output.println();

        if (follow) {
            executor.run("aws", "logs", "tail", logGroup,
                "--region", region, "--follow");
        } else {
            executor.run("aws", "logs", "tail", logGroup,
                "--region", region, "--since", "1h");
        }
    }

    @Command(command = "destroy", description = "Destroy AWS infrastructure")
    public void destroy(
            @Option(longNames = "confirm",
                    description = "Confirm destruction (required)")
            boolean confirm) {

        if (!confirm) {
            output.warning("This will DESTROY all AWS infrastructure!");
            output.println();
            output.error("Database and all data will be permanently deleted.");
            output.println();
            output.info("To proceed, run: fh deploy destroy --confirm");
            return;
        }

        if (!menu.confirm("Are you absolutely sure you want to destroy all infrastructure?")) {
            output.info("Aborted");
            return;
        }

        output.header("Destroying AWS Infrastructure");
        output.println();

        progress.start("Running terraform destroy");
        int result = executor.runInDirectory("terraform",
            "terraform", "destroy", "-auto-approve");

        if (result != 0) {
            progress.fail("Terraform destroy failed");
            return;
        }
        progress.complete();

        output.success("Infrastructure destroyed");
    }

    private void checkPrerequisites() {
        progress.start("Checking prerequisites");

        try {
            executor.requireCommand("aws");
            executor.requireCommand("docker");
            executor.requireCommand("terraform");

            // Check AWS credentials
            int awsResult = executor.runQuiet("aws", "sts", "get-caller-identity");
            if (awsResult != 0) {
                throw new RuntimeException("AWS credentials not configured");
            }

            progress.complete();

            // Show AWS account info
            String accountId = executor.capture("aws", "sts", "get-caller-identity",
                "--query", "Account", "--output", "text").trim();
            String region = executor.capture("aws", "configure", "get", "region").trim();
            if (region.isBlank()) region = "us-east-1";

            output.info("AWS Account: %s, Region: %s", accountId, region);
        } catch (RuntimeException e) {
            progress.fail(e.getMessage());
            throw e;
        }
    }

    private void deployInfrastructure(boolean autoApprove) {
        // Initialize Terraform
        progress.start("Initializing Terraform");
        int initResult = executor.runInDirectory("terraform",
            "terraform", "init", "-input=false");
        if (initResult != 0) {
            progress.fail("Terraform init failed");
            throw new RuntimeException("Terraform init failed");
        }
        progress.complete();

        // Plan
        progress.start("Planning infrastructure changes");
        int planResult = executor.runInDirectory("terraform",
            "terraform", "plan", "-out=tfplan", "-input=false");
        if (planResult != 0) {
            progress.fail("Terraform plan failed");
            throw new RuntimeException("Terraform plan failed");
        }
        progress.complete();

        // Apply
        progress.start("Applying infrastructure changes");
        int applyResult;
        if (autoApprove) {
            applyResult = executor.runInDirectory("terraform",
                "terraform", "apply", "-input=false", "tfplan");
        } else {
            output.println();
            applyResult = executor.runInDirectory("terraform",
                "terraform", "apply", "tfplan");
        }
        if (applyResult != 0) {
            progress.fail("Terraform apply failed");
            throw new RuntimeException("Terraform apply failed");
        }
        progress.complete();

        // Clean up plan file
        executor.runInDirectory("terraform", "rm", "-f", "tfplan");
    }

    private void triggerDeployment(String cluster, String service) {
        String region = getAwsRegion();

        progress.start("Triggering ECS deployment");
        int result = executor.runQuiet("aws", "ecs", "update-service",
            "--cluster", cluster,
            "--service", service,
            "--force-new-deployment",
            "--region", region);

        if (result != 0) {
            progress.fail("Failed to trigger deployment");
            return;
        }
        progress.complete();

        progress.start("Waiting for service to stabilize");
        int waitResult = executor.runQuiet("aws", "ecs", "wait", "services-stable",
            "--cluster", cluster,
            "--services", service,
            "--region", region);

        if (waitResult == 0) {
            progress.complete();
        } else {
            progress.skip("Service may still be deploying");
        }
    }

    private String getAwsRegion() {
        // Try to extract from ECR URL first
        String ecrUrl = getTerraformOutput("ecr_repository_url");
        if (!ecrUrl.isBlank()) {
            String region = extractRegion(ecrUrl);
            if (!region.isBlank()) return region;
        }
        // Fallback to AWS CLI configured region
        String region = executor.capture("aws", "configure", "get", "region").trim();
        return region.isBlank() ? "us-east-1" : region;
    }

    private void checkEcsServiceStatus(String cluster, String service, String region) {
        String status = executor.capture("aws", "ecs", "describe-services",
            "--cluster", cluster,
            "--services", service,
            "--region", region,
            "--query", "services[0].status",
            "--output", "text").trim();

        String runningCount = executor.capture("aws", "ecs", "describe-services",
            "--cluster", cluster,
            "--services", service,
            "--region", region,
            "--query", "services[0].runningCount",
            "--output", "text").trim();

        String desiredCount = executor.capture("aws", "ecs", "describe-services",
            "--cluster", cluster,
            "--services", service,
            "--region", region,
            "--query", "services[0].desiredCount",
            "--output", "text").trim();

        String statusColor = "ACTIVE".equals(status) ? output.green(status) : output.yellow(status);
        output.println("  ECS Status:  " + statusColor);
        output.println("  Tasks:       " + runningCount + "/" + desiredCount + " running");
    }

    private String getTerraformOutput(String name) {
        return executor.captureInDirectory("terraform",
            "terraform", "output", "-raw", name).trim();
    }

    private String extractRegion(String ecrUrl) {
        // Format: account.dkr.ecr.region.amazonaws.com/repo
        String[] parts = ecrUrl.split("\\.");
        if (parts.length >= 4) {
            return parts[3];
        }
        return config.getString("aws.region", "us-east-1");
    }
}
