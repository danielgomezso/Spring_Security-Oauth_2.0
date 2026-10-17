package com.citasmedicas.dto;
import jakarta.validation.constraints.*;
public record LoginRequest(@NotBlank @Size(max=50) String username, @NotBlank @Size(max=72) String password) {}
