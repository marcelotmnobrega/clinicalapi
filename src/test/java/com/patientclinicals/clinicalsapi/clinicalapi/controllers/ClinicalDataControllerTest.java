package com.patientclinicals.clinicalsapi.clinicalapi.controllers;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.patientclinicals.clinicalsapi.clinicalapi.models.ClinicalData;
import com.patientclinicals.clinicalsapi.clinicalapi.models.Patient;
import com.patientclinicals.clinicalsapi.clinicalapi.models.repos.ClinicalDataRepository;
import com.patientclinicals.clinicalsapi.clinicalapi.models.repos.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class ClinicalDataControllerTest {

    private ClinicalDataRepository clinicalDataRepository;
    private PatientRepository patientRepository;
    private ClinicalDataController controller;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        clinicalDataRepository = mock(ClinicalDataRepository.class);
        patientRepository = mock(PatientRepository.class);
        controller = new ClinicalDataController(clinicalDataRepository, patientRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    private ClinicalData makeClinicalData(Long id, String name, String value) {
        ClinicalData cd = new ClinicalData();
        cd.setId(id);
        cd.setComponentName(name);
        cd.setComponentValue(value);
        return cd;
    }

    private Patient makePatient(Long id) {
        Patient p = new Patient();
        p.setId(id);
        return p;
    }

    @Test
    void list_returnsAllClinicalData() throws Exception {
        ClinicalData c1 = makeClinicalData(1L, "bp", "120/80");
        ClinicalData c2 = makeClinicalData(2L, "hr", "72");
        List<ClinicalData> list = Arrays.asList(c1, c2);

        when(clinicalDataRepository.findAll()).thenReturn(list);

        mockMvc.perform(get("/clinicaldata"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].componentName", is("bp")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].componentName", is("hr")));

        verify(clinicalDataRepository, times(1)).findAll();
    }

    @Test
    void getById_found_returnsClinicalData() throws Exception {
        ClinicalData c = makeClinicalData(1L, "bp", "120/80");
        when(clinicalDataRepository.findById(1L)).thenReturn(Optional.of(c));

        mockMvc.perform(get("/clinicaldata/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.componentName", is("bp")));

        verify(clinicalDataRepository, times(1)).findById(1L);
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(clinicalDataRepository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/clinicaldata/{id}", 1L))
                .andExpect(status().isNotFound());

        verify(clinicalDataRepository, times(1)).findById(1L);
    }

    @Test
    void create_savesClinicalData_andReturnsCreatedWithLocation() throws Exception {
        ClinicalData toCreate = new ClinicalData();
        toCreate.setComponentName("temp");
        toCreate.setComponentValue("98.6");

        ClinicalData saved = makeClinicalData(5L, "temp", "98.6");
        when(clinicalDataRepository.save(any(ClinicalData.class))).thenReturn(saved);

        String body = objectMapper.writeValueAsString(toCreate);

        mockMvc.perform(post("/clinicaldata")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/clinicaldata/5")))
                .andExpect(jsonPath("$.id", is(5)))
                .andExpect(jsonPath("$.componentName", is("temp")));

        ArgumentCaptor<ClinicalData> captor = ArgumentCaptor.forClass(ClinicalData.class);
        verify(clinicalDataRepository, times(1)).save(captor.capture());
        ClinicalData passed = captor.getValue();
        assert passed != null;
    }

    @Test
    void update_existingId_updatesAndReturnsOk() throws Exception {
        ClinicalData incoming = new ClinicalData();
        incoming.setComponentName("weight");
        incoming.setComponentValue("160");

        ClinicalData saved = makeClinicalData(10L, "weight", "160");

        when(clinicalDataRepository.existsById(10L)).thenReturn(true);
        when(clinicalDataRepository.save(any(ClinicalData.class))).thenReturn(saved);

        String body = objectMapper.writeValueAsString(incoming);

        mockMvc.perform(put("/clinicaldata/{id}", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.componentName", is("weight")));

        ArgumentCaptor<ClinicalData> captor = ArgumentCaptor.forClass(ClinicalData.class);
        verify(clinicalDataRepository, times(1)).save(captor.capture());
        ClinicalData passed = captor.getValue();
        assert passed.getId() != null && passed.getId().equals(10L);
    }

    @Test
    void update_nonExistingId_returnsNotFound() throws Exception {
        ClinicalData incoming = new ClinicalData();
        incoming.setComponentName("weight");
        incoming.setComponentValue("160");

        when(clinicalDataRepository.existsById(11L)).thenReturn(false);

        String body = objectMapper.writeValueAsString(incoming);

        mockMvc.perform(put("/clinicaldata/{id}", 11L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());

        verify(clinicalDataRepository, never()).save(any());
    }

    @Test
    void delete_existingId_deletesAndReturnsNoContent() throws Exception {
        when(clinicalDataRepository.existsById(7L)).thenReturn(true);
        doNothing().when(clinicalDataRepository).deleteById(7L);

        mockMvc.perform(delete("/clinicaldata/{id}", 7L))
                .andExpect(status().isNoContent());

        verify(clinicalDataRepository, times(1)).existsById(7L);
        verify(clinicalDataRepository, times(1)).deleteById(7L);
    }

    @Test
    void delete_nonExistingId_returnsNotFound() throws Exception {
        when(clinicalDataRepository.existsById(8L)).thenReturn(false);

        mockMvc.perform(delete("/clinicaldata/{id}", 8L))
                .andExpect(status().isNotFound());

        verify(clinicalDataRepository, times(1)).existsById(8L);
        verify(clinicalDataRepository, never()).deleteById(anyLong());
    }

    @Test
    void saveClinicalData_withExistingPatient_setsPatientAndSaves() throws Exception {
        Long patientId = 1L;
        Map<String, Object> req = Map.of(
                "patientId", patientId,
                "componentName", "glucose",
                "componentValue", "90"
        );

        Patient patient = makePatient(patientId);
        ClinicalData saved = makeClinicalData(20L, "glucose", "90");
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(clinicalDataRepository.save(any(ClinicalData.class))).thenReturn(saved);

        String body = objectMapper.writeValueAsString(req);

        mockMvc.perform(post("/clinicaldata/clinicals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(20)))
                .andExpect(jsonPath("$.componentName", is("glucose")))
                .andExpect(jsonPath("$.componentValue", is("90")));

        ArgumentCaptor<ClinicalData> captor = ArgumentCaptor.forClass(ClinicalData.class);
        verify(clinicalDataRepository, times(1)).save(captor.capture());
        ClinicalData passed = captor.getValue();
        assert passed.getPatient() != null && passed.getPatient().getId().equals(patientId);
    }

    @Test
    void saveClinicalData_withMissingPatient_savesWithoutPatient() throws Exception {
        Long patientId = 99L;
        Map<String, Object> req = Map.of(
                "patientId", patientId,
                "componentName", "o2",
                "componentValue", "98"
        );

        ClinicalData saved = makeClinicalData(21L, "o2", "98");
        when(patientRepository.findById(patientId)).thenReturn(Optional.empty());
        when(clinicalDataRepository.save(any(ClinicalData.class))).thenReturn(saved);

        String body = objectMapper.writeValueAsString(req);

        mockMvc.perform(post("/clinicaldata/clinicals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(21)))
                .andExpect(jsonPath("$.componentName", is("o2")));

        ArgumentCaptor<ClinicalData> captor = ArgumentCaptor.forClass(ClinicalData.class);
        verify(clinicalDataRepository, times(1)).save(captor.capture());
        ClinicalData passed = captor.getValue();
        assert passed.getPatient() == null;
    }
}