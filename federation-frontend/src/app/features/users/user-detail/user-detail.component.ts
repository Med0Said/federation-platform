import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';

import { ApiService } from '@core/services/api.service';
import { AuthService } from '@core/services/auth.service';
import { NotificationService } from '@core/services/notification.service';
import { humanize, ManagedUser, UserRole, UserStatus } from '@core/models';
import { PageHeaderComponent } from '@shared/components/page-header/page-header.component';
import { RoleBadgeComponent, StatusChipComponent } from '@shared/components/status-chip/status-chip.component';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';
import { ConfirmDialogComponent } from '@shared/components/confirm-dialog/confirm-dialog.component';
import { InitialsPipe, RoleLabelPipe } from '@shared/pipes';

@Component({
  selector: 'app-user-detail',
  standalone: true,
  imports: [
    CommonModule, RouterModule, ReactiveFormsModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, MatIconModule,
    PageHeaderComponent, RoleBadgeComponent, StatusChipComponent, LoadingSpinnerComponent, InitialsPipe, RoleLabelPipe,
  ],
  template: `
    @if (loading()) {
      <app-loading-spinner message="Loading user…" />
    } @else { @if (user(); as u) {
      <app-page-header [title]="u.fullName" [subtitle]="u.email"
        [breadcrumbs]="[{ label: 'Admin', path: '/admin' }, { label: 'Users', path: '/admin/users' }, { label: u.fullName }]">
        @if (!isSelf()) {
          <button mat-stroked-button color="warn" (click)="remove(u)" actions>
            <mat-icon>delete</mat-icon> Delete
          </button>
        }
      </app-page-header>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div class="space-y-4">
          <div class="card-padded text-center">
            <div class="w-20 h-20 rounded-full bg-primary-100 flex items-center justify-center text-primary-700
                        text-2xl font-bold mx-auto mb-3">{{ u.fullName | initials }}</div>
            <p class="font-semibold text-surface-900">{{ u.fullName }}</p>
            <p class="text-sm text-surface-500 mb-3">&#64;{{ u.username }}</p>
            <div class="flex justify-center gap-2">
              <app-role-badge [role]="u.role" />
              <app-status-chip [status]="u.status" />
            </div>
          </div>
          <div class="card-padded text-sm space-y-2">
            <div class="flex justify-between"><span class="text-surface-500">Created</span><span>{{ u.createdAt | date:'d MMM y' }}</span></div>
            <div class="flex justify-between"><span class="text-surface-500">Last login</span><span>{{ u.lastLogin ? (u.lastLogin | date:'d MMM y, HH:mm') : 'Never' }}</span></div>
            @if (u.athleteId) {
              <a [routerLink]="['/admin/athletes', u.athleteId]" class="flex items-center gap-1 text-primary-600 hover:underline pt-2">
                <mat-icon class="!text-base !w-4 !h-4">directions_run</mat-icon> Linked athlete profile
              </a>
            }
          </div>
        </div>

        <div class="lg:col-span-2 space-y-4">
          <div class="card-padded">
            <h3 class="font-semibold text-surface-800 mb-4">Access</h3>
            @if (isSelf()) {
              <p class="text-sm text-surface-500 mb-3">You cannot change your own role or status.</p>
            }
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <mat-form-field appearance="outline">
                <mat-label>Role</mat-label>
                <mat-select [formControl]="roleCtrl">
                  @for (r of roles; track r) { <mat-option [value]="r">{{ r | roleLabel }}</mat-option> }
                </mat-select>
                <mat-hint>Changing the role signs the user out</mat-hint>
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Status</mat-label>
                <mat-select [formControl]="statusCtrl">
                  @for (s of statuses; track s) { <mat-option [value]="s">{{ humanize(s) }}</mat-option> }
                </mat-select>
              </mat-form-field>
            </div>
            <div class="flex justify-end gap-3 mt-2">
              <button mat-stroked-button (click)="resetPassword(u)">
                <mat-icon>lock_reset</mat-icon> Reset password
              </button>
              <button mat-flat-button color="primary" (click)="saveAccess(u)" [disabled]="isSelf() || !accessChanged()">
                Save access
              </button>
            </div>
            @if (temporaryPassword()) {
              <div class="mt-4 p-4 rounded-lg bg-amber-50 border border-amber-200 text-sm">
                <p class="font-semibold text-amber-800">Temporary password (shown once)</p>
                <p class="font-mono text-lg mt-1 select-all">{{ temporaryPassword() }}</p>
                <p class="text-amber-700 mt-1">Share it securely; the user should change it after logging in.</p>
              </div>
            }
          </div>

          <form [formGroup]="profile" (ngSubmit)="saveProfile(u)" class="card-padded">
            <h3 class="font-semibold text-surface-800 mb-4">Profile</h3>
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <mat-form-field appearance="outline">
                <mat-label>First name</mat-label>
                <input matInput formControlName="firstName" />
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Last name</mat-label>
                <input matInput formControlName="lastName" />
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Phone</mat-label>
                <input matInput formControlName="phone" />
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Avatar URL</mat-label>
                <input matInput formControlName="avatarUrl" />
              </mat-form-field>
            </div>
            <div class="flex justify-end">
              <button mat-flat-button color="primary" type="submit" [disabled]="profile.invalid || profile.pristine">Save profile</button>
            </div>
          </form>
        </div>
      </div>
    } }
  `,
})
export class UserDetailComponent implements OnInit {
  private api    = inject(ApiService);
  private auth   = inject(AuthService);
  private notify = inject(NotificationService);
  private dialog = inject(MatDialog);
  private route  = inject(ActivatedRoute);
  private router = inject(Router);
  private fb     = inject(FormBuilder);
  readonly humanize = humanize;
  readonly roles    = Object.values(UserRole);
  readonly statuses = Object.values(UserStatus);

