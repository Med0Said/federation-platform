import { Component, inject } from '@angular/core';
import { CommonModule }      from '@angular/common';
import { RouterModule }      from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule }     from '@angular/material/input';
import { MatButtonModule }    from '@angular/material/button';
import { MatIconModule }      from '@angular/material/icon';
import { MatCheckboxModule }  from '@angular/material/checkbox';

import { AuthService }    from '@core/services/auth.service';
import { UserRole }       from '@core/models';

@Component({
  selector:   'app-login',
  standalone: true,
  imports: [
    CommonModule, RouterModule, ReactiveFormsModule,
    MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule, MatCheckboxModule,
  ],
  template: `
    <div class="min-h-screen flex items-center justify-center bg-surface-50 px-4 py-12">
      <div class="w-full max-w-md">

        <!-- Card -->
        <div class="card-padded">

          <!-- Header -->
          <div class="text-center mb-8">
            <a routerLink="/" class="inline-flex items-center gap-2 mb-6 group">
              <div class="w-10 h-10 rounded-xl bg-primary-600 flex items-center justify-center">
                <mat-icon class="text-white">emoji_events</mat-icon>
              </div>
              <span class="font-bold text-xl text-surface-900">
                Sports<span class="text-primary-600">Fed</span>
              </span>
            </a>
            <h1 class="text-2xl font-bold text-surface-900">Welcome back</h1>
            <p class="mt-1 text-sm text-surface-500">Sign in to your account</p>
          </div>

          <!-- Info message (e.g. after password change) -->
          @if (infoMessage) {
            <div class="mb-4 p-3 bg-green-50 border border-green-200 rounded-lg
                        flex items-center gap-2 text-sm text-green-700">
              <mat-icon class="!text-base flex-shrink-0">check_circle</mat-icon>
              {{ infoMessage }}
            </div>
          }

          <!-- Error banner -->
          @if (errorMessage) {
            <div class="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg
                        flex items-center gap-2 text-sm text-red-700">
              <mat-icon class="!text-base flex-shrink-0">error_outline</mat-icon>
              {{ errorMessage }}
            </div>
          }

          <!-- Form -->
          <form [formGroup]="form" (ngSubmit)="onSubmit()" novalidate>

            <mat-form-field appearance="outline" class="w-full mb-3">
              <mat-label>Email or Username</mat-label>
              <input matInput
                     formControlName="usernameOrEmail"
                     autocomplete="username"
                     placeholder="admin@federation.local" />
              <mat-icon matPrefix class="mr-2 text-surface-400">person_outline</mat-icon>
              @if (form.get('usernameOrEmail')?.hasError('required') && form.get('usernameOrEmail')?.touched) {
                <mat-error>Email or username is required</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline" class="w-full mb-1">
              <mat-label>Password</mat-label>
              <input matInput
                     [type]="showPassword ? 'text' : 'password'"
                     formControlName="password"
                     autocomplete="current-password" />
              <mat-icon matPrefix class="mr-2 text-surface-400">lock_outline</mat-icon>
              <button mat-icon-button matSuffix type="button"
                      (click)="showPassword = !showPassword">
                <mat-icon>{{ showPassword ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
              @if (form.get('password')?.hasError('required') && form.get('password')?.touched) {
                <mat-error>Password is required</mat-error>
              }
            </mat-form-field>

            <div class="flex items-center justify-between mb-6 text-sm">
              <mat-checkbox formControlName="rememberMe" color="primary">
                Remember me
              </mat-checkbox>
              <a routerLink="/auth/forgot-password"
                 class="text-primary-600 hover:text-primary-700 font-medium">
                Forgot password?
              </a>
            </div>

            <button mat-flat-button
                    color="primary"
                    type="submit"
                    class="w-full !h-11 !rounded-xl !font-semibold !text-base"
                    [disabled]="loading">
              @if (loading) {
                <span class="flex items-center justify-center gap-2">
                  <mat-icon class="animate-spin-slow !text-lg">refresh</mat-icon>
                  Signing in…
                </span>
              } @else {
                Sign In
              }
            </button>
          </form>

          <!-- Divider -->
          <div class="relative my-6">
            <div class="absolute inset-0 flex items-center">
              <div class="w-full border-t border-surface-200"></div>
            </div>
            <div class="relative flex justify-center text-xs text-surface-400">
              <span class="bg-white px-3">New to the platform?</span>
            </div>
          </div>

          <a mat-stroked-button routerLink="/auth/register"
             class="w-full !h-11 !rounded-xl !font-medium">
            Create an Account
          </a>
        </div>

        <!-- Footer note -->
        <p class="mt-6 text-center text-xs text-surface-400">
          Official platform of the National Sports Federation
        </p>
      </div>
    </div>
  `,
})
export class LoginComponent {
  private fb    = inject(FormBuilder);
  private auth  = inject(AuthService);
  private router = inject(Router);
  private route  = inject(ActivatedRoute);

  showPassword  = false;
  loading       = false;
  errorMessage  = '';
  infoMessage   = '';

  form = this.fb.group({
    usernameOrEmail: ['', Validators.required],
    password:        ['', Validators.required],
    rememberMe:      [false],
  });

  constructor() {
    // Show message after password change or session expiry
    this.route.queryParamMap.subscribe(params => {
      const msg = params.get('message');
      if (msg === 'password_changed') {
        this.infoMessage = 'Password changed successfully. Please sign in again.';
      } else if (msg === 'session_expired') {
        this.errorMessage = 'Your session has expired. Please sign in again.';
      }
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading      = true;
    this.errorMessage = '';

    const { usernameOrEmail, password } = this.form.getRawValue();

    this.auth.login({ usernameOrEmail: usernameOrEmail!, password: password! }).subscribe({
      next: () => {
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
        this.router.navigateByUrl(returnUrl ?? this.getDefaultRoute());
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err?.error?.message ?? 'Invalid credentials. Please try again.';
      },
    });
  }

  private getDefaultRoute(): string {
    const role = this.auth.userRole();
    if (role === UserRole.ADMIN || role === UserRole.FEDERATION_STAFF) return '/admin/dashboard';
    if (role === UserRole.CLUB_MANAGER) return '/admin/clubs';
    if (role === UserRole.ATHLETE) return '/portal/profile';
    return '/home';
  }
}
