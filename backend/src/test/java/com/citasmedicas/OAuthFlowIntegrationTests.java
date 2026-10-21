package com.citasmedicas;

import com.citasmedicas.repository.CitaMedicaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("h2") @Transactional
class OAuthFlowIntegrationTests {
    private static final String CALLBACK = "http://localhost:4200/oauth2/callback";
    private static final String VERIFIER = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired CitaMedicaRepository citas;

    private MockHttpSession login(String username, String password) throws Exception {
        return (MockHttpSession) mvc.perform(post("/login").with(csrf())
                .param("username", username).param("password", password))
                .andExpect(status().is3xxRedirection()).andReturn().getRequest().getSession(false);
    }
    private String authorize(MockHttpSession session, String verifier) throws Exception {
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
        var result = mvc.perform(get("/oauth2/authorize").session(session)
                .queryParam("response_type", "code").queryParam("client_id", "citas-frontend")
                .queryParam("redirect_uri", CALLBACK).queryParam("scope", "openid citas.read")
                .queryParam("state", "demo-state").queryParam("code_challenge", challenge)
                .queryParam("code_challenge_method", "S256"))
                .andExpect(status().is3xxRedirection()).andReturn();
        var params = UriComponentsBuilder.fromUriString(result.getResponse().getRedirectedUrl()).build().getQueryParams();
        assertThat(params.getFirst("state")).isEqualTo("demo-state");
        assertThat(params.getFirst("code")).isNotBlank();
        return params.getFirst("code");
    }
    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder exchange(String code, String verifier) {
        return post("/oauth2/token").param("grant_type", "authorization_code")
                .param("client_id", "citas-frontend").param("redirect_uri", CALLBACK)
                .param("code", code).param("code_verifier", verifier);
    }

    @Test void rsaKeySurvivesReload(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        var config = new com.citasmedicas.config.SecurityConfig();
        String path = dir.resolve("key.json").toString();
        var selector = new com.nimbusds.jose.jwk.JWKSelector(new com.nimbusds.jose.jwk.JWKMatcher.Builder().build());
        var first = config.jwkSource(path).get(selector, null).get(0);
        var second = config.jwkSource(path).get(selector, null).get(0);
        assertThat(second.toJSONString()).isEqualTo(first.toJSONString());
    }

    @Test void sessionNeverReplacesBearerToken() throws Exception {
        var patient = login("paciente1", "password123");
        mvc.perform(get("/api/citas/mis-citas").session(patient)).andExpect(status().isUnauthorized());
        var admin = login("admin1", "admin123");
        mvc.perform(get("/api/admin/citas").session(admin)).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/admin/citas/" + citas.findAll().get(0).getId()).session(admin))
                .andExpect(status().isUnauthorized());
    }
    @Test void loginAndLogoutRequireCsrf() throws Exception {
        mvc.perform(post("/login").param("username", "admin1").param("password", "admin123"))
                .andExpect(status().isForbidden());
        var session = login("paciente1", "password123");
        mvc.perform(post("/logout").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/logout").session(session).with(csrf())).andExpect(status().is3xxRedirection());
        assertThat(session.isInvalid()).isTrue();
    }
    @Test void pkceProducesUsableTokenWithoutRefreshToken() throws Exception {
        String code = authorize(login("paciente1", "password123"), VERIFIER);
        var result = mvc.perform(exchange(code, VERIFIER)).andExpect(status().isOk())
                .andExpect(jsonPath("id_token").isString())
                .andExpect(jsonPath("refresh_token").doesNotExist()).andReturn();
        String token = json.readTree(result.getResponse().getContentAsString()).get("access_token").asText();
        mvc.perform(get("/api/citas/mis-citas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(get("/api/admin/citas").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(exchange(code, VERIFIER)).andExpect(status().isBadRequest());
    }
    @Test void oidcLogoutClosesTheServerSession() throws Exception {
        var session = login("admin1", "admin123");
        String code = authorize(session, VERIFIER);
        var result = mvc.perform(exchange(code, VERIFIER)).andExpect(status().isOk()).andReturn();
        String idToken = json.readTree(result.getResponse().getContentAsString()).get("id_token").asText();
        mvc.perform(get("/connect/logout").session(session)
                .queryParam("id_token_hint", idToken)
                .queryParam("post_logout_redirect_uri", "http://localhost:4200/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:4200/"));
        assertThat(session.isInvalid()).isTrue();
    }

    @Test void rejectsIncorrectVerifierAndMissingPkce() throws Exception {
        String code = authorize(login("paciente1", "password123"), VERIFIER);
        mvc.perform(exchange(code, VERIFIER + "wrong")).andExpect(status().isBadRequest());
        var result = mvc.perform(get("/oauth2/authorize").session(login("paciente1", "password123"))
                .queryParam("response_type", "code").queryParam("client_id", "citas-frontend")
                .queryParam("redirect_uri", CALLBACK).queryParam("scope", "openid"))
                .andExpect(status().is3xxRedirection()).andReturn();
        assertThat(result.getResponse().getRedirectedUrl()).contains("error=").doesNotContain("code=");
    }
    @Test void clientCredentialsOnlyReadsSummary() throws Exception {
        var result = mvc.perform(post("/oauth2/token").with(httpBasic("citas-service", "secret123"))
                .param("grant_type", "client_credentials").param("scope", "citas.read"))
                .andExpect(status().isOk()).andReturn();
        String token = json.readTree(result.getResponse().getContentAsString()).get("access_token").asText();
        mvc.perform(get("/api/servicio/resumen").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("totalCitas").value(2));
        mvc.perform(get("/api/admin/citas").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        mvc.perform(get("/api/citas/mis-citas").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        mvc.perform(post("/oauth2/token").with(httpBasic("citas-service", "secret123"))
                .param("grant_type", "client_credentials").param("scope", "citas.write"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/servicio/resumen")).andExpect(status().isUnauthorized());
    }
}
