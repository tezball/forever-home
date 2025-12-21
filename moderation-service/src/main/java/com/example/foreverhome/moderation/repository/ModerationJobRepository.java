package com.example.foreverhome.moderation.repository;

import com.example.foreverhome.moderation.domain.JobStatus;
import com.example.foreverhome.moderation.domain.ModerationJob;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ModerationJobRepository extends CrudRepository<ModerationJob, UUID> {

    List<ModerationJob> findByStatus(JobStatus status);

    @Query("SELECT * FROM moderation_jobs ORDER BY created_at DESC LIMIT :limit")
    List<ModerationJob> findRecentJobs(@Param("limit") int limit);

    @Query("SELECT * FROM moderation_jobs WHERE status = 'RUNNING' ORDER BY started_at ASC LIMIT 1")
    Optional<ModerationJob> findCurrentlyRunningJob();

    @Query("SELECT * FROM moderation_jobs ORDER BY created_at DESC LIMIT 1")
    Optional<ModerationJob> findLatestJob();

    @Query("SELECT COUNT(*) FROM moderation_jobs WHERE status = :status")
    long countByStatus(@Param("status") String status);

    @Query("SELECT SUM(processed_pets) FROM moderation_jobs WHERE status = 'COMPLETED'")
    Long sumProcessedPetsForCompletedJobs();

    @Query("SELECT SUM(flagged_pets) FROM moderation_jobs WHERE status = 'COMPLETED'")
    Long sumFlaggedPetsForCompletedJobs();
}
