package com.example.foreverhome.controller;

import com.example.foreverhome.domain.profile.*;
import com.example.foreverhome.domain.user.NotificationPreferences;
import com.example.foreverhome.exception.ResourceNotFoundException;
import com.example.foreverhome.repository.*;
import com.example.foreverhome.security.UserPrincipal;
import com.example.foreverhome.service.S3StorageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Controller for managing user profiles across all roles.
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final FosterRepository fosterRepository;
    private final AdopterRepository adopterRepository;
    private final VetRepository vetRepository;
    private final RescueOrganizationRepository rescueOrganizationRepository;
    private final UserRepository userRepository;
    private final S3StorageService storageService;

    public ProfileController(FosterRepository fosterRepository, AdopterRepository adopterRepository,
                             VetRepository vetRepository, RescueOrganizationRepository rescueOrganizationRepository,
                             UserRepository userRepository, S3StorageService storageService) {
        this.fosterRepository = fosterRepository;
        this.adopterRepository = adopterRepository;
        this.vetRepository = vetRepository;
        this.rescueOrganizationRepository = rescueOrganizationRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    // ==================== PROFILE STATUS ====================

    @GetMapping("/status")
    public ResponseEntity<ProfileStatusResponse> getProfileStatus(@AuthenticationPrincipal UserPrincipal principal) {
        var user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.userId()));

        boolean isComplete = user.isProfileComplete();
        String role = principal.role().name();

        return ResponseEntity.ok(new ProfileStatusResponse(isComplete, role));
    }

    // ==================== FOSTER PROFILE ====================

    @GetMapping("/foster")
    @PreAuthorize("hasRole('FOSTER')")
    public ResponseEntity<FosterProfileResponse> getFosterProfile(@AuthenticationPrincipal UserPrincipal principal) {
        Foster foster = fosterRepository.findByUserId(principal.userId())
                .orElse(null);

        if (foster == null) {
            return ResponseEntity.ok(new FosterProfileResponse(null, null, null, null, false));
        }

        return ResponseEntity.ok(FosterProfileResponse.from(foster));
    }

    @PostMapping("/foster")
    @PreAuthorize("hasRole('FOSTER')")
    public ResponseEntity<FosterProfileResponse> createOrUpdateFosterProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody FosterProfileRequest request) {

        Foster foster = fosterRepository.findByUserId(principal.userId())
                .orElse(null);

        Address address = toAddress(request.address());

        if (foster == null) {
            foster = Foster.create(principal.userId(), request.firstName(), request.lastName(),
                    request.phone(), address);
        } else {
            foster.updateProfile(request.firstName(), request.lastName(), request.phone(), address);
        }

        Foster saved = fosterRepository.save(foster);
        markProfileComplete(principal.userId());

        return ResponseEntity.status(HttpStatus.CREATED).body(FosterProfileResponse.from(saved));
    }

    // ==================== ADOPTER PROFILE ====================

    @GetMapping("/adopter")
    @PreAuthorize("hasRole('ADOPTER')")
    public ResponseEntity<AdopterProfileResponse> getAdopterProfile(@AuthenticationPrincipal UserPrincipal principal) {
        Adopter adopter = adopterRepository.findByUserId(principal.userId())
                .orElse(null);

        if (adopter == null) {
            return ResponseEntity.ok(new AdopterProfileResponse(null, null, null, null, null, null, false));
        }

        return ResponseEntity.ok(AdopterProfileResponse.from(adopter));
    }

    @PostMapping("/adopter")
    @PreAuthorize("hasRole('ADOPTER')")
    public ResponseEntity<AdopterProfileResponse> createOrUpdateAdopterProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AdopterProfileRequest request) {

        Adopter adopter = adopterRepository.findByUserId(principal.userId())
                .orElse(null);

        Address address = toAddress(request.address());

        if (adopter == null) {
            adopter = Adopter.create(principal.userId(), request.firstName(), request.lastName(),
                    request.phone(), request.livingSituation(), request.petExperience(), address);
        } else {
            adopter.updateProfile(request.firstName(), request.lastName(), request.phone(),
                    request.livingSituation(), request.petExperience(), address);
        }

        Adopter saved = adopterRepository.save(adopter);
        markProfileComplete(principal.userId());

        return ResponseEntity.status(HttpStatus.CREATED).body(AdopterProfileResponse.from(saved));
    }

    // ==================== VET PROFILE ====================

    @GetMapping("/vet")
    @PreAuthorize("hasRole('VET')")
    public ResponseEntity<VetProfileResponse> getVetProfile(@AuthenticationPrincipal UserPrincipal principal) {
        Vet vet = vetRepository.findByUserId(principal.userId())
                .orElse(null);

        if (vet == null) {
            return ResponseEntity.ok(new VetProfileResponse(null, null, null, null, null, null, false, false));
        }

        return ResponseEntity.ok(VetProfileResponse.from(vet));
    }

    @PostMapping("/vet")
    @PreAuthorize("hasRole('VET')")
    public ResponseEntity<VetProfileResponse> createOrUpdateVetProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody VetProfileRequest request) {

        Vet vet = vetRepository.findByUserId(principal.userId())
                .orElse(null);

        Address address = toAddress(request.address());

        if (vet == null) {
            vet = Vet.create(principal.userId(), request.clinicName(), request.licenseNumber(),
                    request.phone(), request.website(), request.description(), address);
        } else {
            vet.updateProfile(request.clinicName(), request.phone(), request.website(),
                    request.description(), address);
        }

        Vet saved = vetRepository.save(vet);
        markProfileComplete(principal.userId());

        return ResponseEntity.status(HttpStatus.CREATED).body(VetProfileResponse.from(saved));
    }

    // ==================== RESCUE ORG PROFILE ====================

    @GetMapping("/rescue-org")
    @PreAuthorize("hasRole('RESCUE_ORG')")
    public ResponseEntity<RescueOrgProfileResponse> getRescueOrgProfile(@AuthenticationPrincipal UserPrincipal principal) {
        RescueOrganization org = rescueOrganizationRepository.findByUserId(principal.userId())
                .orElse(null);

        if (org == null) {
            return ResponseEntity.ok(new RescueOrgProfileResponse(null, null, null, null, null, null, null, null, false, false));
        }

        // Convert S3 key to full URL for frontend
        String logoUrl = org.getLogoUrl() != null ? storageService.getPublicUrl(org.getLogoUrl()) : null;
        return ResponseEntity.ok(RescueOrgProfileResponse.fromWithLogoUrl(org, logoUrl));
    }

    @PostMapping("/rescue-org")
    @PreAuthorize("hasRole('RESCUE_ORG')")
    public ResponseEntity<RescueOrgProfileResponse> createOrUpdateRescueOrgProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RescueOrgProfileRequest request) {

        RescueOrganization org = rescueOrganizationRepository.findByUserId(principal.userId())
                .orElse(null);

        Address address = toAddress(request.address());
        SocialLinks socialLinks = toSocialLinks(request.socialLinks());

        if (org == null) {
            org = RescueOrganization.create(principal.userId(), request.name(), request.phone(),
                    request.website(), request.description(), request.contactName(), request.contactEmail(),
                    address, socialLinks);
        } else {
            org.updateProfile(request.name(), request.phone(), request.website(), request.description(),
                    request.contactName(), request.contactEmail(), address, socialLinks);
        }

        RescueOrganization saved = rescueOrganizationRepository.save(org);
        markProfileComplete(principal.userId());

        // Convert S3 key to full URL for frontend
        String logoUrl = saved.getLogoUrl() != null ? storageService.getPublicUrl(saved.getLogoUrl()) : null;
        return ResponseEntity.status(HttpStatus.CREATED).body(RescueOrgProfileResponse.fromWithLogoUrl(saved, logoUrl));
    }

    @PostMapping(value = "/rescue-org/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('RESCUE_ORG')")
    public ResponseEntity<RescueOrgProfileResponse> uploadRescueOrgLogo(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file) {

        RescueOrganization org = rescueOrganizationRepository.findByUserId(principal.userId())
                .orElseThrow(() -> new ResourceNotFoundException("RescueOrganization", principal.userId()));

        // Delete existing logo if present
        if (org.getLogoUrl() != null) {
            storageService.deleteFile(org.getLogoUrl());
        }

        // Upload new logo
        String s3Key = storageService.uploadFile(file, "rescue-logos/" + org.getId());
        String logoUrl = storageService.getPublicUrl(s3Key);

        org.updateLogoUrl(s3Key);
        RescueOrganization saved = rescueOrganizationRepository.save(org);

        // Return response with full URL for frontend
        return ResponseEntity.ok(RescueOrgProfileResponse.fromWithLogoUrl(saved, logoUrl));
    }

    @DeleteMapping("/rescue-org/logo")
    @PreAuthorize("hasRole('RESCUE_ORG')")
    public ResponseEntity<RescueOrgProfileResponse> deleteRescueOrgLogo(
            @AuthenticationPrincipal UserPrincipal principal) {

        RescueOrganization org = rescueOrganizationRepository.findByUserId(principal.userId())
                .orElseThrow(() -> new ResourceNotFoundException("RescueOrganization", principal.userId()));

        if (org.getLogoUrl() != null) {
            storageService.deleteFile(org.getLogoUrl());
            org.updateLogoUrl(null);
            org = rescueOrganizationRepository.save(org);
        }

        return ResponseEntity.ok(RescueOrgProfileResponse.from(org));
    }

    // ==================== NOTIFICATION PREFERENCES ====================

    @GetMapping("/notifications")
    public ResponseEntity<NotificationPreferencesResponse> getNotificationPreferences(
            @AuthenticationPrincipal UserPrincipal principal) {
        var user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.userId()));

        NotificationPreferences prefs = user.getNotificationPreferences();
        if (prefs == null) {
            prefs = NotificationPreferences.defaults();
        }

        return ResponseEntity.ok(new NotificationPreferencesResponse(
                prefs.emailStatusChanges(),
                prefs.emailNewApplications(),
                prefs.emailFavoriteUpdates(),
                prefs.inAppEnabled()
        ));
    }

    @PutMapping("/notifications")
    public ResponseEntity<NotificationPreferencesResponse> updateNotificationPreferences(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody NotificationPreferencesRequest request) {
        var user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.userId()));

        NotificationPreferences prefs = new NotificationPreferences(
                request.emailStatusChanges(),
                request.emailNewApplications(),
                request.emailFavoriteUpdates(),
                request.inAppEnabled()
        );

        user.updateNotificationPreferences(prefs);
        userRepository.save(user);

        return ResponseEntity.ok(new NotificationPreferencesResponse(
                prefs.emailStatusChanges(),
                prefs.emailNewApplications(),
                prefs.emailFavoriteUpdates(),
                prefs.inAppEnabled()
        ));
    }

    // ==================== HELPER METHODS ====================

    private void markProfileComplete(UUID userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.markProfileComplete();
            userRepository.save(user);
        });
    }

    private Address toAddress(AddressDto dto) {
        if (dto == null) {
            return null;
        }
        return new Address(dto.street(), dto.city(), dto.state(), dto.postalCode(), dto.country());
    }

    private SocialLinks toSocialLinks(SocialLinksDto dto) {
        if (dto == null) {
            return SocialLinks.empty();
        }
        return new SocialLinks(dto.facebook(), dto.instagram(), dto.twitter(), dto.tiktok());
    }

    // ==================== REQUEST DTOs ====================

    public record AddressDto(
            String street,
            String city,
            String state,
            String postalCode,
            String country
    ) {}

    public record SocialLinksDto(
            String facebook,
            String instagram,
            String twitter,
            String tiktok
    ) {}

    public record FosterProfileRequest(
            @NotBlank(message = "First name is required")
            String firstName,
            @NotBlank(message = "Last name is required")
            String lastName,
            String phone,
            AddressDto address
    ) {}

    public record AdopterProfileRequest(
            @NotBlank(message = "First name is required")
            String firstName,
            @NotBlank(message = "Last name is required")
            String lastName,
            String phone,
            String livingSituation,
            String petExperience,
            AddressDto address
    ) {}

    public record VetProfileRequest(
            @NotBlank(message = "Clinic name is required")
            String clinicName,
            @NotBlank(message = "License number is required")
            String licenseNumber,
            String phone,
            String website,
            String description,
            AddressDto address
    ) {}

    public record RescueOrgProfileRequest(
            @NotBlank(message = "Organization name is required")
            String name,
            String phone,
            String website,
            String description,
            String contactName,
            String contactEmail,
            AddressDto address,
            SocialLinksDto socialLinks
    ) {}

    // ==================== RESPONSE DTOs ====================

    public record ProfileStatusResponse(
            boolean isComplete,
            String role
    ) {}

    public record FosterProfileResponse(
            String firstName,
            String lastName,
            String phone,
            AddressDto address,
            boolean isComplete
    ) {
        static FosterProfileResponse from(Foster foster) {
            AddressDto addressDto = foster.getAddress() != null ?
                    new AddressDto(foster.getAddress().street(), foster.getAddress().city(),
                            foster.getAddress().state(), foster.getAddress().postalCode(),
                            foster.getAddress().country()) : null;
            return new FosterProfileResponse(
                    foster.getFirstName(),
                    foster.getLastName(),
                    foster.getPhone(),
                    addressDto,
                    true
            );
        }
    }

    public record AdopterProfileResponse(
            String firstName,
            String lastName,
            String phone,
            String livingSituation,
            String petExperience,
            AddressDto address,
            boolean isComplete
    ) {
        static AdopterProfileResponse from(Adopter adopter) {
            AddressDto addressDto = adopter.getAddress() != null ?
                    new AddressDto(adopter.getAddress().street(), adopter.getAddress().city(),
                            adopter.getAddress().state(), adopter.getAddress().postalCode(),
                            adopter.getAddress().country()) : null;
            return new AdopterProfileResponse(
                    adopter.getFirstName(),
                    adopter.getLastName(),
                    adopter.getPhone(),
                    adopter.getLivingSituation(),
                    adopter.getPetExperience(),
                    addressDto,
                    true
            );
        }
    }

    public record VetProfileResponse(
            String clinicName,
            String licenseNumber,
            String phone,
            String website,
            String description,
            AddressDto address,
            boolean verified,
            boolean isComplete
    ) {
        static VetProfileResponse from(Vet vet) {
            AddressDto addressDto = vet.getAddress() != null ?
                    new AddressDto(vet.getAddress().street(), vet.getAddress().city(),
                            vet.getAddress().state(), vet.getAddress().postalCode(),
                            vet.getAddress().country()) : null;
            return new VetProfileResponse(
                    vet.getClinicName(),
                    vet.getLicenseNumber(),
                    vet.getPhone(),
                    vet.getWebsite(),
                    vet.getDescription(),
                    addressDto,
                    vet.isVerified(),
                    true
            );
        }
    }

    public record RescueOrgProfileResponse(
            String name,
            String phone,
            String website,
            String description,
            String contactName,
            String contactEmail,
            AddressDto address,
            String logoUrl,
            boolean verified,
            boolean isComplete
    ) {
        static RescueOrgProfileResponse from(RescueOrganization org) {
            AddressDto addressDto = org.getAddress() != null ?
                    new AddressDto(org.getAddress().street(), org.getAddress().city(),
                            org.getAddress().state(), org.getAddress().postalCode(),
                            org.getAddress().country()) : null;
            return new RescueOrgProfileResponse(
                    org.getName(),
                    org.getPhone(),
                    org.getWebsite(),
                    org.getDescription(),
                    org.getContactName(),
                    org.getContactEmail(),
                    addressDto,
                    org.getLogoUrl(),
                    org.isVerified(),
                    true
            );
        }

        static RescueOrgProfileResponse fromWithLogoUrl(RescueOrganization org, String logoUrl) {
            AddressDto addressDto = org.getAddress() != null ?
                    new AddressDto(org.getAddress().street(), org.getAddress().city(),
                            org.getAddress().state(), org.getAddress().postalCode(),
                            org.getAddress().country()) : null;
            return new RescueOrgProfileResponse(
                    org.getName(),
                    org.getPhone(),
                    org.getWebsite(),
                    org.getDescription(),
                    org.getContactName(),
                    org.getContactEmail(),
                    addressDto,
                    logoUrl,
                    org.isVerified(),
                    true
            );
        }
    }

    public record NotificationPreferencesRequest(
            boolean emailStatusChanges,
            boolean emailNewApplications,
            boolean emailFavoriteUpdates,
            boolean inAppEnabled
    ) {}

    public record NotificationPreferencesResponse(
            boolean emailStatusChanges,
            boolean emailNewApplications,
            boolean emailFavoriteUpdates,
            boolean inAppEnabled
    ) {}
}
