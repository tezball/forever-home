package com.example.foreverhome.moderation.web.admin;

import com.example.foreverhome.moderation.domain.admin.AccountStatus;
import com.example.foreverhome.moderation.domain.admin.UserRole;
import com.example.foreverhome.moderation.service.admin.AdminUserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/admin/users")
public class AdminUsersController {

    private final AdminUserService userService;

    public AdminUsersController(AdminUserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String listUsers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {

        UserRole userRole = role != null && !role.isBlank() ? UserRole.valueOf(role) : null;
        AccountStatus accountStatus = status != null && !status.isBlank() ? AccountStatus.valueOf(status) : null;

        var result = userService.searchUsers(query, userRole, accountStatus, page, size);

        model.addAttribute("pageTitle", "Users");
        model.addAttribute("currentPage", "admin-users");
        model.addAttribute("users", result.users());
        model.addAttribute("page", result.page());
        model.addAttribute("size", result.size());
        model.addAttribute("totalCount", result.totalCount());
        model.addAttribute("totalPages", result.totalPages());
        model.addAttribute("query", query);
        model.addAttribute("role", role);
        model.addAttribute("status", status);
        model.addAttribute("roles", UserRole.values());
        model.addAttribute("statuses", AccountStatus.values());

        return "admin/users";
    }

    @GetMapping("/{id}")
    public String userDetails(@PathVariable UUID id, Model model) {
        var user = userService.getUser(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        model.addAttribute("pageTitle", "User Details");
        model.addAttribute("currentPage", "admin-users");
        model.addAttribute("user", user);

        return "admin/user-details";
    }

    @PostMapping("/{id}/suspend")
    public String suspendUser(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            userService.suspendUser(id);
            redirectAttributes.addFlashAttribute("success", "User suspended successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to suspend user: " + e.getMessage());
        }
        return "redirect:/admin/users/" + id;
    }

    @PostMapping("/{id}/reactivate")
    public String reactivateUser(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            userService.reactivateUser(id);
            redirectAttributes.addFlashAttribute("success", "User reactivated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to reactivate user: " + e.getMessage());
        }
        return "redirect:/admin/users/" + id;
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            String tempPassword = userService.resetPassword(id);
            redirectAttributes.addFlashAttribute("success", "Password reset successfully. Temporary password: " + tempPassword);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to reset password: " + e.getMessage());
        }
        return "redirect:/admin/users/" + id;
    }

    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("success", "User deleted successfully");
            return "redirect:/admin/users";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete user: " + e.getMessage());
            return "redirect:/admin/users/" + id;
        }
    }
}
