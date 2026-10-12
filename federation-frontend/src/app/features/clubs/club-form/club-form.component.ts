import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { ApiService } from '@core/services/api.service';
import { AuthService } from '@core/services/auth.service';
import { NotificationService } from '@core/services/notification.service';
import { Club, ManagedUser, UserRole } from '@core/models';
import { PageHeaderComponent } from '@shared/components/page-header/page-header.component';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';

@Component({
  selector: 'app-club-form',
  standalone: true,
  imports: [
    CommonModule, RouterModule, ReactiveFormsModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, MatIconModule,
    PageHeaderComponent, LoadingSpinnerComponent,
  ],
  template: `
    <app-page-header
      [title]="isEdit ? 'Edit Club' : 'New Club'"
      [subtitle]="isEdit ? 'Update club information' : 'Register a club with the federation'"
      [breadcrumbs]="[
        { label: 'Admin', path: '/admin' },
        { label: 'Clubs', path: '/admin/clubs' },
        { label: isEdit ? 'Edit' : 'New' }
      ]">
    </app-page-header>

    @if (initLoading()) {
      <app-loading-spinner message="Loading club…" />
    } @else {
      <form [formGroup]="form" (ngSubmit)="onSubmit()" class="card-padded space-y-4 max-w-3xl">
        <h3 class="font-semibold text-surface-800 border-b border-surface-100 pb-3">Identity</h3>

        <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <mat-form-field appearance="outline" class="sm:col-span-2">
            <mat-label>Club name</mat-label>
            <input matInput formControlName="name" />
            @if (f.name.hasError('required')) { <mat-error>Required</mat-error> }
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Short name</mat-label>
            <input matInput formControlName="shortName" maxlength="20" placeholder="EST" />
          </mat-form-field>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <mat-form-field appearance="outline">
            <mat-label>Federation license number</mat-label>
            <input matInput formControlName="licenseNumber" placeholder="CLB-2025-0001" />
            @if (f.licenseNumber.hasError('required')) { <mat-error>Required</mat-error> }
            @if (!isStaff) { <mat-hint>Managed by the federation</mat-hint> }
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Founded</mat-label>
            <input matInput type="number" formControlName="foundedYear" min="1800" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Status</mat-label>
            <mat-select formControlName="status">
              <mat-option value="ACTIVE">Active</mat-option>
              <mat-option value="SUSPENDED">Suspended</mat-option>
              <mat-option value="DISSOLVED">Dissolved</mat-option>
            </mat-select>
          </mat-form-field>
        </div>

        @if (isAdmin) {
          <mat-form-field appearance="outline" class="w-full">
            <mat-label>Club manager</mat-label>
            <mat-select formControlName="managerId">
              <mat-option [value]="null">— None —</mat-option>
              @for (m of managers(); track m.id) {
                <mat-option [value]="m.id">{{ m.fullName }} ({{ m.email }})</mat-option>
              }
            </mat-select>
            <mat-hint>Accounts with the Club Manager role</mat-hint>
          </mat-form-field>
        }

        <h3 class="font-semibold text-surface-800 border-b border-surface-100 pb-3 pt-2">Location & contact</h3>

        <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <mat-form-field appearance="outline">
            <mat-label>City</mat-label>
            <input matInput formControlName="city" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Region</mat-label>
            <input matInput formControlName="region" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Country (ISO)</mat-label>
            <input matInput formControlName="country" maxlength="2" />
          </mat-form-field>
        </div>

        <mat-form-field appearance="outline" class="w-full">
          <mat-label>Address</mat-label>
          <input matInput formControlName="address" />
        </mat-form-field>

        <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <mat-form-field appearance="outline">
            <mat-label>E-mail</mat-label>
            <input matInput type="email" formControlName="email" />
            @if (f.email.hasError('email')) { <mat-error>Invalid e-mail</mat-error> }
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Phone</mat-label>
            <input matInput formControlName="phone" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Website</mat-label>
            <input matInput formControlName="website" placeholder="https://" />
          </mat-form-field>
        </div>

        <mat-form-field appearance="outline" class="w-full">
          <mat-label>Logo URL</mat-label>
          <input matInput formControlName="logoUrl" placeholder="https://" />
        </mat-form-field>

        <mat-form-field appearance="outline" class="w-full">
          <mat-label>Description</mat-label>
          <textarea matInput rows="4" formControlName="description"></textarea>
        </mat-form-field>

        <div class="flex justify-end gap-3 pt-2">
          <button mat-stroked-button type="button" (click)="cancel()">Cancel</button>
          <button mat-flat-button color="primary" type="submit" [disabled]="saving()">
            <mat-icon>save</mat-icon> {{ saving() ? 'Saving…' : 'Save' }}
          </button>
        </div>
      </form>
    }
  `,
})
export class ClubFormComponent implements OnInit {
  private fb     = inject(FormBuilder);
  private api    = inject(ApiService);
  private auth   = inject(AuthService);
  private notify = inject(NotificationService);
  private router = inject(Router);
  private route  = inject(ActivatedRoute);

  readonly isStaff = this.auth.hasAnyRole([UserRole.ADMIN, UserRole.FEDERATION_STAFF]);
  readonly isAdmin = this.auth.hasRole(UserRole.ADMIN);

  saving      = signal(false);
  initLoading = signal(false);
  managers    = signal<ManagedUser[]>([]);
  isEdit      = false;
  clubId?: string;

  form = this.fb.group({
    name:          ['', [Validators.required, Validators.maxLength(200)]],
    shortName:     [''],
    licenseNumber: ['', Validators.required],
    foundedYear:   [null as number | null],
    status:        ['ACTIVE'],
    managerId:     [null as string | null],
    city:          [''],
    region:        [''],
    country:       ['TN'],
    address:       [''],
    email:         ['', Validators.email],
    phone:         [''],
    website:       [''],
    logoUrl:       [''],
    description:   [''],
  });

  get f() { return this.form.controls; }

  ngOnInit(): void {
    this.clubId = this.route.snapshot.paramMap.get('id') ?? undefined;
    this.isEdit = !!this.clubId;

    // Federation-controlled fields are read-only for club managers
    if (!this.isStaff) {
      this.f.licenseNumber.disable();
      this.f.status.disable();
    }
    if (this.isAdmin) {
      this.api.getPaged<ManagedUser>('/users', { role: UserRole.CLUB_MANAGER, size: 200, sort: 'lastName,asc' })
        .subscribe(p => this.managers.set(p.content));
    }
    if (this.isEdit) {
      this.initLoading.set(true);
      this.api.get<Club>(`/clubs/${this.clubId}`).subscribe({
        next: c => { this.form.patchValue(c as any); this.initLoading.set(false); },
        error: () => this.initLoading.set(false),
      });
    }
  }

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    const body = this.form.getRawValue();
    if (!this.isAdmin) delete (body as any).managerId;

    const req$ = this.isEdit
      ? this.api.put<Club>(`/clubs/${this.clubId}`, body)
      : this.api.post<Club>('/clubs', body);
    req$.subscribe({
      next: club => {
        this.notify.success(this.isEdit ? 'Club updated.' : 'Club created.');
        this.router.navigate(['/admin/clubs', club.id]);
      },
      error: () => this.saving.set(false),
    });
  }

  cancel(): void {
    this.router.navigate(this.isEdit ? ['/admin/clubs', this.clubId] : ['/admin/clubs']);
  }
}
