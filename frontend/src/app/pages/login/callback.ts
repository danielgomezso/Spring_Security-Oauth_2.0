import { Component, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  standalone: true,
  imports: [RouterLink],
  template: `
    <h1>Completando el acceso</h1>
    @if (error()) {
      <p class="error" role="alert">{{ error() }}</p>
      <a routerLink="/login">Volver al inicio de sesión</a>
    } @else {
      <p role="status">Validando tu acceso…</p>
    }
  `,
})
export class Callback {
  error = signal('');
  constructor(auth: AuthService, router: Router) {
    const params = new URLSearchParams(window.location.search);
    // Retirar el código de la barra y del historial antes de intercambiarlo.
    window.history.replaceState(window.history.state, '', window.location.pathname);
    auth.completeLogin(params).subscribe({
      next: () =>
        void router.navigateByUrl(auth.hasRole('ROLE_ADMIN') ? '/admin' : '/paciente', {
          replaceUrl: true,
        }),
      error: () => {
        auth.clearSession();
        this.error.set('No se pudo completar el acceso. Inicia sesión nuevamente.');
      },
    });
  }
}
