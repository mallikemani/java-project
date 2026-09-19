package com.mallikraja.releasetracker;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mallikraja.releasetracker.release.ReleaseStore;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.environment=test")
@AutoConfigureMockMvc
class ReleaseTrackerApiTest {
    private static final String VALID_JSON = """
            {"applicationName":"payments-api","version":"1.0.0-SNAPSHOT",
             "environment":"DEV","status":"DEPLOYED"}
            """;

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ReleaseStore store;

    @BeforeEach
    void resetStore() {
        store.findAll(null).forEach(record -> store.delete(record.id()));
    }

    @Test
    void rootShowsApplicationIdentityAndRuntimeEnvironment() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application").value("release-tracker"))
                .andExpect(jsonPath("$.environment").value("test"));
    }

    @Test
    void healthIsUpWithoutExposingDetails() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void noReleasesReturnsAnEmptyArray() throws Exception {
        mvc.perform(get("/api/releases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void createsAndRetrievesARelease() throws Exception {
        String body = mvc.perform(post("/api/releases")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.applicationName").value("payments-api"))
                .andExpect(jsonPath("$.status").value("DEPLOYED"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(body).get("id").asText();
        mvc.perform(get("/api/releases/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void invalidNameReturns400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/releases").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON.replace("payments-api", "bad name")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void missingFieldsReturn400() throws Exception {
        mvc.perform(post("/api/releases").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownEnumReturns400() throws Exception {
        mvc.perform(post("/api/releases").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON.replace("DEV", "UNKNOWN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mvc.perform(post("/api/releases").contentType(MediaType.APPLICATION_JSON).content("{broken"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingReleaseReturns404() throws Exception {
        mvc.perform(get("/api/releases/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void invalidIdReturns400() throws Exception {
        mvc.perform(get("/api/releases/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void queryFiltersByEnvironment() throws Exception {
        mvc.perform(post("/api/releases").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/releases").param("environment", "QA"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/releases").param("environment", "DEV"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void deleteRemovesTheRelease() throws Exception {
        String body = mvc.perform(post("/api/releases").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(body).get("id").asText();
        mvc.perform(delete("/api/releases/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/releases/{id}", id)).andExpect(status().isNotFound());
    }
}
