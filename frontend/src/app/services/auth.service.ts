import { Injectable, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { tap, throwError } from 'rxjs';

interface TokenResponse {
  access_token: string;
  id_token?: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  // URLs de la demostración local. El navegador entra directamente al servidor OAuth.
  private readonly issuer = 'http://localhost:8080';
  private readonly redirectUri = 'http://localhost:4200/oauth2/callback';
  readonly session = signal<{ username: string; roles: string[] } | null>(this.restore());
  constructor(private http: HttpClient) {}
  get token(): string | null {
    return localStorage.getItem('access_token');
  }

  // Leer claims controla la interfaz; Spring valida la firma y autoriza cada petición.
  private restore() {
    try {
      const token = this.token;
      if (!token) return null;
      const encoded = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      const payload = JSON.parse(
        new TextDecoder().decode(Uint8Array.from(atob(encoded), (c) => c.charCodeAt(0))),
      );
      if (
        payload.exp * 1000 <= Date.now() ||
        !Array.isArray(payload.roles) ||
        typeof payload.username !== 'string'
      )
        throw new Error('Token inválido o expirado');
      return { username: payload.username as string, roles: payload.roles as string[] };
    } catch {
      localStorage.removeItem('access_token');
      return null;
    }
  }

  private base64url(bytes: Uint8Array): string {
    return btoa(String.fromCharCode(...bytes))
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '');
  }

  async authorizationUrl(): Promise<string> {
    this.clearSession();
    const verifier = this.base64url(crypto.getRandomValues(new Uint8Array(32)));
    const state = this.base64url(crypto.getRandomValues(new Uint8Array(32)));
    const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(verifier));
    // El verifier queda en esta pestaña, nunca se envía en la URL de autorización.
    sessionStorage.setItem(
      'oauth_request',
      JSON.stringify({ verifier, state, created: Date.now() }),
    );
    const params = new URLSearchParams({
      response_type: 'code',
      client_id: 'citas-frontend',
      redirect_uri: this.redirectUri,
      scope: 'openid profile citas.read citas.write',
      state,
      code_challenge: this.base64url(new Uint8Array(digest)),
      code_challenge_method: 'S256',
      prompt: 'login',
    });
    return `${this.issuer}/oauth2/authorize?${params}`;
  }

  completeLogin(params: URLSearchParams) {
    const saved = sessionStorage.getItem('oauth_request');
    sessionStorage.removeItem('oauth_request'); // Cada intento solo puede consumirse una vez.
    let request;
    try {
      request = JSON.parse(saved || 'null');
    } catch {
      request = null;
    }
    if (
      !request ||
      !request.state ||
      params.get('state') !== request.state ||
      !request.verifier ||
      Date.now() - request.created > 600000 ||
      params.has('error') ||
      !params.get('code')
    ) {
      return throwError(
        () => new Error('El inicio de sesión no es válido o expiró. Intenta nuevamente.'),
      );
    }
    const body = new HttpParams()
      .set('grant_type', 'authorization_code')
      .set('client_id', 'citas-frontend')
      .set('redirect_uri', this.redirectUri)
      .set('code', params.get('code')!)
      .set('code_verifier', request.verifier);
    // Cliente público: no hay client_secret en Angular ni refresh tokens.
    return this.http.post<TokenResponse>(`${this.issuer}/oauth2/token`, body).pipe(
      tap((response) => {
        localStorage.setItem('access_token', response.access_token);
        const session = this.restore();
        if (!session) throw new Error('El servidor devolvió un token inválido.');
        if (response.id_token) localStorage.setItem('id_token', response.id_token);
        this.session.set(session);
      }),
    );
  }

  hasRole(role: string): boolean {
    return this.session()?.roles.includes(role) ?? false;
  }
  clearSession() {
    localStorage.removeItem('access_token');
    localStorage.removeItem('id_token');
    this.session.set(null);
  }
  logout() {
    const idToken = localStorage.getItem('id_token');
    this.clearSession();
    sessionStorage.removeItem('oauth_request');
    // Cerrar también la sesión del servidor mediante el ID token emitido en el login.
    if (idToken)
      window.location.assign(
        `${this.issuer}/connect/logout?` +
          new URLSearchParams({
            id_token_hint: idToken,
            post_logout_redirect_uri: 'http://localhost:4200/',
          }),
      );
    return !!idToken;
  }
}
