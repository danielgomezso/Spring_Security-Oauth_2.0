import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(
    private auth: AuthService,
    private router: Router,
  ) {}
  intercept(request: HttpRequest<unknown>, next: HttpHandler) {
    // Solo la API propia recibe el token. Servicios públicos y login salen sin Bearer.
    const protectedApi = request.url.startsWith('/api/');
    if (protectedApi && this.auth.token) {
      request = request.clone({ setHeaders: { Authorization: `Bearer ${this.auth.token}` } });
    }
    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        if (protectedApi && error.status === 401) {
          this.auth.logout();
          void this.router.navigate(['/login'], { queryParams: { expired: '1' } });
        }
        return throwError(() => error);
      }),
    );
  }
}
