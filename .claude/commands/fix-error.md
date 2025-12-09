# Fix Error Log

Review and fix the error(s) in the `error.log` file.

## Instructions

1. **Read the error log**: Read the contents of `error.log` in the project root to understand the error(s).

2. **Reproduce the issue**: Use the appropriate MCP tool to reproduce and confirm the error exists:
   - For Terraform errors: Run `mcp__docker__run_command` with `terraform plan` or `terraform validate` to see the error
   - For application errors: Run the relevant command to trigger the error
   - Document the exact error output

3. **Analyze and fix**:
   - Identify the file and line number from the error
   - Read the problematic file to understand the context
   - Research the correct syntax/approach if needed (use context7 for documentation)
   - Apply the fix

4. **Confirm the fix**: Run the same MCP command from step 2 to verify the error is resolved:
   - The command should now succeed or show no errors related to the original issue
   - If new errors appear, continue fixing until the original error type is resolved

5. **Report**: Summarize what was wrong and how it was fixed.
