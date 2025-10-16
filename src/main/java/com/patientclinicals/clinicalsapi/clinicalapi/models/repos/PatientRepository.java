package com.patientclinicals.clinicalsapi.clinicalapi.models.repos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.patientclinicals.clinicalsapi.clinicalapi.models.Patient;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
}