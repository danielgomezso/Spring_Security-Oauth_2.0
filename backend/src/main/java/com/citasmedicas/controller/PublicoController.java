package com.citasmedicas.controller;
import com.citasmedicas.service.EspecialidadService;
import java.util.List;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/publico")
public class PublicoController {
 private final EspecialidadService service;
 public PublicoController(EspecialidadService service) { this.service=service; }
 @GetMapping("/especialidades")
 public List<EspecialidadService.Especialidad> especialidades() { return service.listar(); }
}
