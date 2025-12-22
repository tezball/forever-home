package com.example.foreverhome.moderation.service.admin;

import com.example.foreverhome.moderation.domain.admin.AccountStatus;
import com.example.foreverhome.moderation.domain.admin.PetStatus;
import com.example.foreverhome.moderation.domain.admin.UserRole;
import com.example.foreverhome.moderation.repository.admin.ContentFlagRepository;
import com.example.foreverhome.moderation.repository.admin.UserRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class AdminAnalyticsService {

    private final UserRepository userRepository;
    private final ContentFlagRepository contentFlagRepository;
    private final JdbcClient jdbcClient;

    public AdminAnalyticsService(UserRepository userRepository,
                                  ContentFlagRepository contentFlagRepository,
                                  JdbcClient jdbcClient) {
        this.userRepository = userRepository;
        this.contentFlagRepository = contentFlagRepository;
        this.jdbcClient = jdbcClient;
    }

    public Analytics getAnalytics() {
        long totalUsers = userRepository.count();
        long totalPets = countPets();
        long totalAdoptions = countAdoptions();

        // Count pending approvals (RESCUE_ORGs with PENDING status)
        long pendingApprovals = userRepository.countByRoleAndStatus(UserRole.RESCUE_ORG, AccountStatus.PENDING);

        // User distribution by role
        long adopters = userRepository.countByRole(UserRole.ADOPTER);
        long fosters = userRepository.countByRole(UserRole.FOSTER);
        long rescueOrgs = userRepository.countByRole(UserRole.RESCUE_ORG);
        long vets = userRepository.countByRole(UserRole.VET);
        long admins = userRepository.countByRole(UserRole.ADMIN);

        // Pet statistics by status
        long petsAvailable = countPetsByStatus(PetStatus.AVAILABLE);
        long petsPendingRescue = countPetsByStatus(PetStatus.PENDING_RESCUE);
        long petsPendingVet = countPetsByStatus(PetStatus.PENDING_VET);
        long petsInProgress = countPetsByStatus(PetStatus.IN_PROGRESS);
        long petsAdopted = countPetsByStatus(PetStatus.ADOPTED);
        long petsDraft = countPetsByStatus(PetStatus.DRAFT);
        long petsOnHold = countPetsByStatus(PetStatus.ON_HOLD);
        long petsWithdrawn = countPetsByStatus(PetStatus.WITHDRAWN);

        // Content flags
        long pendingFlags = contentFlagRepository.countPending();

        return new Analytics(
                totalUsers,
                totalPets,
                totalAdoptions,
                pendingApprovals,
                pendingFlags,
                new UserDistribution(adopters, fosters, rescueOrgs, vets, admins),
                new PetStatistics(petsAvailable, petsPendingRescue, petsPendingVet, petsInProgress,
                        petsAdopted, petsDraft, petsOnHold, petsWithdrawn)
        );
    }

    private long countPets() {
        return jdbcClient.sql("SELECT COUNT(*) FROM pets")
                .query(Long.class)
                .single();
    }

    private long countPetsByStatus(PetStatus status) {
        return jdbcClient.sql("SELECT COUNT(*) FROM pets WHERE status = :status")
                .param("status", status.name())
                .query(Long.class)
                .single();
    }

    private long countAdoptions() {
        return jdbcClient.sql("SELECT COUNT(*) FROM adoptions")
                .query(Long.class)
                .single();
    }

    public record Analytics(
            long totalUsers,
            long totalPets,
            long totalAdoptions,
            long pendingApprovals,
            long pendingFlags,
            UserDistribution userDistribution,
            PetStatistics petStatistics
    ) {}

    public record UserDistribution(
            long adopters,
            long fosters,
            long rescueOrgs,
            long vets,
            long admins
    ) {
        public long total() {
            return adopters + fosters + rescueOrgs + vets + admins;
        }
    }

    public record PetStatistics(
            long available,
            long pendingRescue,
            long pendingVet,
            long inProgress,
            long adopted,
            long draft,
            long onHold,
            long withdrawn
    ) {
        public long total() {
            return available + pendingRescue + pendingVet + inProgress + adopted + draft + onHold + withdrawn;
        }
    }
}
