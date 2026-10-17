package com.citasmedicas.config;
import com.citasmedicas.model.*;
import com.citasmedicas.repository.*;
import java.time.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
@Component @ConditionalOnProperty(name="app.demo-data",havingValue="true")
public class DataInitializer implements CommandLineRunner {
 private final UsuarioRepository usuarios;
 private final CitaMedicaRepository citas;
 private final BCryptPasswordEncoder passwords;
 public DataInitializer(UsuarioRepository usuarios,CitaMedicaRepository citas,BCryptPasswordEncoder passwords) {
  this.usuarios=usuarios; this.citas=citas; this.passwords=passwords;
 }
 @Override @Transactional
 public void run(String... args) {
  // En una BD persistente no sobrescribe contraseñas ni recrea citas eliminadas.
  boolean nuevo=usuarios.findByUsername("paciente1").isEmpty();
  var paciente=usuarios.findByUsername("paciente1").orElseGet(()->usuarios.save(
   new Usuario("paciente1",passwords.encode("password123"),"paciente1@example.com","ROLE_USER")));
  if(usuarios.findByUsername("admin1").isEmpty()) usuarios.save(
   new Usuario("admin1",passwords.encode("admin123"),"admin1@example.com","ROLE_ADMIN"));
  if(nuevo) {
   citas.save(new CitaMedica("Dra. Ana Mora",LocalDate.now().plusDays(1),LocalTime.of(9,0),"Control general",paciente));
   citas.save(new CitaMedica("Dra. Elena Castro",LocalDate.now().plusDays(2),LocalTime.of(10,0),"Seguimiento",paciente));
  }
 }
}
