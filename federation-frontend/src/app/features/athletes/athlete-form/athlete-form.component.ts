import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule }   from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule }    from '@angular/material/input';
import { MatSelectModule }   from '@angular/material/select';
import { MatButtonModule }   from '@angular/material/button';
import { MatIconModule }     from '@angular/material/icon';

import { ApiService }          from '@core/services/api.service';
import { AuthService }         from '@core/services/auth.service';
import { NotificationService } from '@core/services/notification.service';
import { AthleteDetail, Club, ManagedUser, UserRole } from '@core/models';
import { PageHeaderComponent }     from '@shared/components/page-header/page-header.component';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';

@Component({
  selector:   'app-athlete-form',
  standalone: true,
  imports: [
    CommonModule, RouterModule, ReactiveFormsModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, MatIconModule,
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
      <div class="max-w-3xl">
        <form [formGroup]="form" (ngSubmit)="onSubmit()" class="card-padded space-y-4">

          <h3 class="font-semibold text-surface-800 border-b border-surface-100 pb-3">Licence & club</h3>

          <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <mat-form-field appearance="outline">
              <mat-label>Licence number</mat-label>
              <input matInput formControlName="licenseNumber" placeholder="ATH-2025-00001" />
              @if (f['licenseNumber'].hasError('required') && f['licenseNumber'].touched) {
                <mat-error>Required</mat-error>
              }
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Licence expiry</mat-label>
              <input matInput type="date" formControlName="licenseExpiry" />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Status</mat-label>
              <mat-select formControlName="status">
                <mat-option value="ACTIVE">Active</mat-option>
                <mat-option value="INACTIVE">Inactive</mat-option>
                <mat-option value="INJURED">Injured</mat-option>
                <mat-option value="SUSPENDED">Suspended</mat-option>
                <mat-option value="RETIRED">Retired</mat-option>
              </mat-select>
            </mat-form-field>
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <mat-form-field appearance="outline">
              <mat-label>Club</mat-label>
              <mat-select formControlName="clubId">
                @if (isStaff) { <mat-option [value]="null">Independent (no club)</mat-option> }
                @for (club of clubs(); track club.id) {
                  <mat-option [value]="club.id">{{ club.name }}</mat-option>
                }
              </mat-select>
              @if (!isStaff && isEdit) { <mat-hint>Club transfers are handled by the federation</mat-hint> }
              @if (f['clubId'].hasError('required') && f['clubId'].touched) { <mat-error>Choose your club</mat-error> }
            </mat-form-field>
            @if (isAdmin) {
              <mat-form-field appearance="outline">
                <mat-label>Portal account</mat-label>
                <mat-select formControlName="userId">
                  <mat-option [value]="null">— Not linked —</mat-option>
                  @for (u of athleteAccounts(); track u.id) {
                    <mat-option [value]="u.id">{{ u.fullName }} ({{ u.email }})</mat-option>
                  }
                </mat-select>
                <mat-hint>Lets the athlete register and follow results</mat-hint>
              </mat-form-field>
            }
          </div>

          <h3 class="font-semibold text-surface-800 border-b border-surface-100 pb-3 pt-2">Personal information</h3>

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

          <h3 class="font-semibold text-surface-800 border-b border-surface-100 pb-3 pt-2">Physical & contact</h3>

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
              @if (f['email'].hasError('email')) { <mat-error>Invalid e-mail</mat-error> }
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Phone</mat-label>
              <input matInput formControlName="phone" />
            </mat-form-field>
          </div>

          <div class="grid grid-cols-2 gap-4">
            <mat-form-field appearance="outline">
              <mat-label>Emergency contact</mat-label>
              <input matInput formControlName="emergencyContactName" />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Emergency phone</mat-label>
              <input matInput formControlName="emergencyContactPhone" />
            </mat-form-field>
          </div>

          <h3 class="font-semibold text-surface-800 border-b border-surface-100 pb-3 pt-2">Medical & notes</h3>

          <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <mat-form-field appearance="outline">
              <mat-label>Medical clearance</mat-label>
              <input matInput type="date" formControlName="medicalClearanceDate" />
            </mat-form-field>
            <mat-form-field appearance="outline" class="sm:col-span-2">
              <mat-label>Photo URL</mat-label>
              <input matInput formControlName="photoUrl" placeholder="https://" />
            </mat-form-field>
          </div>
          <mat-form-field appearance="outline" class="w-full">
            <mat-label>Medical notes (confidential)</mat-label>
            <textarea matInput rows="2" formControlName="medicalNotes"></textarea>
          </mat-form-field>
          <mat-form-field appearance="outline" class="w-full">
            <mat-label>Internal notes</mat-label>
            <textarea matInput rows="2" formControlName="notes"></textarea>
          </mat-form-field>

          <div class="flex justify-end gap-3 pt-2">
            <button mat-stroked-button type="button" (click)="cancel()">Cancel</button>
            <button mat-flat-button color="primary" type="submit" [disabled]="saving()">
              <mat-icon>save</mat-icon> {{ saving() ? 'Saving…' : 'Save' }}
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
  private auth   = inject(AuthService);
  private notify = inject(NotificationService);
  private router = inject(Router);
  private route  = inject(ActivatedRoute);

  readonly isStaff = this.auth.hasAnyRole([UserRole.ADMIN, UserRole.FEDERATION_STAFF]);
  readonly isAdmin = this.auth.hasRole(UserRole.ADMIN);

  saving          = signal(false);
  initLoading     = signal(false);
  clubs           = signal<Club[]>([]);
  athleteAccounts = signal<ManagedUser[]>([]);
  isEdit          = false;
  athleteId?: string;

  form = this.fb.group({
    licenseNumber:         ['', Validators.required],
    licenseExpiry:         [''],
    status:                ['ACTIVE'],
    clubId:                [null as string | null],
    userId:                [null as string | null],
    firstName:             ['', Validators.required],
    lastName:              ['', Validators.required],
    dateOfBirth:           ['', Validators.required],
    gender:                ['MALE', Validators.required],
    nationality:           ['Tunisian'],
    countryCode:           ['TUN'],
    weightKg:              [null as number | null],
    heightCm:              [null as number | null],
    email:                 ['', Validators.email],
    phone:                 [''],
    emergencyContactName:  [''],
    emergencyContactPhone: [''],
    medicalClearanceDate:  [''],
    medicalNotes:          [''],
    photoUrl:              [''],
    notes:                 [''],
  });

  get f() { return this.form.controls; }

  ngOnInit(): void {
    this.athleteId = this.route.snapshot.paramMap.get('id') ?? undefined;
    this.isEdit    = !!this.athleteId;

    if (this.isStaff) {
      this.api.getPaged<Club>('/clubs', { size: 200, sort: 'name,asc' }).subscribe(p => this.clubs.set(p.content));
    } else {
      // Club managers create athletes for (and only see) the clubs they manage
      this.f.clubId.addValidators(Validators.required);
      this.api.get<Club[]>('/clubs/mine').subscribe(clubs => {
        this.clubs.set(clubs);
        if (!this.isEdit && clubs.length === 1) this.f.clubId.setValue(clubs[0].id);
      });
      if (this.isEdit) this.f.clubId.disable();
    }
    if (this.isAdmin) {
      this.api.getPaged<ManagedUser>('/users', { role: UserRole.ATHLETE, size: 200, sort: 'lastName,asc' })
        .subscribe(p => this.athleteAccounts.set(p.content));
    }

    if (this.isEdit) {
      this.initLoading.set(true);
      this.api.get<AthleteDetail>(`/athletes/${this.athleteId}`).subscribe({
        next: a => {
          this.form.patchValue(a as any);
          // Managers may edit athletes of other clubs' history, but never move them
          if (!this.isStaff && a.clubId && !this.clubs().some(c => c.id === a.clubId)) {
            this.clubs.update(list => [...list, { id: a.clubId!, name: a.clubName ?? 'Current club' } as Club]);
          }
          this.initLoading.set(false);
        },
        error: () => this.initLoading.set(false),
      });
    }
  }

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);

    const v = this.form.getRawValue();
    // Optional dates must be sent as null, not empty strings
    const body = {
      ...v,
      licenseExpiry:        v.licenseExpiry || null,
      medicalClearanceDate: v.medicalClearanceDate || null,
      email:                v.email || null,
    };
    if (!this.isAdmin) delete (body as any).userId;

    const req$ = this.isEdit
      ? this.api.put<AthleteDetail>(`/athletes/${this.athleteId}`, body)
      : this.api.post<AthleteDetail>('/athletes', body);

    req$.subscribe({
      next: a => {
        this.notify.success(this.isEdit ? 'Athlete updated.' : 'Athlete created.');
        this.router.navigate(['/admin/athletes', a.id]);
      },
      error: () => this.saving.set(false),
    });
  }

  cancel(): void {
    this.router.navigate(this.isEdit ? ['/admin/athletes', this.athleteId] : ['/admin/athletes']);
  }
}
