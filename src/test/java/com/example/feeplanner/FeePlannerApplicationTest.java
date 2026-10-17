package com.example.feeplanner;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;


@SpringBootTest
@AutoConfigureMockMvc
class FeePlannerApplicationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void healthIsUp() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.diskSpace").value("UP"));
    }

    @Test
    void kubernetesProbesAnswer() throws Exception {
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
    }

    @Test
    void infoNamesTheApplicationAndTheInstance() throws Exception {
        mvc.perform(get("/api/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("fee-planner"))
                .andExpect(jsonPath("$.servedBy").isNotEmpty());
    }

    @Test
    void plansAFee() throws Exception {
        mvc.perform(get("/api/plans")
                        .param("amount", "45000")
                        .param("instalments", "3")
                        .param("frequency", "QUARTERLY")
                        .param("firstDueDate", "2026-11-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instalments").value(3))
                .andExpect(jsonPath("$.schedule.length()").value(3))
                .andExpect(jsonPath("$.schedule[0].dueDate").value("2026-11-01"))
                .andExpect(jsonPath("$.schedule[2].dueDate").value("2027-05-01"));
    }

    @Test
    void badInputIsA400WithAReason() throws Exception {
        mvc.perform(get("/api/plans").param("amount", "100").param("instalments", "13"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("instalments must be between 1 and 12"));
        mvc.perform(get("/api/plans").param("amount", "0"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/plans"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/plans").param("amount", "100").param("frequency", "WEEKLY"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void homepageIsServed() throws Exception {
        mvc.perform(get("/index.html")).andExpect(status().isOk());
    }
}
