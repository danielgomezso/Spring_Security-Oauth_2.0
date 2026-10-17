package com.citasmedicas.controller;
import com.citasmedicas.dto.CitaResponse;
import com.citasmedicas.service.CitaService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin/citas")
public class AdminController {
 private final CitaService service;
 public AdminController(CitaService service) { this.service=service; }
 @GetMapping public List<CitaResponse> todas() { return service.todas(); }
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
 public void eliminar(@PathVariable Long id) { service.eliminar(id); }
}
