export interface Especialidad {
  nombre: string;
  medicoNombre: string;
}
export interface NuevaCita {
  medicoNombre: string;
  fecha: string;
  hora: string;
  motivo: string;
}
export interface Cita extends NuevaCita {
  id: number;
  estado: string;
  paciente: string;
}
export interface LoginResponse {
  access_token: string;
  token_type: string;
  expires_in: number;
  username: string;
  roles: string[];
}
