import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { webcrypto } from 'node:crypto';
import { vi } from 'vitest';
import { AuthService } from './auth.service';
import { mockStorage } from '../testing/storage';

describe('OAuth con PKCE', () => {
  let auth: AuthService;
  let http: HttpTestingController;
  beforeEach(() => {
    mockStorage();
    vi.stubGlobal('crypto', webcrypto);
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => {
    http.verify();
    vi.unstubAllGlobals();
  });

  it('genera challenge SHA-256 y un state nuevo por intento', async () => {
    const url = new URL(await auth.authorizationUrl());
    const saved = JSON.parse(sessionStorage.getItem('oauth_request')!);
    const digest = await webcrypto.subtle.digest(
      'SHA-256',
      new TextEncoder().encode(saved.verifier),
    );
    const expected = btoa(String.fromCharCode(...new Uint8Array(digest)))
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '');
    expect(url.searchParams.get('code_challenge')).toBe(expected);
    expect(url.searchParams.get('state')).toBe(saved.state);
    expect(url.searchParams.get('code_challenge_method')).toBe('S256');
    expect(url.searchParams.has('code_verifier')).toBe(false);
    expect(url.searchParams.has('client_secret')).toBe(false);
    const second = new URL(await auth.authorizationUrl());
    expect(second.searchParams.get('state')).not.toBe(saved.state);
  });

  it('rechaza un state incorrecto sin llamar al token endpoint', async () => {
    await auth.authorizationUrl();
    const error = vi.fn();
    auth
      .completeLogin(new URLSearchParams({ state: 'incorrecto', code: 'abc' }))
      .subscribe({ error });
    expect(error).toHaveBeenCalled();
    expect(sessionStorage.getItem('oauth_request')).toBeNull();
    http.expectNone('http://localhost:8080/oauth2/token');
  });

  it('rechaza solicitudes vencidas y errores del servidor', async () => {
    for (const expired of [true, false]) {
      await auth.authorizationUrl();
      const saved = JSON.parse(sessionStorage.getItem('oauth_request')!);
      if (expired) saved.created = Date.now() - 700000;
      sessionStorage.setItem('oauth_request', JSON.stringify(saved));
      const params = new URLSearchParams({ state: saved.state, code: 'abc' });
      if (!expired) params.set('error', 'access_denied');
      const error = vi.fn();
      auth.completeLogin(params).subscribe({ error });
      expect(error).toHaveBeenCalled();
    }
    http.expectNone('http://localhost:8080/oauth2/token');
  });

  it('intercambia el código una sola vez y restaura el usuario desde el access token', async () => {
    await auth.authorizationUrl();
    const saved = JSON.parse(sessionStorage.getItem('oauth_request')!);
    const params = new URLSearchParams({ state: saved.state, code: 'codigo-demo' });
    auth.completeLogin(params).subscribe();
    const req = http.expectOne('http://localhost:8080/oauth2/token');
    expect(req.request.body.get('code_verifier')).toBe(saved.verifier);
    expect(req.request.body.get('grant_type')).toBe('authorization_code');
    expect(req.request.body.has('client_secret')).toBe(false);
    const claims = btoa(
      JSON.stringify({
        username: 'paciente1',
        roles: ['ROLE_USER'],
        exp: Math.floor(Date.now() / 1000) + 3600,
      }),
    );
    req.flush({ access_token: `header.${claims}.signature`, id_token: 'id-token-demo' });
    expect(auth.hasRole('ROLE_USER')).toBe(true);
    expect(localStorage.getItem('id_token')).toBe('id-token-demo');
    const error = vi.fn();
    auth.completeLogin(params).subscribe({ error });
    expect(error).toHaveBeenCalled();
    http.expectNone('http://localhost:8080/oauth2/token');
    auth.clearSession();
    expect(auth.session()).toBeNull();
    expect(localStorage.getItem('id_token')).toBeNull();
  });
});
