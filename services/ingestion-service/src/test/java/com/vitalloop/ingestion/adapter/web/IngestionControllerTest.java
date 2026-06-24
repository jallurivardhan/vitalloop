package com.vitalloop.ingestion.adapter.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vitalloop.ingestion.application.IngestionAccepted;
import com.vitalloop.ingestion.application.IngestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(IngestionController.class)
class IngestionControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private IngestionService ingestionService;

  @Test
  void validRequestReturns202WithEventId() throws Exception {
    when(ingestionService.accept(any())).thenReturn(new IngestionAccepted("evt-123"));

    String body =
        """
        {
          "patientId": "patient-1",
          "vitalType": "heart_rate",
          "value": 72.0,
          "unit": "bpm"
        }
        """;

    mockMvc
        .perform(post("/api/v1/vitals").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.eventId").value("evt-123"));
  }

  @Test
  void invalidRequestReturns400WithFieldErrors() throws Exception {
    // Blank patientId and missing value both violate the request constraints.
    String body =
        """
        {
          "patientId": "",
          "vitalType": "heart_rate",
          "unit": "bpm"
        }
        """;

    mockMvc
        .perform(post("/api/v1/vitals").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.fieldErrors.patientId").exists())
        .andExpect(jsonPath("$.fieldErrors.value").exists());
  }
}
