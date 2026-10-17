import { Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CitaService, mensajeError } from '../../services/cita.service';
import { Especialidad } from '../../models/cita';
@Component({
  standalone: true,
  imports: [RouterLink],
  template: `
    <p class="eyebrow">ACCESO PÚBLICO · SIN TOKEN</p>
    <h1>Tu próxima cita, más cerca.</h1>
    <p class="intro">
      Consulta nuestros servicios e inicia sesión para reservar tu atención médica.
    </p>
    @if (error()) {
      <p class="error" role="alert">{{ error() }}</p>
    }
    @if (loading()) {
      <p role="status">Cargando especialidades…</p>
    }
    <div class="cards">
      @for (item of servicios(); track item.nombre) {
        <article class="card">
          <span class="badge">Consulta médica</span>
          <h2>{{ item.nombre }}</h2>
          <p>{{ item.medicoNombre }}</p>
        </article>
      }
    </div>
    <a class="button" routerLink="/login">Iniciar sesión</a>
    <aside>
      Demostración académica: esta página consulta <code>GET /publico/especialidades</code> sin
      autenticación.
    </aside>
  `,
})
export class Inicio {
  servicios = signal<Especialidad[]>([]);
  error = signal('');
  loading = signal(true);
  constructor(api: CitaService) {
    api.especialidades().subscribe({
      next: (value) => {
        this.servicios.set(value);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(mensajeError(err));
        this.loading.set(false);
      },
    });
  }
}
