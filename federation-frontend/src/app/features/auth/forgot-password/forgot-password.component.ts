import { Component, inject } from '@angular/core';
import { CommonModule }      from '@angular/common';
import { RouterModule }      from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule }     from '@angular/material/input';
import { MatButtonModule }    from '@angular/material/button';
import { MatIconModule }      from '@angular/material/icon';

@Component({
  selector:   'app-forgot-password',
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
            <div class="w-14 h-14 rounded-2xl bg-primary-100 flex items-center justify-center
                        mx-auto mb-4">
              <mat-icon class="!text-3xl text-primary-600">lock_reset</mat-icon>
            </div>
            <h1 class="text-2xl font-bold text-surface-900">Forgot password?</h1>
            <p class="mt-2 text-sm text-surface-500">
              Enter your email and we'll send you a reset link.
            </p>
          </div>

          @if (submitted) {
            <div class="p-4 bg-green-50 border border-green-200 rounded-xl text-center">
              <mat-icon class="text-green-500 !text-3xl mb-2">mark_email_read</mat-icon>
              <p class="font-semibold text-green-800">Check your inbox</p>
              <p class="text-sm text-green-600 mt-1">
                If an account exists for {{ submittedEmail }}, you'll receive a reset link.
              </p>
            </div>
          } @else {
            <form [formGroup]="form" (ngSubmit)="onSubmit()" novalidate>
              <mat-form-field appearance="outline" class="w-full mb-4">
                <mat-label>Email address</mat-label>
                <input matInput formControlName="email" type="email"
                       placeholder="you@example.com" />
                <mat-icon matPrefix class="mr-2 text-surface-400">mail_outline</mat-icon>
                @if (form.get('email')?.hasError('required') && form.get('email')?.touched) {
                  <mat-error>Email is required</mat-error>
                }
                @if (form.get('email')?.hasError('email') && form.get('email')?.touched) {
                  <mat-error>Enter a valid email address</mat-error>
                }
              </mat-form-field>

              <button mat-flat-button color="primary" type="submit"
                      class="w-full !h-11 !rounded-xl !font-semibold">
                Send Reset Link
              </button>
            </form>
          }

          <div class="mt-6 text-center">
            <a routerLink="/auth/login"
               class="text-sm text-primary-600 hover:underline flex items-center
                      justify-center gap-1">
              <mat-icon class="!text-base">arrow_back</mat-icon>
              Back to login
            </a>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class ForgotPasswordComponent {
  private fb = inject(FormBuilder);

  submitted     = false;
  submittedEmail = '';

  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
  });

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    // TODO: wire up to /auth/forgot-password API when backend supports it
    this.submittedEmail = this.form.value.email!;
    this.submitted = true;
  }
}
