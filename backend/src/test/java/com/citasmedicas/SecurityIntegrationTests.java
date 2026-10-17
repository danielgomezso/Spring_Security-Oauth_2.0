package com.citasmedicas;

import com.citasmedicas.model.*;
import com.citasmedicas.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("h2") @Transactional
class SecurityIntegrationTests {
 @Autowired MockMvc mvc;
 @Autowired ObjectMapper json;
 @Autowired UsuarioRepository usuarios;
 @Autowired CitaMedicaRepository citas;
 @Autowired JwtEncoder encoder;
 String login(String username,String password) throws Exception {
  var response=mvc.perform(post("/auth/login").contentType("application/json")
   .content(json.writeValueAsString(java.util.Map.of("username",username,"password",password))))
   .andExpect(status().isOk()).andExpect(jsonPath("token_type").value("Bearer")).andReturn();
  return json.readTree(response.getResponse().getContentAsString()).get("access_token").asText();
 }
 @Test void publicAndMissingToken() throws Exception {
  mvc.perform(get("/publico/especialidades")).andExpect(status().isOk()).andExpect(jsonPath("$",hasSize(3)));
  mvc.perform(get("/api/citas/mis-citas")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/admin/citas")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/citas").contentType("application/json").content("{}" )).andExpect(status().isUnauthorized());
  mvc.perform(delete("/api/admin/citas/1")).andExpect(status().isUnauthorized());
 }
 @Test void invalidLoginAndTamperedToken() throws Exception {
  mvc.perform(post("/auth/login").contentType("application/json").content("{\"username\":\"paciente1\",\"password\":\"incorrecta\"}"))
   .andExpect(status().isUnauthorized());
  var token=login("paciente1","password123");
  var parts=token.split("\\.");
  var signature=parts[2];
  parts[2]=(signature.charAt(0)=='a'?"b":"a")+signature.substring(1);
  mvc.perform(get("/api/citas/mis-citas").header("Authorization","Bearer "+String.join(".",parts)))
   .andExpect(status().isUnauthorized());
 }
 @Test void expiredAndWrongIssuerRejected() throws Exception {
  var now=Instant.now();
  for (var claims:java.util.List.of(
   JwtClaimsSet.builder().issuer("citas-medicas-demo").subject("paciente1").issuedAt(now.minusSeconds(7200)).expiresAt(now.minusSeconds(3600)).claim("roles",java.util.List.of("ROLE_USER")).build(),
   JwtClaimsSet.builder().issuer("otro-emisor").subject("paciente1").issuedAt(now).expiresAt(now.plusSeconds(3600)).claim("roles",java.util.List.of("ROLE_USER")).build())) {
   var token=encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
   mvc.perform(get("/api/citas/mis-citas").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
  }
 }
 @Test void rolesAndPatientIsolation() throws Exception {
  var otro=usuarios.save(new Usuario("otro","hash-solo-test","otro@example.com","ROLE_USER"));
  citas.saveAndFlush(new CitaMedica("Dr. Luis Solano",LocalDate.now().plusDays(7),LocalTime.NOON,"Cita privada",otro));
  var user=login("paciente1","password123");
  mvc.perform(get("/api/citas/mis-citas").header("Authorization","Bearer "+user))
   .andExpect(status().isOk()).andExpect(jsonPath("$",hasSize(2)))
   .andExpect(jsonPath("$[*].paciente",everyItem(is("paciente1")))).andExpect(jsonPath("$[0].password").doesNotExist());
  mvc.perform(get("/api/admin/citas").header("Authorization","Bearer "+user)).andExpect(status().isForbidden());
  mvc.perform(delete("/api/admin/citas/1").header("Authorization","Bearer "+user)).andExpect(status().isForbidden());
  var admin=login("admin1","admin123");
  mvc.perform(get("/api/admin/citas").header("Authorization","Bearer "+admin)).andExpect(status().isOk()).andExpect(jsonPath("$",hasSize(3)));
  mvc.perform(get("/api/citas/mis-citas").header("Authorization","Bearer "+admin)).andExpect(status().isForbidden());
  mvc.perform(post("/api/citas").header("Authorization","Bearer "+admin).contentType("application/json").content("{}")).andExpect(status().isForbidden());
 }
 @Test void createAndDelete() throws Exception {
  var user=login("paciente1","password123");
  var body=json.writeValueAsString(java.util.Map.of("medicoNombre","Dra. Ana Mora","fecha",LocalDate.now().plusDays(5).toString(),"hora","14:00","motivo","Control","usuarioId",999));
  var result=mvc.perform(post("/api/citas").header("Authorization","Bearer "+user).contentType("application/json").content(body))
   .andExpect(status().isCreated()).andExpect(jsonPath("paciente").value("paciente1")).andReturn();
  var id=json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
  var admin=login("admin1","admin123");
  mvc.perform(delete("/api/admin/citas/"+id).header("Authorization","Bearer "+admin)).andExpect(status().isNoContent());
  mvc.perform(delete("/api/admin/citas/"+id).header("Authorization","Bearer "+admin)).andExpect(status().isNotFound());
 }
 @Test void duplicateSlotIsConflict() throws Exception {
  var body=json.writeValueAsString(java.util.Map.of("medicoNombre","Dra. Ana Mora","fecha",LocalDate.now().plusDays(1).toString(),"hora","09:00","motivo","Duplicada"));
  mvc.perform(post("/api/citas").header("Authorization","Bearer "+login("paciente1","password123"))
   .contentType("application/json").content(body)).andExpect(status().isConflict());
 }
 @Test void rejectsInvalidBody() throws Exception {
  mvc.perform(post("/api/citas").header("Authorization","Bearer "+login("paciente1","password123")).contentType("application/json").content("{}"))
   .andExpect(status().isBadRequest());
 }
 @Test void corsPreflight() throws Exception {
  mvc.perform(options("/api/citas").header("Origin","http://localhost:4200")
   .header("Access-Control-Request-Method","POST").header("Access-Control-Request-Headers","authorization,content-type"))
   .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","http://localhost:4200"));
 }
}
