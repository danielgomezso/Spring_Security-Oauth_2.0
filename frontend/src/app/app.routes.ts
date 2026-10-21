import { Routes } from '@angular/router';
import { roleGuard } from './guards/auth.guard';
import { Inicio } from './pages/inicio/inicio';
import { Callback } from './pages/login/callback';
import { Login } from './pages/login/login';
import { Paciente } from './pages/paciente/paciente';
import { Admin } from './pages/admin/admin';
export const routes: Routes = [
  { path: '', component: Inicio },
  { path: 'login', component: Login },
  { path: 'oauth2/callback', component: Callback },
  { path: 'paciente', component: Paciente, canActivate: [roleGuard], data: { role: 'ROLE_USER' } },
  { path: 'admin', component: Admin, canActivate: [roleGuard], data: { role: 'ROLE_ADMIN' } },
  { path: '**', redirectTo: '' },
];
