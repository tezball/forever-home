package com.example.foreverhome.moderation.web.admin;

import com.example.foreverhome.moderation.service.admin.AdminExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Controller
@RequestMapping("/admin/export")
public class AdminExportController {

    private final AdminExportService exportService;

    public AdminExportController(AdminExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping
    public String exportPage(Model model) {
        model.addAttribute("pageTitle", "Data Export");
        model.addAttribute("currentPage", "admin-export");
        return "admin/export";
    }

    @GetMapping("/users")
    public ResponseEntity<byte[]> exportUsers(@RequestParam(required = false) Map<String, String> filters) {
        String csv = exportService.exportUsersToCsv(filters);
        String filename = "users_" + LocalDate.now().format(DateTimeFormatter.ISO_DATE) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes());
    }

    @GetMapping("/pets")
    public ResponseEntity<byte[]> exportPets(@RequestParam(required = false) Map<String, String> filters) {
        String csv = exportService.exportPetsToCsv(filters);
        String filename = "pets_" + LocalDate.now().format(DateTimeFormatter.ISO_DATE) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes());
    }

    @GetMapping("/audit")
    public ResponseEntity<byte[]> exportAuditLog(@RequestParam(required = false) Map<String, String> filters) {
        String csv = exportService.exportAuditLogToCsv(filters);
        String filename = "audit_log_" + LocalDate.now().format(DateTimeFormatter.ISO_DATE) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes());
    }

    @GetMapping("/adoptions")
    public ResponseEntity<byte[]> exportAdoptions(@RequestParam(required = false) Map<String, String> filters) {
        String csv = exportService.exportAdoptionsToCsv(filters);
        String filename = "adoptions_" + LocalDate.now().format(DateTimeFormatter.ISO_DATE) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes());
    }
}
