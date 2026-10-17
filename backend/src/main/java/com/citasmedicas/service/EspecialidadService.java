package com.citasmedicas.service;
import java.util.List;
import org.springframework.stereotype.Service;
@Service
public class EspecialidadService {
 public record Especialidad(String nombre,String medicoNombre) {}
 public List<Especialidad> listar() { return List.of(
  new Especialidad("Medicina general","Dra. Ana Mora"),
  new Especialidad("Pediatría","Dr. Luis Solano"),
  new Especialidad("Cardiología","Dra. Elena Castro")); }
}
