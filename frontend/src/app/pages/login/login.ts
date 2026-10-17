import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { mensajeError } from '../../services/cita.service';
@Component({
  standalone: true,
  imports: [FormsModule],
  template: `
    <p class="eyebrow">AUTENTICACIÓN</p>
    <h1>Bienvenido de nuevo</h1>
    <p>Ingresa con una de las cuentas de demostración.</p>
    @if (expired) {
      <p role="status">Tu sesión expiró. Inicia sesión nuevamente.</p>
    }
    <form #form="ngForm" (ngSubmit)="login()" class="card narrow">
      <label for="username">Usuario</label
      ><input
        id="username"
        name="username"
        [(ngModel)]="username"
        required
        maxlength="50"
        autocomplete="username"
      />
      <label for="password">Contraseña</label
      ><input
        id="password"
        name="password"
        type="password"
        [(ngModel)]="password"
        required
        maxlength="72"
        autocomplete="current-password"
      />
      @if (error()) {
        <p class="error" role="alert">{{ error() }}</p>
      }
      <button [disabled]="form.invalid || busy()">{{ busy() ? 'Ingresando…' : 'Ingresar' }}</button>
    </form>
    <aside>
      Paciente: <code>paciente1 / password123</code><br />Administrador:
      <code>admin1 / admin123</code>
    </aside>
  `,
})
export class Login {
  username = '';
  password = '';
  error = signal('');
  busy = signal(false);
  expired: boolean;
  constructor(
    private auth: AuthService,
    private router: Router,
    route: ActivatedRoute,
  ) {
    this.expired = route.snapshot.queryParamMap.has('expired');
  }
  login() {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    this.auth.login(this.username, this.password).subscribe({
      next: () => {
        this.busy.set(false);
        this.password = '';
        void this.router.navigate([this.auth.hasRole('ROLE_ADMIN') ? '/admin' : '/paciente']);
      },
      error: (err) => {
        this.busy.set(false);
        this.error.set(mensajeError(err));
      },
    });
  }
}
