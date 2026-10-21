import { Component, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  standalone: true,
  template: `
    <p class="eyebrow">INICIAR SESIÓN</p>
    <h1>Bienvenido de nuevo</h1>
    <p>Continúa al formulario de acceso para ingresar con tu cuenta.</p>
    @if (expired) {
      <p role="status">Tu sesión expiró. Inicia sesión nuevamente.</p>
    }
    <div class="card narrow">
      @if (error()) {
        <p class="error" role="alert">{{ error() }}</p>
      }
      <button [disabled]="busy()" (click)="login()">
        {{ busy() ? 'Redirigiendo…' : 'Iniciar sesión' }}
      </button>
    </div>
    <aside>
      Paciente: <code>paciente1 / password123</code><br />Administrador:
      <code>admin1 / admin123</code>
    </aside>
  `,
})
export class Login {
  error = signal('');
  busy = signal(false);
  expired: boolean;
  constructor(
    private auth: AuthService,
    route: ActivatedRoute,
  ) {
    this.expired = route.snapshot.queryParamMap.has('expired');
  }
  async login() {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    try {
      window.location.assign(await this.auth.authorizationUrl());
    } catch {
      this.error.set('No se pudo iniciar el acceso. Intenta nuevamente.');
      this.busy.set(false);
    }
  }
}
