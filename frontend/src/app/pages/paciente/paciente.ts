import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CitaService, mensajeError } from '../../services/cita.service';
import { Cita, Especialidad, NuevaCita } from '../../models/cita';
@Component({ standalone: true, imports: [FormsModule], templateUrl: './paciente.html' })
export class Paciente {
  citas = signal<Cita[]>([]);
  especialidades = signal<Especialidad[]>([]);
  error = signal('');
  success = signal('');
  busy = signal(false);
  loading = signal(true);
  nueva: NuevaCita = { medicoNombre: '', fecha: '', hora: '', motivo: '' };
  constructor(private api: CitaService) {
    this.cargar();
    api
      .especialidades()
      .subscribe({
        next: (data) => this.especialidades.set(data),
        error: (err) => this.error.set(mensajeError(err)),
      });
  }
  cargar() {
    this.loading.set(true);
    this.api.misCitas().subscribe({
      next: (data) => {
        this.citas.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(mensajeError(err));
        this.loading.set(false);
      },
    });
  }
  agendar() {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    this.success.set('');
    this.api.crear(this.nueva).subscribe({
      next: () => {
        this.busy.set(false);
        this.success.set('Cita agendada correctamente.');
        this.nueva = { medicoNombre: '', fecha: '', hora: '', motivo: '' };
        this.cargar();
      },
      error: (err) => {
        this.busy.set(false);
        this.error.set(mensajeError(err));
      },
    });
  }
}
