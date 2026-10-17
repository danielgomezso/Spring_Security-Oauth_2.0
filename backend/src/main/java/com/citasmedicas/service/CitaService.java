package com.citasmedicas.service;
import com.citasmedicas.dto.*;
import com.citasmedicas.model.CitaMedica;
import com.citasmedicas.repository.*;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
@Service @Transactional
public class CitaService {
 private final CitaMedicaRepository citas;
 private final UsuarioRepository usuarios;
 private final EspecialidadService especialidades;
 public CitaService(CitaMedicaRepository citas,UsuarioRepository usuarios,EspecialidadService especialidades) {
  this.citas=citas; this.usuarios=usuarios; this.especialidades=especialidades;
 }
 @Transactional(readOnly=true)
 public List<CitaResponse> misCitas(String username) {
  return citas.findByUsuarioUsernameOrderByFechaAscHoraAsc(username).stream().map(CitaResponse::from).toList();
 }
 @Transactional(readOnly=true)
 public List<CitaResponse> todas() { return citas.findAllByOrderByFechaAscHoraAsc().stream().map(CitaResponse::from).toList(); }
 public CitaResponse crear(CitaRequest r,String username) {
  if(!LocalDateTime.of(r.fecha(),r.hora()).isAfter(LocalDateTime.now()))
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La cita debe estar en el futuro");
  if(especialidades.listar().stream().noneMatch(e->e.medicoNombre().equals(r.medicoNombre())))
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Médico no disponible");
  var usuario=usuarios.findByUsername(username).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Usuario no disponible"));
  return CitaResponse.from(citas.saveAndFlush(new CitaMedica(r.medicoNombre(),r.fecha(),r.hora(),r.motivo(),usuario)));
 }
 public void eliminar(Long id) {
  var cita=citas.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Cita no encontrada"));
  citas.delete(cita);
 }
}
