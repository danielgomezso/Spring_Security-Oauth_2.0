package com.citasmedicas.model;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity @Table(name="citas_medicas")
public class CitaMedica {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="medico_nombre", nullable=false, length=100) private String medicoNombre;
 @Column(nullable=false) private LocalDate fecha;
 @Column(nullable=false) private LocalTime hora;
 @Column(nullable=false, length=500) private String motivo;
 @Column(nullable=false, length=20) private String estado;
 @ManyToOne(fetch=FetchType.LAZY, optional=false)
 @JoinColumn(name="usuario_id", nullable=false) private Usuario usuario;
 protected CitaMedica() {}
 public CitaMedica(String medicoNombre, LocalDate fecha, LocalTime hora, String motivo, Usuario usuario) {
  this.medicoNombre=medicoNombre; this.fecha=fecha; this.hora=hora;
  this.motivo=motivo; this.usuario=usuario; this.estado="PENDIENTE";
 }
 public Long getId() { return id; }
 public String getMedicoNombre() { return medicoNombre; }
 public LocalDate getFecha() { return fecha; }
 public LocalTime getHora() { return hora; }
 public String getMotivo() { return motivo; }
 public String getEstado() { return estado; }
 public Usuario getUsuario() { return usuario; }
}
