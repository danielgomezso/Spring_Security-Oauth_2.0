package com.citasmedicas;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import com.citasmedicas.model.CitaMedica;
import com.citasmedicas.model.Usuario;
import com.citasmedicas.repository.CitaMedicaRepository;
import com.citasmedicas.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
class SecurityIntegrationTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private CitaMedicaRepository citas;

    @Autowired
    private JwtEncoder encoder;

    @Value("${app.oauth2.issuer:http://localhost:8080}")
    private String issuer;

    private String login(String username, String password) throws Exception {
        var response = mvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(json.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("token_type").value("Bearer"))
                .andReturn();
        return json.readTree(response.getResponse().getContentAsString()).get("access_token").asText();
    }

    @Test
    void oauth2OpenIdConfigurationEndpoint() throws Exception {
        mvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("issuer").value(issuer))
                .andExpect(jsonPath("jwks_uri").value(issuer + "/oauth2/jwks"))
                .andExpect(jsonPath("token_endpoint").value(issuer + "/oauth2/token"));
    }

    @Test
    void oauth2JwksEndpoint() throws Exception {
        mvc.perform(get("/oauth2/jwks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("keys", hasSize(1)))
                .andExpect(jsonPath("keys[0].kty").value("RSA"))
                .andExpect(jsonPath("keys[0].n", notNullValue()))
                .andExpect(jsonPath("keys[0].e", notNullValue()));
    }

    @Test
    void oauth2ClientCredentialsToken() throws Exception {
        mvc.perform(post("/oauth2/token")
                        .with(httpBasic("citas-service", "secret123"))
                        .param("grant_type", "client_credentials")
                        .param("scope", "citas.read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("token_type").value("Bearer"))
                .andExpect(jsonPath("access_token", notNullValue()))
                .andExpect(jsonPath("expires_in", notNullValue()));
    }

    @Test
    void publicAndMissingToken() throws Exception {
        mvc.perform(get("/publico/especialidades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        mvc.perform(get("/api/citas/mis-citas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/citas")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/citas").contentType("application/json").content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/admin/citas/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidLoginAndTamperedToken() throws Exception {
        mvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"paciente1\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized());

        var token = login("paciente1", "password123");
        var parts = token.split("\\.");
        var signature = parts[2];
        parts[2] = (signature.charAt(0) == 'a' ? "b" : "a") + signature.substring(1);

        mvc.perform(get("/api/citas/mis-citas")
                        .header("Authorization", "Bearer " + String.join(".", parts)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredAndWrongIssuerRejected() throws Exception {
        var now = Instant.now();
        for (var claims : List.of(
                JwtClaimsSet.builder()
                        .issuer(issuer)
                        .subject("paciente1")
                        .issuedAt(now.minusSeconds(7200))
                        .expiresAt(now.minusSeconds(3600))
                        .claim("roles", List.of("ROLE_USER"))
                        .build(),
                JwtClaimsSet.builder()
                        .issuer("otro-emisor-no-valido")
                        .subject("paciente1")
                        .issuedAt(now)
                        .expiresAt(now.plusSeconds(3600))
                        .claim("roles", List.of("ROLE_USER"))
                        .build())) {

            var token = encoder.encode(
                    JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims)
            ).getTokenValue();

            mvc.perform(get("/api/citas/mis-citas")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void rolesAndPatientIsolation() throws Exception {
        var otro = usuarios.save(new Usuario("otro", "hash-solo-test", "otro@example.com", "ROLE_USER"));
        citas.saveAndFlush(new CitaMedica("Dr. Luis Solano", LocalDate.now().plusDays(7), LocalTime.NOON, "Cita privada", otro));

        var user = login("paciente1", "password123");
        mvc.perform(get("/api/citas/mis-citas").header("Authorization", "Bearer " + user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].paciente", everyItem(is("paciente1"))))
                .andExpect(jsonPath("$[0].password").doesNotExist());

        mvc.perform(get("/api/admin/citas").header("Authorization", "Bearer " + user))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/admin/citas/1").header("Authorization", "Bearer " + user))
                .andExpect(status().isForbidden());

        var admin = login("admin1", "admin123");
        mvc.perform(get("/api/admin/citas").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        mvc.perform(get("/api/citas/mis-citas").header("Authorization", "Bearer " + admin))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/citas").header("Authorization", "Bearer " + admin)
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createAndDelete() throws Exception {
        var user = login("paciente1", "password123");
        var body = json.writeValueAsString(Map.of(
                "medicoNombre", "Dra. Ana Mora",
                "fecha", LocalDate.now().plusDays(5).toString(),
                "hora", "14:00",
                "motivo", "Control",
                "usuarioId", 999
        ));

        var result = mvc.perform(post("/api/citas")
                        .header("Authorization", "Bearer " + user)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("paciente").value("paciente1"))
                .andReturn();

        var id = json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        var admin = login("admin1", "admin123");

        mvc.perform(delete("/api/admin/citas/" + id).header("Authorization", "Bearer " + admin))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/admin/citas/" + id).header("Authorization", "Bearer " + admin))
                .andExpect(status().isNotFound());
    }

    @Test
    void duplicateSlotIsConflict() throws Exception {
        var body = json.writeValueAsString(Map.of(
                "medicoNombre", "Dra. Ana Mora",
                "fecha", LocalDate.now().plusDays(1).toString(),
                "hora", "09:00",
                "motivo", "Duplicada"
        ));

        mvc.perform(post("/api/citas")
                        .header("Authorization", "Bearer " + login("paciente1", "password123"))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsInvalidBody() throws Exception {
        mvc.perform(post("/api/citas")
                        .header("Authorization", "Bearer " + login("paciente1", "password123"))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void corsPreflight() throws Exception {
        mvc.perform(options("/api/citas")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }
}
