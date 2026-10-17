import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs';
import { LoginResponse } from '../models/cita';

@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly session = signal<{ username: string; roles: string[] } | null>(this.restore());
  constructor(private http: HttpClient) {}
  get token(): string | null {
    return localStorage.getItem('access_token');
  }
  // Estos claims solo controlan la interfaz. El backend valida firma y permisos.
  private restore() {
    try {
      const token = localStorage.getItem('access_token');
      if (!token) return null;
      const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
      if (payload.exp * 1000 <= Date.now() || !Array.isArray(payload.roles))
        throw new Error('Token expirado');
      return { username: payload.username as string, roles: payload.roles as string[] };
    } catch {
      localStorage.removeItem('access_token');
      return null;
    }
  }
  login(username: string, password: string) {
    return this.http.post<LoginResponse>('/auth/login', { username, password }).pipe(
      tap((response) => {
        localStorage.setItem('access_token', response.access_token);
        this.session.set({ username: response.username, roles: response.roles });
      }),
    );
  }
  hasRole(role: string): boolean {
    return this.session()?.roles.includes(role) ?? false;
  }
  logout() {
    localStorage.removeItem('access_token');
    this.session.set(null);
  }
}
