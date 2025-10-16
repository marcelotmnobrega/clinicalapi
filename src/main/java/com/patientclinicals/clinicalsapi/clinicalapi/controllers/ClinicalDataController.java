package com.patientclinicals.clinicalsapi.clinicalapi.controllers;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.patientclinicals.clinicalsapi.clinicalapi.models.ClinicalData;
import com.patientclinicals.clinicalsapi.clinicalapi.models.repos.ClinicalDataRepository;
import com.patientclinicals.clinicalsapi.clinicalapi.models.repos.PatientRepository;

@CrossOrigin(origins = "http://localhost:3000") // adjust origin(s) for your frontend
@RestController
@RequestMapping("/clinicaldata")
public class ClinicalDataController {

    private final ClinicalDataRepository clinicalDataRepository;
    private final PatientRepository patientRepository;

    public ClinicalDataController(ClinicalDataRepository clinicalDataRepository, PatientRepository patientRepository) {
        this.clinicalDataRepository = clinicalDataRepository;
        this.patientRepository = patientRepository;
    }

    @GetMapping
    public List<ClinicalData> list() {
        return clinicalDataRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClinicalData> getById(@PathVariable Long id) {
        return clinicalDataRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ClinicalData> create(@RequestBody ClinicalData clinicalData) {
        ClinicalData saved = clinicalDataRepository.save(clinicalData);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClinicalData> update(@PathVariable Long id, @RequestBody ClinicalData clinicalData) {
        if (!clinicalDataRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        clinicalData.setId(id);
        ClinicalData saved = clinicalDataRepository.save(clinicalData);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!clinicalDataRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        clinicalDataRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    //method that receives patient id, clinical data and sabes it to the database
    @PostMapping("/clinicals")
    public ClinicalData saveClinicalData(@RequestBody ClinicalDataRequest request) {
        
        ClinicalData clinicalData = new ClinicalData();
        clinicalData.setComponentName(request.getComponentName());
        clinicalData.setComponentValue(request.getComponentValue());

        patientRepository.findById(request.getPatientId()).ifPresent(clinicalData::setPatient);

        return clinicalDataRepository.save(clinicalData);
    }
}