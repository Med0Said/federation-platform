import {
  ApplicationConfig,
  APP_INITIALIZER,
  provideZoneChangeDetection,
} from '@angular/core';
import { provideRouter, withComponentInputBinding, withViewTransitions } from '@angular/router';
import { provideHttpClient, withInterceptors, withFetch } from '@angular/common/http';
import { provideAnimationsAsync }   from '@angular/platform-browser/animations/async';
import { provideNativeDateAdapter } from '@angular/material/core';

import { routes }             from './app.routes';
import { authInterceptor }    from '@core/interceptors/auth.interceptor';
import { errorInterceptor }   from '@core/interceptors/error.interceptor';
import { AuthService }        from '@core/services/auth.service';
import { Observable }         from 'rxjs';

/**
 * App initializer — restores auth session on page reload.
 *
 * Called before the first route is activated.
 * If a refresh token exists in localStorage, attempts a silent token refresh
 * and profile load so the user remains logged in after navigation.
 */
function initializeAuth(authService: AuthService): () => Observable<boolean> {
  return () => authService.initializeAuth();
}

export const appConfig: ApplicationConfig = {
  providers: [

    // ── Zone.js (event coalescing for performance) ──────────────────────
    provideZoneChangeDetection({ eventCoalescing: true }),

    // ── Router ──────────────────────────────────────────────────────────
    provideRouter(
      routes,
      withComponentInputBinding(),     // bind route params to @Input() directly
      withViewTransitions()            // smooth page transitions (Chrome 111+)
    ),

    // ── HTTP Client ─────────────────────────────────────────────────────
    provideHttpClient(
      withFetch(),                     // use native fetch instead of XHR
      withInterceptors([
        authInterceptor,               // 1st: attach Bearer token
        errorInterceptor,              // 2nd: handle error responses
      ])
    ),

    // ── Angular Material ─────────────────────────────────────────────────
    provideAnimationsAsync(),
    provideNativeDateAdapter(),

    // ── App Initializer — restore session before first render ─────────────
    {
      provide:    APP_INITIALIZER,
      useFactory: initializeAuth,
      deps:       [AuthService],
      multi:      true,
    },
  ],
};