  user              = signal<ManagedUser | null>(null);
  loading           = signal(true);
  temporaryPassword = signal<string | null>(null);
  roleCtrl          = new FormControl<string>('');
  statusCtrl        = new FormControl<string>('');
  private formTick  = signal(0);

  profile = this.fb.group({
    firstName: ['', Validators.required],
    lastName:  ['', Validators.required],
    phone:     [''],
    avatarUrl: [''],
  });

  isSelf = computed(() => this.user()?.id === this.auth.currentUser()?.id);
  accessChanged = computed(() => {
    this.formTick();
    const u = this.user();
    return !!u && (this.roleCtrl.value !== u.role || this.statusCtrl.value !== u.status);
  });

  ngOnInit(): void {
    this.roleCtrl.valueChanges.subscribe(() => this.formTick.update(t => t + 1));
    this.statusCtrl.valueChanges.subscribe(() => this.formTick.update(t => t + 1));
    this.load();
  }

  private load(): void {
    this.api.get<ManagedUser>(`/users/${this.route.snapshot.paramMap.get('id')}`).subscribe({
      next: u => this.apply(u),
      error: () => this.loading.set(false),
    });
  }

  private apply(u: ManagedUser): void {
    this.user.set(u);
    this.roleCtrl.setValue(u.role);
    this.statusCtrl.setValue(u.status);
    this.profile.reset({ firstName: u.firstName, lastName: u.lastName, phone: u.phone ?? '', avatarUrl: u.avatarUrl ?? '' });
    if (this.isSelf()) { this.roleCtrl.disable(); this.statusCtrl.disable(); }
    this.loading.set(false);
  }

  saveAccess(u: ManagedUser): void {
    const calls = [];
    if (this.roleCtrl.value !== u.role) calls.push(this.api.patch<ManagedUser>(`/users/${u.id}/role`, { role: this.roleCtrl.value }));
    if (this.statusCtrl.value !== u.status) calls.push(this.api.patch<ManagedUser>(`/users/${u.id}/status`, { status: this.statusCtrl.value }));
    let done = 0;
    calls.forEach(c => c.subscribe(updated => {
      if (++done === calls.length) { this.apply(updated); this.notify.success('Access updated.'); }
    }));
  }

  saveProfile(u: ManagedUser): void {
    this.api.put<ManagedUser>(`/users/${u.id}`, this.profile.getRawValue()).subscribe(updated => {
      this.apply(updated);
      this.notify.success('Profile updated.');
    });
  }

  resetPassword(u: ManagedUser): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Reset password',
        message: `Generate a new temporary password for ${u.fullName}? Their current sessions will be closed.`,
        confirmText: 'Reset',
      },
    }).afterClosed().subscribe(ok => {
      if (!ok) return;
      this.api.post<ManagedUser>(`/users/${u.id}/reset-password`).subscribe(res => {
        this.temporaryPassword.set(res.temporaryPassword ?? null);
        this.notify.success('Password reset.');
      });
    });
  }

  remove(u: ManagedUser): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Delete user', message: `Permanently delete the account of ${u.fullName}?`, confirmText: 'Delete', danger: true },
    }).afterClosed().subscribe(ok => {
      if (!ok) return;
      this.api.delete(`/users/${u.id}`).subscribe(() => {
        this.notify.success('User deleted.');
        this.router.navigate(['/admin/users']);
      });
    });
  }
}
