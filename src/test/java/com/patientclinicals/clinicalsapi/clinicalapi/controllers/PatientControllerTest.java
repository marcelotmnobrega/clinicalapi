// language: java
package com.patientclinicals.clinicalsapi.clinicalapi.controllers;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.patientclinicals.clinicalsapi.clinicalapi.models.Patient;
import com.patientclinicals.clinicalsapi.clinicalapi.models.repos.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class PatientControllerTest {

    private PatientRepository patientRepository;
    private PatientController controller;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        patientRepository = mock(PatientRepository.class);
        controller = new PatientController(patientRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    // Helper to create a Patient with only id set (other fields optional)
    private Patient makePatient(Long id) {
        Patient p = new Patient();
        p.setId(id);
        return p;
    }

    @Test
    void list_returnsAllPatients() throws Exception {
        Patient p1 = makePatient(1L);
        Patient p2 = makePatient(2L);
        List<Patient> list = Arrays.asList(p1, p2);

        when(patientRepository.findAll()).thenReturn(list);

        mockMvc.perform(get("/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[1].id", is(2)));

        verify(patientRepository, times(1)).findAll();
    }

    @Test
    void getById_found_returnsPatient() throws Exception {
        Patient p = makePatient(1L);
        when(patientRepository.findById(1L)).thenReturn(Optional.of(p));

        mockMvc.perform(get("/patients/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));

        verify(patientRepository, times(1)).findById(1L);
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(patientRepository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/patients/{id}", 1L))
                .andExpect(status().isNotFound());

        verify(patientRepository, times(1)).findById(1L);
    }

    @Test
    void create_savesPatient_andReturnsCreatedWithLocation() throws Exception {
        Patient toCreate = new Patient(); // id null
        // set any other fields if desired, e.g., name/age, but id is sufficient for controller behavior
        Patient saved = makePatient(1L);

        when(patientRepository.save(any(Patient.class))).thenReturn(saved);

        String body = objectMapper.writeValueAsString(toCreate);

        mockMvc.perform(post("/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/patients/1")))
                .andExpect(jsonPath("$.id", is(1)));

        ArgumentCaptor<Patient> captor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository, times(1)).save(captor.capture());
        // id on the saved argument from controller may be null (controller doesn't set id on create)
        // but verify object passed to save is the same structure (at least not null)
        Patient passed = captor.getValue();
        // ensure passed exists
        assert passed != null;
    }

    @Test
    void update_existingId_updatesAndReturnsOk() throws Exception {
        Patient incoming = new Patient(); // body without id
        String body = objectMapper.writeValueAsString(incoming);
        Patient saved = makePatient(1L);

        when(patientRepository.existsById(1L)).thenReturn(true);
        when(patientRepository.save(any(Patient.class))).thenReturn(saved);

        mockMvc.perform(put("/patients/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));

        ArgumentCaptor<Patient> captor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository, times(1)).save(captor.capture());
        Patient passed = captor.getValue();
        // controller should set the id on the entity before saving
        assert passed.getId() != null && passed.getId().equals(1L);
    }

    @Test
    void update_nonExistingId_returnsNotFound() throws Exception {
        Patient incoming = new Patient();
        String body = objectMapper.writeValueAsString(incoming);

        when(patientRepository.existsById(1L)).thenReturn(false);

        mockMvc.perform(put("/patients/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());

        verify(patientRepository, never()).save(any());
    }

    @Test
    void delete_existingId_deletesAndReturnsNoContent() throws Exception {
        when(patientRepository.existsById(1L)).thenReturn(true);
        doNothing().when(patientRepository).deleteById(1L);

        mockMvc.perform(delete("/patients/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(patientRepository, times(1)).existsById(1L);
        verify(patientRepository, times(1)).deleteById(1L);
    }

    @Test
    void delete_nonExistingId_returnsNotFound() throws Exception {
        when(patientRepository.existsById(1L)).thenReturn(false);

        mockMvc.perform(delete("/patients/{id}", 1L))
                .andExpect(status().isNotFound());

        verify(patientRepository, times(1)).existsById(1L);
        verify(patientRepository, never()).deleteById(anyLong());
    }
}