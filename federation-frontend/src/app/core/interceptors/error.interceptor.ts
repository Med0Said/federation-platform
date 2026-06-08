import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject }  from '@angular/core';
import { throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { NotificationService } from '@core/services/notification.service';
import { ValidationErrorResponse } from '@core/models';

/**
 * Global error interceptor — functional style (Angular 19).
 *
 * Responsibilities:
 *  - 400 Validation errors → extract field messages and notify user
 *  - 401 Unauthenticated   → silently passed (handled by authInterceptor)
 *  - 403 Forbidden         → notify user
 *  - 404 Not found         → notify user
 *  - 409 Conflict          → notify user
 *  - 422 Business rule     → notify user
 *  - 500 Server error      → generic error message
 *  - Network errors        → offline/timeout message
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const notify = inject(NotificationService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // Don't show notification for 401 — auth interceptor handles it
      if (error.status !== 401) {
        handleError(error, notify);
      }
      return throwError(() => error);
    })
  );
};

function handleError(error: HttpErrorResponse, notify: NotificationService): void {

  // Network / CORS error (status 0)
  if (error.status === 0) {
    notify.error('Network error — please check your connection and try again.');
    return;
  }

  const body = error.error;

  switch (error.status) {

    case 400: {
      // Validation error — show field-level messages
      if (body?.errors?.length) {
        const ve = body as ValidationErrorResponse;
        const msg = ve.errors.map((e: any) => `${e.field}: ${e.message}`).join('\n');
        notify.error(msg || ve.message || 'Validation failed.');
      } else {
        notify.error(body?.message || 'Invalid request. Please check your input.');
      }
      break;
    }

    case 403:
      notify.error('You do not have permission to perform this action.');
      break;

    case 404:
      notify.warning(body?.message || 'The requested resource was not found.');
      break;

    case 409:
      notify.error(body?.message || 'Conflict — this record already exists.');
      break;

    case 422:
      notify.error(body?.message || 'The operation could not be completed due to a business rule violation.');
      break;

    case 429:
      notify.warning('Too many requests — please wait a moment before trying again.');
      break;

    case 500:
    case 502:
    case 503:
      notify.error('A server error occurred. Please try again later.');
      break;

    default:
      notify.error(body?.message || `An unexpected error occurred (${error.status}).`);
  }
}
