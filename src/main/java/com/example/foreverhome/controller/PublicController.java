package com.example.foreverhome.controller;

import com.example.foreverhome.domain.pet.Pet;
import com.example.foreverhome.domain.pet.PetImage;
import com.example.foreverhome.domain.pet.PetStatus;
import com.example.foreverhome.domain.profile.RescueOrganization;
import com.example.foreverhome.domain.profile.Vet;
import com.example.foreverhome.repository.AdoptionRepository;
import com.example.foreverhome.repository.PetImageRepository;
import com.example.foreverhome.repository.PetRepository;
import com.example.foreverhome.repository.RescueOrganizationRepository;
import com.example.foreverhome.repository.VetRepository;
import com.example.foreverhome.repository.VetSignOffRepository;
import com.example.foreverhome.service.S3StorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for public endpoints accessible without authentication.
 */
@RestController
@RequestMapping("/api")
public class PublicController {

    private final RescueOrganizationRepository rescueOrganizationRepository;
    private final PetRepository petRepository;
    private final PetImageRepository petImageRepository;
    private final AdoptionRepository adoptionRepository;
    private final VetRepository vetRepository;
    private final VetSignOffRepository vetSignOffRepository;
    private final S3StorageService storageService;

    public PublicController(RescueOrganizationRepository rescueOrganizationRepository,
                           PetRepository petRepository,
                           PetImageRepository petImageRepository,
                           AdoptionRepository adoptionRepository,
                           VetRepository vetRepository,
                           VetSignOffRepository vetSignOffRepository,
                           S3StorageService storageService) {
        this.rescueOrganizationRepository = rescueOrganizationRepository;
        this.petRepository = petRepository;
        this.petImageRepository = petImageRepository;
        this.adoptionRepository = adoptionRepository;
        this.vetRepository = vetRepository;
        this.vetSignOffRepository = vetSignOffRepository;
        this.storageService = storageService;
    }

