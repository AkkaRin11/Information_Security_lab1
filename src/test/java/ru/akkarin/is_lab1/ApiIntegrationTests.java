package ru.akkarin.is_lab1;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:apitest;DB_CLOSE_DELAY=-1",
        "app.jwt-secret=test-only-secret-with-at-least-32-bytes-long",
        "app.demo.username=student",
        "app.demo.password=strong-password-for-tests"
})
class ApiIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void requiresValidToken() throws Exception {
        mvc.perform(get("/api/data")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/data").contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"hello\"}")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/data").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginRejectsWrongPasswordAndSqlInjection() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"student\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"' OR 1=1 --\",\"password\":\"anything\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanCreateAndReadEscapedNote() throws Exception {
        String loginResponse = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"student\",\"password\":\"strong-password-for-tests\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode login = mapper.readTree(loginResponse);
        String bearer = "Bearer " + login.get("accessToken").asText();

        mvc.perform(post("/api/data").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"<script>alert(1)</script>\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content", containsString("&lt;script&gt;")));
        mvc.perform(get("/api/data").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content", containsString("&lt;script&gt;")))
                .andExpect(jsonPath("$[0].content", not(containsString("<script>"))));
    }
}
