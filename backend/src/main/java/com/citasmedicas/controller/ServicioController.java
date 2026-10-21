package com.citasmedicas.controller;

import com.citasmedicas.repository.CitaMedicaRepository;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/** Recurso mínimo para demostrar Client Credentials sin exponer datos de pacientes. */
@RestController
@RequestMapping("/api/servicio")
public class ServicioController {
    private final CitaMedicaRepository citas;
    public ServicioController(CitaMedicaRepository citas) { this.citas = citas; }

    @GetMapping("/resumen")
    public Map<String, Long> resumen() { return Map.of("totalCitas", citas.count()); }
}
