import { Component, signal } from '@angular/core';
import { CitaService, mensajeError } from '../../services/cita.service';
import { Cita } from '../../models/cita';
@Component({
  standalone: true,
  template: `
    <p class="eyebrow">ADMINISTRACIÓN · ROLE_ADMIN</p>
    <h1>Agenda de pacientes</h1>
    <p>Consulta todas las citas y elimina las reservas canceladas.</p>
    @if (error()) {
      <p class="error" role="alert">{{ error() }}</p>
    }
    @if (success()) {
      <p class="success" role="status">{{ success() }}</p>
    }
    @if (loading()) {
      <p role="status">Cargando citas…</p>
    } @else {
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Paciente</th>
              <th>Médico</th>
              <th>Fecha / hora</th>
              <th>Motivo</th>
              <th>Estado</th>
              <th>Acción</th>
            </tr>
          </thead>
          <tbody>
            @for (c of citas(); track c.id) {
              <tr>
                <td>{{ c.paciente }}</td>
                <td>{{ c.medicoNombre }}</td>
                <td>{{ c.fecha }} · {{ c.hora }}</td>
                <td>{{ c.motivo }}</td>
                <td>{{ c.estado }}</td>
                <td>
                  <button class="danger" [disabled]="busy()" (click)="eliminar(c.id)">
                    Eliminar
                  </button>
                </td>
              </tr>
            } @empty {
              <tr>
                <td colspan="6">No hay citas registradas.</td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    }
  `,
})
export class Admin {
  citas = signal<Cita[]>([]);
  error = signal('');
  success = signal('');
  loading = signal(true);
  busy = signal(false);
  constructor(private api: CitaService) {
    this.cargar();
  }
  cargar() {
    this.api.todas().subscribe({
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
  eliminar(id: number) {
    if (!confirm('¿Eliminar esta cita de forma definitiva?')) return;
    this.busy.set(true);
    this.error.set('');
    this.success.set('');
    this.api.eliminar(id).subscribe({
      next: () => {
        this.busy.set(false);
        this.success.set('Cita eliminada.');
        this.cargar();
      },
      error: (err) => {
        this.busy.set(false);
        this.error.set(mensajeError(err));
      },
    });
  }
}
