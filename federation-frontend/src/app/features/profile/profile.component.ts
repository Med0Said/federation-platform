import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule }       from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatTabsModule }      from '@angular/material/tabs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule }     from '@angular/material/input';
import { MatButtonModule }    from '@angular/material/button';
import { MatIconModule }      from '@angular/material/icon';

import { AuthService }             from '@core/services/auth.service';
import { ApiService }              from '@core/services/api.service';
import { NotificationService }     from '@core/services/notification.service';
import { PageHeaderComponent }     from '@shared/components/page-header/page-header.component';
import { RoleBadgeComponent, StatusChipComponent } from '@shared/components/status-chip/status-chip.component';
import { InitialsPipe }            from '@shared/pipes';
import { ChangePasswordRequest }   from '@core/models';

@Component({
  selector:   'app-profile',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatTabsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule,
    PageHeaderComponent, RoleBadgeComponent, StatusChipComponent, InitialsPipe,
  ],
  template: `
    <app-page-header
      title="My Profile"
      subtitle="Manage your account information and security settings"
      [breadcrumbs]="[{ label: 'Admin', path: '/admin' }, { label: 'Profile' }]">
    </app-page-header>

    <div class="grid grid-cols-1 lg:grid-cols-4 gap-6">

      <!-- ── Profile card ─────────────────────────────────────── -->
      <div class="lg:col-span-1">
        <div class="card-padded text-center">
          <!-- Avatar -->
          <div class="relative inline-block mb-4">
            @if (auth.currentUser()?.avatarUrl) {
              <img [src]="auth.currentUser()!.avatarUrl"
                   class="w-20 h-20 rounded-full object-cover mx-auto" alt="Avatar" />
            } @else {
              <div class="w-20 h-20 rounded-full bg-primary-600 flex items-center
                          justify-center text-white text-2xl font-bold mx-auto">
                {{ auth.currentUser()?.fullName | initials }}
              </div>
            }
          </div>

          <h2 class="font-bold text-surface-900 text-lg">
            {{ auth.currentUser()?.fullName }}
          </h2>
          <p class="text-sm text-surface-500 mb-3">{{ auth.currentUser()?.email }}</p>

          <div class="flex flex-col items-center gap-2">
            <app-role-badge [role]="auth.currentUser()?.role ?? ''" />
            <app-status-chip [status]="auth.currentUser()?.status ?? ''" />
          </div>

          <div class="mt-4 pt-4 border-t border-surface-100 text-left space-y-2">
            <div class="flex justify-between text-sm">
              <span class="text-surface-500">Username</span>
              <span class="font-medium">{{ auth.currentUser()?.username }}</span>
            </div>
            <div class="flex justify-between text-sm">
              <span class="text-surface-500">Member since</span>
              <span class="font-medium">
                {{ auth.currentUser()?.createdAt | date:'MMM yyyy' }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- ── Tabs ─────────────────────────────────────────────── -->
      <div class="lg:col-span-3">
        <mat-tab-group animationDuration="200ms" class="card">

          <!-- Profile tab -->
          <mat-tab label="Profile">
            <div class="p-6">
              <form [formGroup]="profileForm" (ngSubmit)="saveProfile()">
                <div class="grid grid-cols-2 gap-4 mb-4">
                  <mat-form-field appearance="outline">
                    <mat-label>First Name</mat-label>
                    <input matInput formControlName="firstName" />
                  </mat-form-field>
                  <mat-form-field appearance="outline">
                    <mat-label>Last Name</mat-label>
                    <input matInput formControlName="lastName" />
                  </mat-form-field>
                </div>
                <mat-form-field appearance="outline" class="w-full mb-4">
                  <mat-label>Phone</mat-label>
                  <input matInput formControlName="phone" type="tel" />
                  <mat-icon matPrefix class="mr-2">phone</mat-icon>
                </mat-form-field>
                <mat-form-field appearance="outline" class="w-full mb-4">
                  <mat-label>Avatar URL</mat-label>
                  <input matInput formControlName="avatarUrl" placeholder="https://…" />
                  <mat-icon matPrefix class="mr-2">image</mat-icon>
                </mat-form-field>
                <div class="flex justify-end">
                  <button mat-flat-button color="primary" type="submit"
                          [disabled]="profileSaving()">
                    {{ profileSaving() ? 'Saving…' : 'Save Changes' }}
                  </button>
                </div>
              </form>
            </div>
          </mat-tab>

          <!-- Security tab -->
          <mat-tab label="Security">
            <div class="p-6">
              <h3 class="font-semibold text-surface-800 mb-4">Change Password</h3>
              <form [formGroup]="passwordForm" (ngSubmit)="changePassword()">
                <mat-form-field appearance="outline" class="w-full mb-4">
                  <mat-label>Current Password</mat-label>
                  <input matInput type="password" formControlName="currentPassword" />
                  <mat-icon matPrefix class="mr-2">lock</mat-icon>
                </mat-form-field>
                <mat-form-field appearance="outline" class="w-full mb-4">
                  <mat-label>New Password</mat-label>
                  <input matInput type="password" formControlName="newPassword" />
                  <mat-icon matPrefix class="mr-2">lock_open</mat-icon>
                </mat-form-field>
                <mat-form-field appearance="outline" class="w-full mb-6">
                  <mat-label>Confirm New Password</mat-label>
                  <input matInput type="password" formControlName="confirmPassword" />
                  <mat-icon matPrefix class="mr-2">lock_open</mat-icon>
                </mat-form-field>
                <div class="flex justify-end">
                  <button mat-flat-button color="warn" type="submit"
                          [disabled]="passwordSaving()">
                    {{ passwordSaving() ? 'Updating…' : 'Update Password' }}
                  </button>
                </div>
              </form>
            </div>
          </mat-tab>

        </mat-tab-group>
      </div>
    </div>
  `,
})
export class ProfileComponent implements OnInit {
  readonly auth   = inject(AuthService);
  private api     = inject(ApiService);
  private notify  = inject(NotificationService);
  private fb      = inject(FormBuilder);

  profileSaving = signal(false);
  passwordSaving = signal(false);

  profileForm = this.fb.group({
    firstName: ['', Validators.required],
    lastName:  ['', Validators.required],
    phone:     [''],
    avatarUrl: [''],
  });

  passwordForm = this.fb.group({
    currentPassword: ['', Validators.required],
    newPassword:     ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', Validators.required],
  });

  ngOnInit(): void {
    const user = this.auth.currentUser();
    if (user) {
      this.profileForm.patchValue({
        firstName: user.firstName,
        lastName:  user.lastName,
        phone:     user.phone ?? '',
        avatarUrl: user.avatarUrl ?? '',
      });
    }
  }

  saveProfile(): void {
    if (this.profileForm.invalid) return;
    this.profileSaving.set(true);
    this.api.patch('/auth/me', this.profileForm.value).subscribe({
      next:  () => { this.notify.success('Profile updated.'); this.profileSaving.set(false); },
      error: () => this.profileSaving.set(false),
    });
  }

  changePassword(): void {
    if (this.passwordForm.invalid) return;
    this.passwordSaving.set(true);
    const req = this.passwordForm.getRawValue() as ChangePasswordRequest;
    this.auth.changePassword(req).subscribe({
      next:  () => this.passwordSaving.set(false),
      error: () => this.passwordSaving.set(false),
    });
  }
}
