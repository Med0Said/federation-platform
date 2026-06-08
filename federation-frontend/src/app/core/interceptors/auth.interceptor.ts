import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { inject }  from '@angular/core';
import { Router }  from '@angular/router';
import { Observable, throwError, BehaviorSubject } from 'rxjs';
import { catchError, filter, switchMap, take, finalize } from 'rxjs/operators';

import { TokenService } from '@core/services/token.service';
import { AuthService }  from '@core/services/auth.service';

let isRefreshing   = false;
const refreshDone$ = new BehaviorSubject<string | null>(null);

export const authInterceptor: HttpInterceptorFn = (
  req:  HttpRequest<unknown>,
  next: HttpHandlerFn
): Observable<any> => {
  const tokenService = inject(TokenService);
  const authService  = inject(AuthService);
  const router       = inject(Router);

  if (isPublicEndpoint(req.url)) {
    return next(req);
  }

  const accessToken = tokenService.getAccessToken();
  const authedReq   = accessToken ? addBearerToken(req, accessToken) : req;

  return next(authedReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        return handle401(req, next, tokenService, authService, router);
      }
      return throwError(() => error);
    })
  );
};

function handle401(
  originalReq:  HttpRequest<unknown>,
  next:         HttpHandlerFn,
  tokenService: TokenService,
  authService:  AuthService,
  router:       Router
): Observable<any> {
  if (!isRefreshing) {
    isRefreshing = true;
    refreshDone$.next(null);

    return authService.refreshToken().pipe(
      switchMap(() => {
        isRefreshing = false;
        const newToken = tokenService.getAccessToken()!;
        refreshDone$.next(newToken);
        return next(addBearerToken(originalReq, newToken));
      }),
      catchError(err => {
        isRefreshing = false;
        refreshDone$.next(null);
        router.navigate(['/auth/login']);
        return throwError(() => err);
      }),
      finalize(() => { isRefreshing = false; })
    );
  }

  return refreshDone$.pipe(
    filter(token => token !== null),
    take(1),
    switchMap(token => next(addBearerToken(originalReq, token!)))
  );
}

function addBearerToken(req: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
}

function isPublicEndpoint(url: string): boolean {
  return ['/auth/login', '/auth/register', '/auth/refresh'].some(p => url.includes(p));
}
