import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Cita, Especialidad, NuevaCita } from '../models/cita';
@Injectable({ providedIn: 'root' })
export class CitaService {
  constructor(private http: HttpClient) {}
  especialidades() {
    return this.http.get<Especialidad[]>('/publico/especialidades');
  }
  misCitas() {
    return this.http.get<Cita[]>('/api/citas/mis-citas');
  }
  todas() {
    return this.http.get<Cita[]>('/api/admin/citas');
  }
  crear(cita: NuevaCita) {
    return this.http.post<Cita>('/api/citas', cita);
  }
  eliminar(id: number) {
    return this.http.delete<void>(`/api/admin/citas/${id}`);
  }
}
export function mensajeError(error: HttpErrorResponse): string {
  if (error.status === 0) return 'No se pudo conectar con el backend. Verifica que esté iniciado.';
  if (error.status === 403) return 'Tu rol no tiene permiso para esta acción.';
  if (error.status === 401) return 'Credenciales incorrectas o sesión expirada.';
  return error.error?.message || 'No se pudo completar la solicitud. Intenta de nuevo.';
}
