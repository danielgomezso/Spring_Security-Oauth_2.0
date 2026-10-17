import { mockStorage } from './testing/storage';
import { vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { App } from './app';
describe('App', () => {
  beforeEach(async () => {
    mockStorage();
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([]), provideHttpClient()],
    }).compileComponents();
  });
  afterEach(() => vi.unstubAllGlobals());
  it('muestra servicios y login sin paneles privados al visitante', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('Clínica Demo');
    expect(html.textContent).toContain('Ingresar');
    expect(html.textContent).not.toContain('Administración');
    expect(html.textContent).not.toContain('Mis citas');
  });
});
