import { Component, inject } from '@angular/core';
import { CommonModule }      from '@angular/common';
import { RouterModule }      from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators, AbstractControl } from '@angular/forms';
import { Router } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule }     from '@angular/material/input';
import { MatButtonModule }    from '@angular/material/button';
import { MatIconModule }      from '@angular/material/icon';
import { MatStepperModule }   from '@angular/material/stepper';

import { AuthService } from '@core/services/auth.service';

// Password complexity validator
function passwordStrengthValidator(control: AbstractControl) {
  const val = control.value ?? '';
  const strong = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/.test(val);
  return strong ? null : { weakPassword: true };
}

// Cross-field validator: confirm must match password
function passwordsMatch(group: AbstractControl) {
  const pw  = group.get('password')?.value;
  const cpw = group.get('confirmPassword')?.value;
  return pw && cpw && pw !== cpw ? { mismatch: true } : null;
}

@Component({
  selector:   'app-register',
  standalone: true,
  imports: [
    CommonModule, RouterModule, ReactiveFormsModule,
    MatFormFieldModule, MatInputModule, MatButtonModule,
    MatIconModule, MatStepperModule,
  ],
  template: `
    <div class="min-h-screen flex items-center justify-center bg-surface-50 px-4 py-12">
      <div class="w-full max-w-lg">
        <div class="card-padded">

          <!-- Header -->
          <div class="text-center mb-8">
            <a routerLink="/" class="inline-flex items-center gap-2 mb-6">
              <div class="w-10 h-10 rounded-xl bg-primary-600 flex items-center justify-center">
                <mat-icon class="text-white">emoji_events</mat-icon>
              </div>
              <span class="font-bold text-xl text-surface-900">
                Sports<span class="text-primary-600">Fed</span>
              </span>
            </a>
            <h1 class="text-2xl font-bold text-surface-900">Create your account</h1>
            <p class="mt-1 text-sm text-surface-500">Join the federation platform</p>
          </div>

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

            <!-- Name row -->
            <div class="grid grid-cols-2 gap-3 mb-3">
              <mat-form-field appearance="outline" class="w-full">
                <mat-label>First Name</mat-label>
                <input matInput formControlName="firstName" autocomplete="given-name" />
                @if (f['firstName'].hasError('required') && f['firstName'].touched) {
                  <mat-error>Required</mat-error>
                }
              </mat-form-field>

              <mat-form-field appearance="outline" class="w-full">
                <mat-label>Last Name</mat-label>
                <input matInput formControlName="lastName" autocomplete="family-name" />
                @if (f['lastName'].hasError('required') && f['lastName'].touched) {
                  <mat-error>Required</mat-error>
                }
              </mat-form-field>
            </div>

            <mat-form-field appearance="outline" class="w-full mb-3">
              <mat-label>Username</mat-label>
              <input matInput formControlName="username" autocomplete="username"
                     placeholder="e.g. jane_doe" />
              <mat-icon matPrefix class="mr-2 text-surface-400">alternate_email</mat-icon>
              @if (f['username'].hasError('required') && f['username'].touched) {
                <mat-error>Username is required</mat-error>
              }
              @if (f['username'].hasError('pattern') && f['username'].touched) {
                <mat-error>Letters, digits, . _ - only</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline" class="w-full mb-3">
              <mat-label>Email</mat-label>
              <input matInput formControlName="email" type="email" autocomplete="email"
                     placeholder="you@example.com" />
              <mat-icon matPrefix class="mr-2 text-surface-400">mail_outline</mat-icon>
              @if (f['email'].hasError('required') && f['email'].touched) {
                <mat-error>Email is required</mat-error>
              }
              @if (f['email'].hasError('email') && f['email'].touched) {
                <mat-error>Enter a valid email address</mat-error>
              }
            </mat-form-field>

            <div formGroupName="passwords">
              <mat-form-field appearance="outline" class="w-full mb-3">
                <mat-label>Password</mat-label>
                <input matInput [type]="showPw ? 'text' : 'password'"
                       formControlName="password" autocomplete="new-password" />
                <mat-icon matPrefix class="mr-2 text-surface-400">lock_outline</mat-icon>
                <button mat-icon-button matSuffix type="button" (click)="showPw = !showPw">
                  <mat-icon>{{ showPw ? 'visibility_off' : 'visibility' }}</mat-icon>
                </button>
                @if (passwords.get('password')?.hasError('weakPassword') && passwords.get('password')?.touched) {
                  <mat-error>Min 8 chars with upper, lower, digit and special character</mat-error>
                }
              </mat-form-field>

              <!-- Password strength bar -->
              @if (passwords.get('password')?.value) {
                <div class="flex gap-1 mb-3 -mt-2">
                  @for (seg of [1,2,3,4]; track seg) {
                    <div class="h-1 flex-1 rounded-full transition-colors"
                         [class]="getStrengthColor(seg)"></div>
                  }
                </div>
              }

              <mat-form-field appearance="outline" class="w-full mb-4">
                <mat-label>Confirm Password</mat-label>
                <input matInput [type]="showPw ? 'text' : 'password'"
                       formControlName="confirmPassword" autocomplete="new-password" />
                <mat-icon matPrefix class="mr-2 text-surface-400">lock_outline</mat-icon>
                @if (passwords.hasError('mismatch') && passwords.get('confirmPassword')?.touched) {
                  <mat-error>Passwords do not match</mat-error>
                }
              </mat-form-field>
            </div>

            <button mat-flat-button color="primary" type="submit"
                    class="w-full !h-11 !rounded-xl !font-semibold !text-base"
                    [disabled]="loading">
              @if (loading) {
                <span class="flex items-center justify-center gap-2">
                  <mat-icon class="animate-spin-slow !text-lg">refresh</mat-icon>
                  Creating account…
                </span>
              } @else {
                Create Account
              }
            </button>
          </form>

          <div class="mt-6 text-center text-sm text-surface-500">
            Already have an account?
            <a routerLink="/auth/login" class="text-primary-600 font-medium hover:underline">
              Sign in
            </a>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class RegisterComponent {
  private fb    = inject(FormBuilder);
  private auth  = inject(AuthService);
  private router = inject(Router);

  showPw        = false;
  loading       = false;
  errorMessage  = '';

  form = this.fb.group({
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName:  ['', [Validators.required, Validators.maxLength(100)]],
    username:  ['', [Validators.required, Validators.minLength(3),
                     Validators.pattern(/^[a-zA-Z0-9._-]+$/)]],
    email:     ['', [Validators.required, Validators.email]],
    passwords: this.fb.group({
      password:        ['', [Validators.required, passwordStrengthValidator]],
      confirmPassword: ['', Validators.required],
    }, { validators: passwordsMatch }),
  });

  get f() { return this.form.controls; }
  get passwords() { return this.form.get('passwords')!; }

  getStrengthScore(): number {
    const pw = this.passwords.get('password')?.value ?? '';
    let score = 0;
    if (pw.length >= 8) score++;
    if (/[A-Z]/.test(pw)) score++;
    if (/\d/.test(pw)) score++;
    if (/[@$!%*?&]/.test(pw)) score++;
    return score;
  }

  getStrengthColor(segment: number): string {
    const score = this.getStrengthScore();
    if (segment > score) return 'bg-surface-200';
    if (score === 1) return 'bg-red-400';
    if (score === 2) return 'bg-amber-400';
    if (score === 3) return 'bg-yellow-400';
    return 'bg-green-500';
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading      = true;
    this.errorMessage = '';

    const { firstName, lastName, username, email } = this.form.getRawValue();
    const password = this.passwords.get('password')?.value!;

    this.auth.register({ firstName: firstName!, lastName: lastName!,
                         username: username!, email: email!, password }).subscribe({
      next: () => this.router.navigate(['/admin/dashboard']),
      error: (err) => {
        this.loading      = false;
        this.errorMessage = err?.error?.message ?? 'Registration failed. Please try again.';
      },
    });
  }
}
