package com.example.foreverhome.repository;

import com.example.foreverhome.domain.verification.VaccinationRecord;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VaccinationRecordRepository extends CrudRepository<VaccinationRecord, UUID> {

    List<VaccinationRecord> findBySignOffId(UUID signOffId);
}
