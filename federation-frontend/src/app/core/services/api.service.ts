import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { environment }                    from '@environments/environment';
import { ApiResponse, PagedResponse }     from '@core/models';

/**
 * Generic HTTP service — wraps Angular's HttpClient with type-safe helpers
 * that unwrap the backend's {@link ApiResponse} envelope automatically.
 *
 * Every method returns the inner `data` payload so callers never have to
 * reach inside `response.data` themselves.
 *
 * Usage:
 *   this.api.get<Club[]>('/clubs').subscribe(clubs => …)
 */
@Injectable({ providedIn: 'root' })
export class ApiService {

  private readonly base = environment.apiBaseUrl;

  constructor(private http: HttpClient) {}

  // ────────────────────────────────────────────────────────────────────────
  // Core HTTP verbs (auto-unwrap ApiResponse envelope)
  // ────────────────────────────────────────────────────────────────────────

  get<T>(path: string, params?: Record<string, any>): Observable<T> {
    return this.http
      .get<ApiResponse<T>>(this.url(path), { params: this.buildParams(params) })
      .pipe(map(r => r.data));
  }

  post<T>(path: string, body: unknown = {}): Observable<T> {
    return this.http
      .post<ApiResponse<T>>(this.url(path), body)
      .pipe(map(r => r.data));
  }

  put<T>(path: string, body: unknown = {}): Observable<T> {
    return this.http
      .put<ApiResponse<T>>(this.url(path), body)
      .pipe(map(r => r.data));
  }

  patch<T>(path: string, body: unknown = {}): Observable<T> {
    return this.http
      .patch<ApiResponse<T>>(this.url(path), body)
      .pipe(map(r => r.data));
  }

  delete<T = void>(path: string): Observable<T> {
    return this.http
      .delete<ApiResponse<T>>(this.url(path))
      .pipe(map(r => r.data));
  }

  // ────────────────────────────────────────────────────────────────────────
  // Paginated GET — unwraps into PagedResponse<T>
  // ────────────────────────────────────────────────────────────────────────

  getPaged<T>(path: string, params?: Record<string, any>): Observable<PagedResponse<T>> {
    return this.http
      .get<ApiResponse<PagedResponse<T>>>(this.url(path), { params: this.buildParams(params) })
      .pipe(map(r => r.data));
  }

  // ────────────────────────────────────────────────────────────────────────
  // Raw response access — when you need status codes or headers
  // ────────────────────────────────────────────────────────────────────────

  getRaw<T>(path: string, params?: Record<string, any>): Observable<ApiResponse<T>> {
    return this.http
      .get<ApiResponse<T>>(this.url(path), { params: this.buildParams(params) });
  }

  postRaw<T>(path: string, body: unknown = {}): Observable<ApiResponse<T>> {
    return this.http.post<ApiResponse<T>>(this.url(path), body);
  }

  // ────────────────────────────────────────────────────────────────────────
  // Helpers
  // ────────────────────────────────────────────────────────────────────────

  private url(path: string): string {
    return `${this.base}${path.startsWith('/') ? path : '/' + path}`;
  }

  private buildParams(params?: Record<string, any>): HttpParams {
    let p = new HttpParams();
    if (!params) return p;
    Object.entries(params).forEach(([k, v]) => {
      if (v !== null && v !== undefined) {
        p = p.set(k, String(v));
      }
    });
    return p;
  }
}
