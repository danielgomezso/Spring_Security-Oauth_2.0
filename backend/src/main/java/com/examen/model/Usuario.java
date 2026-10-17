package com.examen.model;
import jakarta.persistence.*;

/** Se guarda un hash BCrypt; esta entidad nunca se serializa en la API. */
@Entity @Table(name = "usuarios")
public class Usuario {
 @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
 @Column(nullable=false, unique=true, length=50) private String username;
 @Column(nullable=false, length=100) private String password;
 @Column(nullable=false, unique=true, length=150) private String email;
 @Column(nullable=false, length=20) private String rol;
 protected Usuario() {}
 public Usuario(String username, String password, String email, String rol) {
  this.username=username; this.password=password; this.email=email; this.rol=rol;
 }
 public Long getId() { return id; }
 public String getUsername() { return username; }
 public String getPassword() { return password; }
 public String getEmail() { return email; }
 public String getRol() { return rol; }
}
