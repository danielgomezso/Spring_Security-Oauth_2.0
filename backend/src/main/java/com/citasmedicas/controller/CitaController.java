package com.citasmedicas.controller;
import com.citasmedicas.dto.*;
import com.citasmedicas.service.CitaService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/citas")
public class CitaController {
 private final CitaService service;
 public CitaController(CitaService service) { this.service=service; }
 @GetMapping("/mis-citas")
 public List<CitaResponse> misCitas(@AuthenticationPrincipal Jwt jwt) { return service.misCitas(jwt.getSubject()); }
 @PostMapping @ResponseStatus(HttpStatus.CREATED)
 public CitaResponse crear(@Valid @RequestBody CitaRequest request,@AuthenticationPrincipal Jwt jwt) {
  return service.crear(request,jwt.getSubject());
 }
}
