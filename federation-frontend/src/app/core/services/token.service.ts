import { Injectable } from '@angular/core';
import { environment } from '@environments/environment';

/**
 * Stateful token storage service.
 *
 * Strategy:
 *  - Access token:  stored in memory ONLY (never localStorage — XSS mitigation)
 *  - Refresh token: stored in localStorage (httpOnly cookie ideal, but requires backend changes)
 *
 * The refresh token's presence indicates "user has logged in before on this device"
 * but the actual authentication state is determined by the in-memory access token.
 */
@Injectable({ providedIn: 'root' })
export class TokenService {

  private accessToken: string | null = null;

  // ────────────────────────────────────────────────────────────────────────
  // Access Token (memory only)
  // ────────────────────────────────────────────────────────────────────────

  getAccessToken(): string | null {
    return this.accessToken;
  }

  setAccessToken(token: string): void {
    this.accessToken = token;
  }

  clearAccessToken(): void {
    this.accessToken = null;
  }

  // ────────────────────────────────────────────────────────────────────────
  // Refresh Token (localStorage — survive page reload)
  // ────────────────────────────────────────────────────────────────────────

  getRefreshToken(): string | null {
    if (this.isLocalStorageAvailable()) {
      return localStorage.getItem(environment.refreshTokenKey);
    }
    return null;
  }

  setRefreshToken(token: string): void {
    if (this.isLocalStorageAvailable()) {
      localStorage.setItem(environment.refreshTokenKey, token);
    }
  }

  clearRefreshToken(): void {
    if (this.isLocalStorageAvailable()) {
      localStorage.removeItem(environment.refreshTokenKey);
    }
  }

  // ────────────────────────────────────────────────────────────────────────
  // Clear all
  // ────────────────────────────────────────────────────────────────────────

  clearAll(): void {
    this.clearAccessToken();
    this.clearRefreshToken();
  }

  // ────────────────────────────────────────────────────────────────────────
  // Token introspection (decode JWT payload without verification)
  // ────────────────────────────────────────────────────────────────────────

  decodeToken(token: string): any {
    try {
      const payload = token.split('.')[1];
      return JSON.parse(atob(payload));
    } catch {
      return null;
    }
  }

  /** Extract expiry timestamp (seconds since epoch) from a JWT. */
  getTokenExpiry(token: string): number | null {
    const decoded = this.decodeToken(token);
    return decoded?.exp ?? null;
  }

  /** Check if the access token is expired or will expire soon (within buffer). */
  isTokenExpiringSoon(bufferSeconds: number = 60): boolean {
    const token = this.getAccessToken();
    if (!token) return true;

    const expiry = this.getTokenExpiry(token);
    if (!expiry) return true;

    const now = Math.floor(Date.now() / 1000);
    return expiry - now < bufferSeconds;
  }

  // ────────────────────────────────────────────────────────────────────────
  // Helpers
  // ────────────────────────────────────────────────────────────────────────

  private isLocalStorageAvailable(): boolean {
    try {
      const test = '__test__';
      localStorage.setItem(test, test);
      localStorage.removeItem(test);
      return true;
    } catch {
      return false;
    }
  }
}
