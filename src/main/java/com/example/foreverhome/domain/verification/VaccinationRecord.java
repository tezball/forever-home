package com.example.foreverhome.domain.verification;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Individual vaccination entry within a vet signoff.
 */
@Table("vaccination_records")
public class VaccinationRecord {

    @Id
    private UUID id;

    @Column("signoff_id")
    private UUID signOffId;

    @Column("vaccine_name")
    private String vaccineName;

    @Column("date_administered")
    private LocalDate dateAdministered;

    @Column("expiration_date")
    private LocalDate expirationDate;

    protected VaccinationRecord() {
    }

    private VaccinationRecord(UUID id, UUID signOffId, String vaccineName,
                               LocalDate dateAdministered, LocalDate expirationDate) {
        this.id = id;
        this.signOffId = signOffId;
        this.vaccineName = vaccineName;
        this.dateAdministered = dateAdministered;
        this.expirationDate = expirationDate;
    }

    public static VaccinationRecord create(UUID signOffId, String vaccineName,
                                            LocalDate dateAdministered, LocalDate expirationDate) {
        if (signOffId == null) {
            throw new IllegalArgumentException("signOffId cannot be null");
        }
        if (vaccineName == null || vaccineName.isBlank()) {
            throw new IllegalArgumentException("vaccineName cannot be null or blank");
        }
        if (dateAdministered == null) {
            throw new IllegalArgumentException("dateAdministered cannot be null");
        }
        return new VaccinationRecord(UUID.randomUUID(), signOffId, vaccineName.trim(),
                dateAdministered, expirationDate);
    }

    public UUID getId() {
        return id;
    }

    public UUID getSignOffId() {
        return signOffId;
    }

    public String getVaccineName() {
        return vaccineName;
    }

    public LocalDate getDateAdministered() {
        return dateAdministered;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public boolean isExpired() {
        return expirationDate != null && expirationDate.isBefore(LocalDate.now());
    }

    public boolean isExpiringSoon(int daysThreshold) {
        if (expirationDate == null) {
            return false;
        }
        LocalDate threshold = LocalDate.now().plusDays(daysThreshold);
        return expirationDate.isBefore(threshold);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VaccinationRecord that = (VaccinationRecord) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