    /**
     * Get all verified rescue organizations (public).
     */
    @GetMapping("/rescues")
    public ResponseEntity<List<RescueOrgPublicResponse>> listRescueOrganizations() {
        List<RescueOrganization> orgs = rescueOrganizationRepository.findAllVerified();
        List<RescueOrgPublicResponse> response = orgs.stream()
                .map(org -> {
                    // Count available pets for this rescue org
                    long petCount = petRepository.countByRescueOrgIdAndStatus(org.getId(), PetStatus.AVAILABLE);
                    return RescueOrgPublicResponse.from(org, petCount);
                })
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Get a specific rescue organization by ID (public).
     */
    @GetMapping("/rescues/{id}")
    public ResponseEntity<RescueOrgPublicResponse> getRescueOrganization(@PathVariable UUID id) {
        return rescueOrganizationRepository.findById(id)
                .filter(RescueOrganization::isVerified)
                .map(org -> {
                    long petCount = petRepository.countByRescueOrgIdAndStatus(org.getId(), PetStatus.AVAILABLE);
                    return ResponseEntity.ok(RescueOrgPublicResponse.from(org, petCount));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get available pets for a rescue organization (public).
     */
    @GetMapping("/rescues/{id}/pets")
    public ResponseEntity<List<PetPublicResponse>> getRescuePets(@PathVariable UUID id) {
        // Verify the rescue org exists and is verified
        return rescueOrganizationRepository.findById(id)
                .filter(RescueOrganization::isVerified)
                .map(org -> {
                    List<Pet> pets = petRepository.findByRescueOrgIdAndStatus(org.getId(), PetStatus.AVAILABLE);
                    List<PetPublicResponse> response = pets.stream()
                            .map(pet -> {
                                List<String> imageUrls = petImageRepository.findByPetIdOrderByDisplayOrder(pet.getId())
                                        .stream()
                                        .map(img -> storageService.getPublicUrl(img.getS3Key()))
                                        .toList();
                                return PetPublicResponse.from(pet, imageUrls);
                            })
                            .toList();
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all verified vets (public).
     */
    @GetMapping("/vets")
    public ResponseEntity<List<VetPublicResponse>> listVets() {
        List<Vet> vets = vetRepository.findAllVerified();
        List<VetPublicResponse> response = vets.stream()
                .map(vet -> {
                    long signOffCount = vetSignOffRepository.countByVetId(vet.getId());
                    return VetPublicResponse.from(vet, signOffCount);
                })
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Get a specific vet by ID (public).
     */
    @GetMapping("/vets/{id}")
    public ResponseEntity<VetPublicResponse> getVet(@PathVariable UUID id) {
        return vetRepository.findById(id)
                .filter(Vet::isVerified)
                .map(vet -> {
                    long signOffCount = vetSignOffRepository.countByVetId(vet.getId());
                    return ResponseEntity.ok(VetPublicResponse.from(vet, signOffCount));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get platform statistics (public).
     */
    @GetMapping("/stats")
    public ResponseEntity<PlatformStatsResponse> getPlatformStats() {
        long petsAvailable = petRepository.countByStatus(PetStatus.AVAILABLE);
        long totalAdoptions = adoptionRepository.countAll();
        long totalRescues = rescueOrganizationRepository.findAllVerified().size();
        return ResponseEntity.ok(new PlatformStatsResponse(petsAvailable, totalAdoptions, totalRescues));
    }

    record PlatformStatsResponse(
            long petsAvailable,
            long totalAdoptions,
            long totalRescues
    ) {}

    record VetPublicResponse(
            UUID id,
            String clinicName,
            String description,
            String location,
            String website,
            String phone,
            String logoUrl,
            long signOffCount
    ) {
        static VetPublicResponse from(Vet vet, long signOffCount) {
            String location = null;
            if (vet.getAddress() != null) {
                String city = vet.getAddress().city();
                String state = vet.getAddress().state();
                if (city != null && state != null) {
                    location = city + ", " + state;
                } else if (city != null) {
                    location = city;
                } else if (state != null) {
                    location = state;
                }
            }
            return new VetPublicResponse(
                    vet.getId(),
                    vet.getClinicName(),
                    vet.getDescription(),
                    location,
                    vet.getWebsite(),
                    vet.getPhone(),
                    vet.getLogoUrl(),
                    signOffCount
            );
        }
    }

    // Response DTOs
    record RescueOrgPublicResponse(
            UUID id,
            String name,
            String description,
            String location,
            String website,
            String email,
            String phone,
            String logoUrl,
            long petCount
    ) {
        static RescueOrgPublicResponse from(RescueOrganization org, long petCount) {
            String location = null;
            if (org.getAddress() != null) {
                String city = org.getAddress().city();
                String state = org.getAddress().state();
                if (city != null && state != null) {
                    location = city + ", " + state;
                } else if (city != null) {
                    location = city;
                } else if (state != null) {
                    location = state;
                }
            }
            return new RescueOrgPublicResponse(
                    org.getId(),
                    org.getName(),
                    org.getDescription(),
                    location,
                    org.getWebsite(),
                    org.getContactEmail(),
                    org.getPhone(),
                    org.getLogoUrl(),
                    petCount
            );
        }
    }

    record PetPublicResponse(
            UUID id,
            String name,
            String species,
            String breed,
            int age,
            String ageUnit,
            String sex,
            String size,
            String microchipId,
            String description,
            String healthNotes,
            String status,
            UUID rescueOrgId,
            List<String> imageUrls
    ) {
        static PetPublicResponse from(Pet pet, List<String> imageUrls) {
            return new PetPublicResponse(
                    pet.getId(),
                    pet.getName(),
                    pet.getSpecies().name(),
                    pet.getBreed() != null ? pet.getBreed().name() : null,
                    pet.getAge(),
                    pet.getAgeUnit().name(),
                    pet.getSex().name(),
                    pet.getSize().name(),
                    pet.getMicrochipId(),
                    pet.getDescription(),
                    pet.getHealthNotes(),
                    pet.getStatus().name(),
                    pet.getRescueOrgId(),
                    imageUrls
            );
        }
    }
}
