import { Injectable } from '@angular/core';
import { MatSnackBar, MatSnackBarConfig } from '@angular/material/snack-bar';

/**
 * Notification service — wraps Angular Material's MatSnackBar with
 * semantic helpers for success, error, warning, and info messages.
 *
 * Usage:
 *   this.notify.success('Athlete saved successfully.');
 *   this.notify.error('Failed to load competitions.');
 */
@Injectable({ providedIn: 'root' })
export class NotificationService {

  private readonly defaults: MatSnackBarConfig = {
    duration:            4000,
    horizontalPosition:  'end',
    verticalPosition:    'top',
  };

  constructor(private snackBar: MatSnackBar) {}

  success(message: string, action = 'Dismiss'): void {
    this.snackBar.open(message, action, {
      ...this.defaults,
      panelClass: ['snack-success'],
    });
  }

  error(message: string, action = 'Dismiss'): void {
    this.snackBar.open(message, action, {
      ...this.defaults,
      duration:   8000,
      panelClass: ['snack-error'],
    });
  }

  warning(message: string, action = 'OK'): void {
    this.snackBar.open(message, action, {
      ...this.defaults,
      panelClass: ['snack-warning'],
    });
  }

  info(message: string, action = 'OK'): void {
    this.snackBar.open(message, action, {
      ...this.defaults,
      panelClass: ['snack-info'],
    });
  }

  dismiss(): void {
    this.snackBar.dismiss();
  }
}
