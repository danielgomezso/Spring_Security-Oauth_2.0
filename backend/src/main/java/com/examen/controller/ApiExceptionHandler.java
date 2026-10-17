package com.examen.controller;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestControllerAdvice
public class ApiExceptionHandler {
 @ExceptionHandler(ResponseStatusException.class)
 ResponseEntity<?> status(ResponseStatusException ex) {
  return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message",ex.getReason()==null?"Solicitud rechazada":ex.getReason()));
 }
 @ExceptionHandler({MethodArgumentNotValidException.class,HttpMessageNotReadableException.class})
 ResponseEntity<?> validation(Exception ex) {
  return ResponseEntity.badRequest().body(Map.of("message","Verifica los campos y el formato de fecha/hora"));
 }
 @ExceptionHandler(DataIntegrityViolationException.class)
 ResponseEntity<?> conflict(DataIntegrityViolationException ex) {
  return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message","El médico ya tiene una cita en ese horario"));
 }
}
