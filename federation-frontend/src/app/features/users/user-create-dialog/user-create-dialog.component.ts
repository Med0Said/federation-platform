import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';

import { ApiService } from '@core/services/api.service';
import { ManagedUser, UserRole } from '@core/models';
import { RoleLabelPipe } from '@shared/pipes';

const PASSWORD_PATTERN = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/;

@Component({
  selector: 'app-user-create-dialog',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatDialogModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, RoleLabelPipe,
  ],
  template: `
    <h2 mat-dialog-title>New user</h2>
    <form [formGroup]="form" (ngSubmit)="save()">
      <mat-dialog-content class="!pt-2">
        <div class="grid grid-cols-2 gap-3">
          <mat-form-field appearance="outline">
            <mat-label>First name</mat-label>
            <input matInput formControlName="firstName" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Last name</mat-label>
            <input matInput formControlName="lastName" />
          </mat-form-field>
        </div>
        <div class="grid grid-cols-2 gap-3">
          <mat-form-field appearance="outline">
            <mat-label>E-mail</mat-label>
            <input matInput type="email" formControlName="email" />
            @if (form.controls.email.hasError('email')) { <mat-error>Invalid e-mail</mat-error> }
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Username</mat-label>
            <input matInput formControlName="username" />
            @if (form.controls.username.hasError('pattern')) { <mat-error>Letters, digits, . _ - only</mat-error> }
          </mat-form-field>
        </div>
        <div class="grid grid-cols-2 gap-3">
          <mat-form-field appearance="outline">
            <mat-label>Role</mat-label>
            <mat-select formControlName="role">
              @for (r of roles; track r) { <mat-option [value]="r">{{ r | roleLabel }}</mat-option> }
            </mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Initial password</mat-label>
            <input matInput type="text" formControlName="password" autocomplete="new-password" />
            @if (form.controls.password.hasError('pattern')) {
              <mat-error>8+ chars with upper, lower, digit and &#64;$!%*?&amp;</mat-error>
            }
          </mat-form-field>
        </div>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancel</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="saving()">Create</button>
      </mat-dialog-actions>
    </form>
  `,
})
export class UserCreateDialogComponent {
  private api = inject(ApiService);
  private ref = inject(MatDialogRef<UserCreateDialogComponent, ManagedUser>);
  private fb  = inject(FormBuilder);
  readonly roles = Object.values(UserRole);
  saving = signal(false);

  form = this.fb.group({
    firstName: ['', Validators.required],
    lastName:  ['', Validators.required],
    email:     ['', [Validators.required, Validators.email]],
    username:  ['', [Validators.required, Validators.minLength(3), Validators.pattern(/^[a-zA-Z0-9._-]+$/)]],
    role:      [UserRole.CLUB_MANAGER as string, Validators.required],
    password:  ['', [Validators.required, Validators.pattern(PASSWORD_PATTERN)]],
  });

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    this.api.post<ManagedUser>('/users', this.form.getRawValue()).subscribe({
      next: user => this.ref.close(user),
      error: () => this.saving.set(false),
    });
  }
}
