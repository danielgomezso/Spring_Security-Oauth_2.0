package com.examen.controller;
import com.examen.dto.LoginRequest;
import com.examen.repository.UsuarioRepository;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/auth")
public class AuthController {
 private final UsuarioRepository usuarios;
 private final BCryptPasswordEncoder passwords;
 private final JwtEncoder encoder;
 private final String issuer;
 private final String dummyHash;
 public AuthController(UsuarioRepository usuarios, BCryptPasswordEncoder passwords, JwtEncoder encoder,
  @Value("${app.jwt.issuer}") String issuer) {
  this.usuarios=usuarios; this.passwords=passwords; this.encoder=encoder; this.issuer=issuer;
  this.dummyHash=passwords.encode("usuario-inexistente");
 }
 @PostMapping("/login")
 public Map<String,Object> login(@Valid @RequestBody LoginRequest request) {
  var usuario=usuarios.findByUsername(request.username()).orElse(null);
  boolean valid=passwords.matches(request.password(),usuario==null?dummyHash:usuario.getPassword());
  if(!valid || usuario==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Credenciales incorrectas");
  var now=Instant.now();
  var claims=JwtClaimsSet.builder().issuer(issuer).subject(usuario.getUsername()).issuedAt(now)
   .expiresAt(now.plusSeconds(3600)).claim("username",usuario.getUsername())
   .claim("roles",List.of(usuario.getRol())).build();
  var token=encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims));
  return Map.of("access_token",token.getTokenValue(),"token_type","Bearer","expires_in",3600,
   "username",usuario.getUsername(),"roles",List.of(usuario.getRol()));
 }
}
