import { mockStorage } from '../testing/storage';
import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import {
  HttpClient,
  HTTP_INTERCEPTORS,
  provideHttpClient,
  withInterceptorsFromDi,
} from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AuthInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth.service';
describe('AuthInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  beforeEach(() => {
    mockStorage();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    // Instanciar antes de guardar el token ficticio para no restaurar claims de prueba.
    TestBed.inject(AuthService);
    localStorage.setItem('access_token', 'token-de-prueba');
  });
  afterEach(() => {
    backend?.verify();
    vi.unstubAllGlobals();
  });
  it('adjunta Bearer únicamente a rutas privadas propias', () => {
    for (const url of [
      '/api/citas/mis-citas',
      '/publico/especialidades',
      '/auth/login',
      'https://example.com/api/citas',
    ]) {
      http.get(url).subscribe();
      const request = backend.expectOne(url);
      expect(request.request.headers.get('Authorization')).toBe(
        url.startsWith('/api/') ? 'Bearer token-de-prueba' : null,
      );
      request.flush([]);
    }
  });
});
