import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule }   from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule }     from '@angular/material/input';
import { MatSelectModule }    from '@angular/material/select';
import { MatDatepickerModule }from '@angular/material/datepicker';
import { MatButtonModule }    from '@angular/material/button';
import { MatIconModule }      from '@angular/material/icon';

import { ApiService }          from '@core/services/api.service';
import { NotificationService } from '@core/services/notification.service';
import { PageHeaderComponent } from '@shared/components/page-header/page-header.component';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';

@Component({
  selector:   'app-athlete-form',
  standalone: true,
  imports: [
    CommonModule, RouterModule, ReactiveFormsModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatDatepickerModule, MatButtonModule, MatIconModule,
    PageHeaderComponent, LoadingSpinnerComponent,
  ],
  template: `
    <app-page-header
      [title]="isEdit ? 'Edit Athlete' : 'Add Athlete'"
      [subtitle]="isEdit ? 'Update athlete information' : 'Register a new federation athlete'"
      [breadcrumbs]="[
        { label: 'Admin', path: '/admin' },
        { label: 'Athletes', path: '/admin/athletes' },
        { label: isEdit ? 'Edit' : 'New' }
      ]">
    </app-page-header>

    @if (initLoading()) {
      <app-loading-spinner message="Loading athlete…" />
    } @else {
      <div class="max-w-2xl">
        <form [formGroup]="form" (ngSubmit)="onSubmit()" class="card-padded space-y-4">

          <h3 class="font-semibold text-surface-800 border-b border-surface-100 pb-3">
            Personal Information
          </h3>

          <div class="grid grid-cols-2 gap-4">
            <mat-form-field appearance="outline">
              <mat-label>First Name</mat-label>
              <input matInput formControlName="firstName" />
              @if (f['firstName'].hasError('required') && f['firstName'].touched) {
                <mat-error>Required</mat-error>
              }
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Last Name</mat-label>
              <input matInput formControlName="lastName" />
              @if (f['lastName'].hasError('required') && f['lastName'].touched) {
                <mat-error>Required</mat-error>
              }
            </mat-form-field>
          </div>

          <div class="grid grid-cols-2 gap-4">
            <mat-form-field appearance="outline">
              <mat-label>Date of Birth</mat-label>
              <input matInput type="date" formControlName="dateOfBirth" />
              @if (f['dateOfBirth'].hasError('required') && f['dateOfBirth'].touched) {
                <mat-error>Required</mat-error>
              }
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Gender</mat-label>
              <mat-select formControlName="gender">
                <mat-option value="MALE">Male</mat-option>
                <mat-option value="FEMALE">Female</mat-option>
                <mat-option value="OTHER">Other</mat-option>
              </mat-select>
            </mat-form-field>
          </div>

          <div class="grid grid-cols-2 gap-4">
            <mat-form-field appearance="outline">
              <mat-label>Nationality</mat-label>
              <input matInput formControlName="nationality" />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Country Code (IOC)</mat-label>
              <input matInput formControlName="countryCode" maxlength="3" placeholder="TUN" />
            </mat-form-field>
          </div>

          <h3 class="font-semibold text-surface-800 border-b border-surface-100 pb-3 pt-2">
            Physical & Contact
          </h3>

          <div class="grid grid-cols-2 gap-4">
            <mat-form-field appearance="outline">
              <mat-label>Weight (kg)</mat-label>
              <input matInput type="number" formControlName="weightKg" step="0.1" />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Height (cm)</mat-label>
              <input matInput type="number" formControlName="heightCm" step="0.1" />
            </mat-form-field>
          </div>

          <div class="grid grid-cols-2 gap-4">
            <mat-form-field appearance="outline">
              <mat-label>Email</mat-label>
              <input matInput type="email" formControlName="email" />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Phone</mat-label>
              <input matInput type="tel" formControlName="phone" />
            </mat-form-field>
          </div>

          <mat-form-field appearance="outline" class="w-full">
            <mat-label>Status</mat-label>
            <mat-select formControlName="status">
              <mat-option value="ACTIVE">Active</mat-option>
              <mat-option value="INACTIVE">Inactive</mat-option>
              <mat-option value="INJURED">Injured</mat-option>
              <mat-option value="SUSPENDED">Suspended</mat-option>
              <mat-option value="RETIRED">Retired</mat-option>
            </mat-select>
          </mat-form-field>

          <div class="flex justify-end gap-3 pt-4 border-t border-surface-100">
            <a mat-stroked-button routerLink="/admin/athletes">Cancel</a>
            <button mat-flat-button color="primary" type="submit" [disabled]="saving()">
              @if (saving()) { Saving… } @else { {{ isEdit ? 'Update Athlete' : 'Create Athlete' }} }
            </button>
          </div>
        </form>
      </div>
    }
  `,
})
export class AthleteFormComponent implements OnInit {
  private fb     = inject(FormBuilder);
  private api    = inject(ApiService);
  private notify = inject(NotificationService);
  private router = inject(Router);
  private route  = inject(ActivatedRoute);

  saving      = signal(false);
  initLoading = signal(false);
  isEdit      = false;
  athleteId?: string;

  form = this.fb.group({
    firstName:   ['', Validators.required],
    lastName:    ['', Validators.required],
    dateOfBirth: ['', Validators.required],
    gender:      ['MALE', Validators.required],
    nationality: ['Tunisian'],
    countryCode: ['TUN'],
    weightKg:    [null as number | null],
    heightCm:    [null as number | null],
    email:       [''],
    phone:       [''],
    status:      ['ACTIVE'],
  });

  get f() { return this.form.controls; }

  ngOnInit(): void {
    this.athleteId = this.route.snapshot.paramMap.get('id') ?? undefined;
    this.isEdit    = !!this.athleteId;

    if (this.isEdit) {
      this.initLoading.set(true);
      this.api.get<any>(`/athletes/${this.athleteId}`).subscribe({
        next:  a => { this.form.patchValue(a); this.initLoading.set(false); },
        error: () => this.initLoading.set(false),
      });
    }
  }

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);

    const req$ = this.isEdit
      ? this.api.put(`/athletes/${this.athleteId}`, this.form.value)
      : this.api.post('/athletes', this.form.value);

    req$.subscribe({
      next: () => {
        this.notify.success(this.isEdit ? 'Athlete updated.' : 'Athlete created.');
        this.router.navigate(['/admin/athletes']);
      },
      error: () => this.saving.set(false),
    });
  }
}
