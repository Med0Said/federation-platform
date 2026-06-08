import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, throwError, of } from 'rxjs';
import { tap, catchError, switchMap, map } from 'rxjs/operators';

import { environment } from '@environments/environment';
import { TokenService } from './token.service';
import {
  User, UserRole, LoginRequest, RegisterRequest, AuthResponse,
  RefreshTokenRequest, ChangePasswordRequest, ApiResponse
} from '@core/models';

@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly apiUrl = `${environment.apiBaseUrl}/auth`;

  private currentUserSignal    = signal<User | null>(null);
  private isAuthenticatedSignal = signal<boolean>(false);
  private isLoadingSignal       = signal<boolean>(false);

  readonly currentUser    = this.currentUserSignal.asReadonly();
  readonly isAuthenticated = this.isAuthenticatedSignal.asReadonly();
  readonly isLoading       = this.isLoadingSignal.asReadonly();

  readonly userRole = computed(() => this.currentUser()?.role ?? null);
  readonly isAdmin  = computed(() => this.userRole() === UserRole.ADMIN);
  readonly isStaff  = computed(() => {
    const role = this.userRole();
    return role === UserRole.ADMIN || role === UserRole.FEDERATION_STAFF;
  });

  constructor(
    private http:   HttpClient,
    private router: Router,
    private token:  TokenService
  ) {}

  // ── Login ──────────────────────────────────────────────────────────────

  login(request: LoginRequest): Observable<any> {
    this.isLoadingSignal.set(true);
    return this.http.post<ApiResponse<AuthResponse>>(`${this.apiUrl}/login`, request).pipe(
      map(r => r.data),
      tap((auth: AuthResponse) => this.handleAuthSuccess(auth)),
      switchMap(() => this.loadProfile()),
      catchError(err => {
        this.isLoadingSignal.set(false);
        return throwError(() => err);
      }),
      tap(() => this.isLoadingSignal.set(false))
    );
  }

  // ── Register ───────────────────────────────────────────────────────────

  register(request: RegisterRequest): Observable<any> {
    this.isLoadingSignal.set(true);
    return this.http.post<ApiResponse<AuthResponse>>(`${this.apiUrl}/register`, request).pipe(
      map(r => r.data),
      tap((auth: AuthResponse) => this.handleAuthSuccess(auth)),
      switchMap(() => this.loadProfile()),
      catchError(err => {
        this.isLoadingSignal.set(false);
        return throwError(() => err);
      }),
      tap(() => this.isLoadingSignal.set(false))
    );
  }

  // ── Logout ─────────────────────────────────────────────────────────────

  logout(): Observable<any> {
    const hasToken = !!this.token.getAccessToken();
    this.clearAuthState();
    this.router.navigate(['/auth/login']);

    if (hasToken) {
      return this.http.post<any>(`${this.apiUrl}/logout`, {}).pipe(
        catchError(() => of(null))
      );
    }
    return of(null);
  }

  // ── Token refresh ──────────────────────────────────────────────────────

  refreshToken(): Observable<any> {
    const refreshToken = this.token.getRefreshToken();
    if (!refreshToken) {
      return throwError(() => new Error('No refresh token available'));
    }
    const request: RefreshTokenRequest = { refreshToken };
    return this.http.post<ApiResponse<AuthResponse>>(`${this.apiUrl}/refresh`, request).pipe(
      map(r => r.data),
      tap((auth: AuthResponse) => {
        this.token.setAccessToken(auth.accessToken);
        this.token.setRefreshToken(auth.refreshToken);
      }),
      catchError(err => {
        this.clearAuthState();
        this.router.navigate(['/auth/login']);
        return throwError(() => err);
      })
    );
  }

  // ── Profile ────────────────────────────────────────────────────────────

  loadProfile(): Observable<any> {
    return this.http.get<ApiResponse<User>>(`${this.apiUrl}/me`).pipe(
      map(r => r.data),
      tap((user: User) => {
        this.currentUserSignal.set(user);
        this.isAuthenticatedSignal.set(true);
      }),
      catchError(err => {
        this.clearAuthState();
        return throwError(() => err);
      })
    );
  }

  // ── Change password ────────────────────────────────────────────────────

  changePassword(request: ChangePasswordRequest): Observable<any> {
    return this.http.patch<ApiResponse<void>>(`${this.apiUrl}/change-password`, request).pipe(
      tap(() => {
        this.clearAuthState();
        this.router.navigate(['/auth/login'], {
          queryParams: { message: 'password_changed' }
        });
      })
    );
  }

  // ── Authorization helpers ──────────────────────────────────────────────

  hasRole(role: UserRole): boolean {
    return this.currentUser()?.role === role;
  }

  hasAnyRole(roles: UserRole[]): boolean {
    const currentRole = this.currentUser()?.role;
    return currentRole ? roles.includes(currentRole) : false;
  }

  canAccessAdmin(): boolean {
    return this.hasAnyRole([UserRole.ADMIN, UserRole.FEDERATION_STAFF]);
  }

  // ── Session restore on page reload ────────────────────────────────────

  initializeAuth(): Observable<boolean> {
    const accessToken  = this.token.getAccessToken();
    const refreshToken = this.token.getRefreshToken();

    if (!accessToken && !refreshToken) return of(false);

    if (accessToken) {
      return this.loadProfile().pipe(
        switchMap(() => of(true)),
        catchError(() => {
          if (refreshToken) {
            return this.refreshToken().pipe(
              switchMap(() => this.loadProfile()),
              switchMap(() => of(true)),
              catchError(() => of(false))
            );
          }
          return of(false);
        })
      );
    }

    return this.refreshToken().pipe(
      switchMap(() => this.loadProfile()),
      switchMap(() => of(true)),
      catchError(() => of(false))
    );
  }

  // ── Private ────────────────────────────────────────────────────────────

  private handleAuthSuccess(auth: AuthResponse): void {
    this.token.setAccessToken(auth.accessToken);
    this.token.setRefreshToken(auth.refreshToken);
  }

  private clearAuthState(): void {
    this.token.clearAll();
    this.currentUserSignal.set(null);
    this.isAuthenticatedSignal.set(false);
  }
}
