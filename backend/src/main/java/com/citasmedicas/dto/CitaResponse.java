package com.citasmedicas.dto;
import com.citasmedicas.model.CitaMedica;
import java.time.*;
public record CitaResponse(Long id, String medicoNombre, LocalDate fecha, LocalTime hora,
 String motivo, String estado, String paciente) {
 public static CitaResponse from(CitaMedica c) {
  return new CitaResponse(c.getId(),c.getMedicoNombre(),c.getFecha(),c.getHora(),c.getMotivo(),c.getEstado(),c.getUsuario().getUsername());
 }
}
