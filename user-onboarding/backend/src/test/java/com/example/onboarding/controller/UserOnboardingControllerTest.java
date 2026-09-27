package com.example.onboarding.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserOnboardingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String validBody(String email) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "email", email,
                "fullName", "Ada Lovelace",
                "password", "s3cret-pass",
                "dateOfBirth", "1990-12-10",
                "termsAccepted", true));
    }

    @Test
    void onboardsUserAndReturnsStandardizedResponse() throws Exception {
        mockMvc.perform(post("/api/v1/users/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody("grace@example.com")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("ONB_201_USER_ONBOARDED"))
                .andExpect(jsonPath("$.data.email").value("grace@example.com"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void returnsConflictForDuplicateEmail() throws Exception {
        mockMvc.perform(post("/api/v1/users/onboarding")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody("dup@example.com")));

        mockMvc.perform(post("/api/v1/users/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody("DUP@example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("ONB_409_EMAIL_EXISTS"));
    }

    @Test
    void returnsBadRequestWithRedactedFieldErrors() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", "not-an-email",
                "fullName", "A",
                "password", "short",
                "dateOfBirth", "2999-01-01",
                "termsAccepted", false));

        mockMvc.perform(post("/api/v1/users/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ONB_400_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.data[?(@.field == 'email')]").exists())
                .andExpect(jsonPath("$.data[?(@.field == 'password')].rejectedValue")
                        .value("***REDACTED***"))
                .andExpect(jsonPath("$.data[?(@.field == 'termsAccepted')]").exists());
    }

    @Test
    void returnsBadRequestForMalformedBody() throws Exception {
        mockMvc.perform(post("/api/v1/users/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ONB_400_MALFORMED_BODY"));
    }

    @Test
    void findsOnboardedUserById() throws Exception {
        String created = mockMvc.perform(post("/api/v1/users/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody("byid@example.com")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(created).path("data").path("id").asText();

        mockMvc.perform(get("/api/v1/users/onboarding/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ONB_200_USER_FOUND"))
                .andExpect(jsonPath("$.data.id").value(id));
    }

    @Test
    void findsOnboardedUserByEmail() throws Exception {
        mockMvc.perform(post("/api/v1/users/onboarding")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody("byemail@example.com")));

        mockMvc.perform(get("/api/v1/users/onboarding").param("email", "byemail@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("byemail@example.com"));
    }

    @Test
    void returnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(get("/api/v1/users/onboarding/{id}",
                        java.util.UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ONB_404_USER_NOT_FOUND"));
    }

    @Test
    void rejectsInvalidEmailQueryParam() throws Exception {
        mockMvc.perform(get("/api/v1/users/onboarding").param("email", "nope"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(containsString("ONB_400")));
    }

    @Test
    void echoesSuppliedRequestIdHeader() throws Exception {
        mockMvc.perform(get("/api/v1/users/onboarding/{id}",
                        java.util.UUID.randomUUID())
                        .header("X-Request-Id", "test-request-id"))
                .andExpect(header().string("X-Request-Id", "test-request-id"));
    }
}
