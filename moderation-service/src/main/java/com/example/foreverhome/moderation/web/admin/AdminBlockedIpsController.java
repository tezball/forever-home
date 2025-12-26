package com.example.foreverhome.moderation.web.admin;

import com.example.foreverhome.moderation.service.admin.AdminBlockedIpService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/admin/blocked-ips")
public class AdminBlockedIpsController {

    private final AdminBlockedIpService blockedIpService;

    public AdminBlockedIpsController(AdminBlockedIpService blockedIpService) {
        this.blockedIpService = blockedIpService;
    }

    @GetMapping
    public String blockedIps(@RequestParam(defaultValue = "1") int page, Model model) {
        var dashboard = blockedIpService.getDashboard();
        var blockedIps = blockedIpService.getAllBlocked(page, 50);

        model.addAttribute("pageTitle", "Blocked IPs");
        model.addAttribute("currentPage", "admin-blocked-ips");
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("blockedIps", blockedIps);
        model.addAttribute("page", page);

        return "admin/blocked-ips";
    }

    @PostMapping("/block")
    public String blockIp(@RequestParam String ipAddress,
                          @RequestParam String reason,
                          @RequestParam(defaultValue = "false") boolean permanent,
                          @RequestParam(required = false) Integer durationMinutes,
                          RedirectAttributes redirectAttributes) {
        try {
            // TODO: Get actual admin user ID from security context
            UUID adminId = null;

            if (permanent) {
                blockedIpService.blockIp(ipAddress, reason, adminId, true);
            } else {
                int duration = durationMinutes != null ? durationMinutes : 60;
                blockedIpService.blockIpTemporary(ipAddress, reason, adminId, duration);
            }
            redirectAttributes.addFlashAttribute("success", "IP " + ipAddress + " blocked successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to block IP: " + e.getMessage());
        }
        return "redirect:/admin/blocked-ips";
    }

    @PostMapping("/block-cidr")
    public String blockCidr(@RequestParam String cidrRange,
                            @RequestParam String reason,
                            @RequestParam(defaultValue = "false") boolean permanent,
                            RedirectAttributes redirectAttributes) {
        try {
            UUID adminId = null; // TODO: Get from security context
            blockedIpService.blockCidr(cidrRange, reason, adminId, permanent);
            redirectAttributes.addFlashAttribute("success", "CIDR range " + cidrRange + " blocked successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to block CIDR: " + e.getMessage());
        }
        return "redirect:/admin/blocked-ips";
    }

    @PostMapping("/{id}/unblock")
    public String unblock(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            UUID adminId = null; // TODO: Get from security context
            blockedIpService.unblock(id, adminId);
            redirectAttributes.addFlashAttribute("success", "IP unblocked successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to unblock: " + e.getMessage());
        }
        return "redirect:/admin/blocked-ips";
    }

    @PostMapping("/cleanup")
    public String cleanupExpired(RedirectAttributes redirectAttributes) {
        try {
            int count = blockedIpService.cleanupExpired();
            redirectAttributes.addFlashAttribute("success", count + " expired blocks cleaned up");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to cleanup: " + e.getMessage());
        }
        return "redirect:/admin/blocked-ips";
    }

    @GetMapping("/check")
    @ResponseBody
    public boolean checkIp(@RequestParam String ip) {
        return blockedIpService.isIpBlocked(ip);
    }
}
