package com.citasmedicas.dto;
import jakarta.validation.constraints.*;
import java.time.*;
// El paciente se obtiene del JWT, nunca de un usuarioId enviado por el navegador.
public record CitaRequest(@NotBlank @Size(max=100) String medicoNombre,
 @NotNull @FutureOrPresent LocalDate fecha, @NotNull LocalTime hora,
 @NotBlank @Size(max=500) String motivo) {}
