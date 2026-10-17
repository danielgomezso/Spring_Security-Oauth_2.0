package com.examen.config;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.env.*;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;

@Configuration
public class SecurityConfig {
 @Bean BCryptPasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
 @Bean SecretKey jwtKey(@Value("${app.jwt.secret}") String secret) {
  var bytes=secret.getBytes(StandardCharsets.UTF_8);
  if(bytes.length<32) throw new IllegalArgumentException("JWT_SECRET necesita al menos 32 bytes");
  return new SecretKeySpec(bytes,"HmacSHA256");
 }
 @Bean JwtEncoder jwtEncoder(SecretKey key) { return new NimbusJwtEncoder(new ImmutableSecret<>(key)); }
 @Bean JwtDecoder jwtDecoder(SecretKey key, @Value("${app.jwt.issuer}") String issuer) {
  var decoder=NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
  // Valida firma, expiración y emisor. Decodificar un JWT no equivale a validarlo.
  decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
  return decoder;
 }
 @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, Environment env) throws Exception {
  var roles=new JwtGrantedAuthoritiesConverter();
  roles.setAuthoritiesClaimName("roles");
  roles.setAuthorityPrefix(""); // El claim ya contiene ROLE_USER o ROLE_ADMIN.
  var converter=new JwtAuthenticationConverter();
  converter.setJwtGrantedAuthoritiesConverter(roles);
  http.cors(Customizer.withDefaults())
   .csrf(csrf->csrf.disable()) // API stateless con Bearer, sin autenticación por cookies.
   .sessionManagement(session->session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .authorizeHttpRequests(auth->{
    auth.requestMatchers("/publico/**","/auth/**").permitAll();
    if(env.acceptsProfiles(Profiles.of("h2"))) auth.requestMatchers("/h2-console/**").permitAll();
    auth.requestMatchers("/api/citas","/api/citas/**").hasRole("USER")
     .requestMatchers("/api/admin/**").hasRole("ADMIN").anyRequest().denyAll();
   })
   .oauth2ResourceServer(oauth->oauth.jwt(jwt->jwt.jwtAuthenticationConverter(converter)));
  if(env.acceptsProfiles(Profiles.of("h2"))) http.headers(h->h.frameOptions(f->f.sameOrigin()));
  return http.build();
 }
 @Bean CorsConfigurationSource corsConfigurationSource(@Value("${app.cors-origin}") String origin) {
  var config=new CorsConfiguration();
  config.setAllowedOrigins(List.of(origin));
  config.setAllowedMethods(List.of("GET","POST","DELETE","OPTIONS"));
  config.setAllowedHeaders(List.of("Authorization","Content-Type"));
  var source=new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/**",config);
  return source;
 }
}
