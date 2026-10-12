import { Component, inject, signal } from '@angular/core';
import { CommonModule }      from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule }     from '@angular/material/input';
import { MatButtonModule }    from '@angular/material/button';
import { MatIconModule }      from '@angular/material/icon';

import { ApiService }          from '@core/services/api.service';
import { NotificationService } from '@core/services/notification.service';

const PASSWORD_PATTERN = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/;

function passwordsMatch(group: AbstractControl): ValidationErrors | null {
  const { newPassword, confirmPassword } = group.value;
  return newPassword && confirmPassword && newPassword !== confirmPassword ? { mismatch: true } : null;
}

/** Target of the e-mailed link: /auth/reset-password?token=… */
@Component({
  selector:   'app-reset-password',
  standalone: true,
  imports: [
    CommonModule, RouterModule, ReactiveFormsModule,
    MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule,
  ],
  template: `
    <div class="min-h-screen flex items-center justify-center bg-surface-50 px-4 py-12">
      <div class="w-full max-w-md">
        <div class="card-padded">
          <div class="text-center mb-8">
            <div class="w-14 h-14 rounded-2xl bg-primary-100 flex items-center justify-center mx-auto mb-4">
              <mat-icon class="!text-3xl text-primary-600">password</mat-icon>
            </div>
            <h1 class="text-2xl font-bold text-surface-900">Choose a new password</h1>
          </div>

          @if (!token) {
            <div class="p-4 bg-red-50 border border-red-200 rounded-xl text-center text-sm text-red-700">
              This link is incomplete. Request a new one from the
              <a routerLink="/auth/forgot-password" class="underline">forgot password</a> page.
            </div>
          } @else {
            <form [formGroup]="form" (ngSubmit)="onSubmit()" novalidate class="space-y-1">
              <mat-form-field appearance="outline" class="w-full">
                <mat-label>New password</mat-label>
                <input matInput [type]="show() ? 'text' : 'password'" formControlName="newPassword" autocomplete="new-password" />
                <button mat-icon-button matSuffix type="button" (click)="show.set(!show())">
                  <mat-icon>{{ show() ? 'visibility_off' : 'visibility' }}</mat-icon>
                </button>
                @if (form.controls.newPassword.hasError('pattern')) {
                  <mat-error>8+ characters with upper, lower, digit and one of &#64;$!%*?&amp;</mat-error>
                }
              </mat-form-field>
              <mat-form-field appearance="outline" class="w-full">
                <mat-label>Confirm password</mat-label>
                <input matInput [type]="show() ? 'text' : 'password'" formControlName="confirmPassword" autocomplete="new-password" />
              </mat-form-field>
              @if (form.hasError('mismatch') && form.controls.confirmPassword.touched) {
                <p class="text-sm text-red-600 -mt-3 mb-3">Passwords do not match.</p>
              }
              <button mat-flat-button color="primary" type="submit" [disabled]="saving()"
                      class="w-full !h-11 !rounded-xl !font-semibold">
                {{ saving() ? 'Saving…' : 'Set new password' }}
              </button>
            </form>
          }

          <div class="mt-6 text-center">
            <a routerLink="/auth/login" class="text-sm text-primary-600 hover:underline">Back to login</a>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class ResetPasswordComponent {
  private fb     = inject(FormBuilder);
  private api    = inject(ApiService);
  private notify = inject(NotificationService);
  private router = inject(Router);
  readonly token = inject(ActivatedRoute).snapshot.queryParamMap.get('token');

  saving = signal(false);
  show   = signal(false);

  form = this.fb.group({
    newPassword:     ['', [Validators.required, Validators.pattern(PASSWORD_PATTERN)]],
    confirmPassword: ['', Validators.required],
  }, { validators: passwordsMatch });

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    this.api.post('/auth/reset-password', { token: this.token, ...this.form.getRawValue() }).subscribe({
      next: () => {
        this.notify.success('Password updated — you can now log in.');
        this.router.navigate(['/auth/login']);
      },
      error: () => this.saving.set(false),
    });
  }
}
