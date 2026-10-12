import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { ApiService } from '@core/services/api.service';
import { NotificationService } from '@core/services/notification.service';
import { Club, Competition, COMPETITION_STATUSES, humanize } from '@core/models';
import { PageHeaderComponent } from '@shared/components/page-header/page-header.component';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';

/** End date ≥ start date and registration deadline ≤ start date. */
function dateOrder(group: AbstractControl): ValidationErrors | null {
  const { startDate, endDate, registrationDeadline } = group.value;
  const errors: ValidationErrors = {};
  if (startDate && endDate && endDate < startDate) errors['endBeforeStart'] = true;
  if (startDate && registrationDeadline && registrationDeadline > startDate) errors['deadlineAfterStart'] = true;
  return Object.keys(errors).length ? errors : null;
}

@Component({
  selector: 'app-competition-form',
  standalone: true,
  imports: [
    CommonModule, RouterModule, ReactiveFormsModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, MatIconModule,
    PageHeaderComponent, LoadingSpinnerComponent,
  ],
  template: `
    <app-page-header
      [title]="isEdit ? 'Edit Competition' : 'New Competition'"
      [subtitle]="isEdit ? 'Update competition details' : 'Events can be added once the competition is created'"
      [breadcrumbs]="[
        { label: 'Admin', path: '/admin' },
        { label: 'Competitions', path: '/admin/competitions' },
        { label: isEdit ? 'Edit' : 'New' }
      ]">
    </app-page-header>

    @if (initLoading()) {
      <app-loading-spinner message="Loading competition…" />
    } @else {
      <form [formGroup]="form" (ngSubmit)="onSubmit()" class="card-padded space-y-4 max-w-3xl">
        <h3 class="font-semibold text-surface-800 border-b border-surface-100 pb-3">General</h3>

        <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <mat-form-field appearance="outline" class="sm:col-span-2">
            <mat-label>Name</mat-label>
            <input matInput formControlName="name" />
            @if (f.name.hasError('required')) { <mat-error>Required</mat-error> }
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Edition</mat-label>
            <input matInput formControlName="edition" placeholder="2025" />
          </mat-form-field>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-4 gap-4">
          <mat-form-field appearance="outline">
            <mat-label>Sport</mat-label>
            <input matInput formControlName="sport" />
            @if (f.sport.hasError('required')) { <mat-error>Required</mat-error> }
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Level</mat-label>
            <mat-select formControlName="level">
              @for (l of levels; track l) { <mat-option [value]="l">{{ humanize(l) }}</mat-option> }
            </mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Format</mat-label>
            <mat-select formControlName="format">
              @for (fm of formats; track fm) { <mat-option [value]="fm">{{ humanize(fm) }}</mat-option> }
            </mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Status</mat-label>
            <mat-select formControlName="status">
              @for (s of statuses; track s) { <mat-option [value]="s">{{ humanize(s) }}</mat-option> }
            </mat-select>
          </mat-form-field>
        </div>

        <mat-form-field appearance="outline" class="w-full">
          <mat-label>Description</mat-label>
          <textarea matInput rows="4" formControlName="description"></textarea>
        </mat-form-field>

        <h3 class="font-semibold text-surface-800 border-b border-surface-100 pb-3 pt-2">Schedule & venue</h3>

        <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <mat-form-field appearance="outline">
            <mat-label>Start date</mat-label>
            <input matInput type="date" formControlName="startDate" />
            @if (f.startDate.hasError('required')) { <mat-error>Required</mat-error> }
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>End date</mat-label>
            <input matInput type="date" formControlName="endDate" />
            @if (f.endDate.hasError('required')) { <mat-error>Required</mat-error> }
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Registration deadline</mat-label>
            <input matInput type="date" formControlName="registrationDeadline" />
          </mat-form-field>
        </div>
        @if (form.hasError('endBeforeStart')) {
          <p class="text-sm text-red-600 -mt-2">The end date must be on or after the start date.</p>
        }
        @if (form.hasError('deadlineAfterStart')) {
          <p class="text-sm text-red-600 -mt-2">The registration deadline must be before the start date.</p>
        }

        <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <mat-form-field appearance="outline">
            <mat-label>Venue</mat-label>
            <input matInput formControlName="venueName" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>City</mat-label>
            <input matInput formControlName="venueCity" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Host club</mat-label>
            <mat-select formControlName="hostClubId">
              <mat-option [value]="null">— None —</mat-option>
              @for (club of clubs(); track club.id) { <mat-option [value]="club.id">{{ club.name }}</mat-option> }
            </mat-select>
          </mat-form-field>
        </div>

        <h3 class="font-semibold text-surface-800 border-b border-surface-100 pb-3 pt-2">Registration rules</h3>

        <div class="grid grid-cols-1 sm:grid-cols-4 gap-4">
          <mat-form-field appearance="outline">
            <mat-label>Max participants</mat-label>
            <input matInput type="number" min="1" formControlName="maxParticipants" />
            <mat-hint>Empty = unlimited</mat-hint>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Max per club</mat-label>
            <input matInput type="number" min="1" formControlName="maxPerClub" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Entry fee</mat-label>
            <input matInput type="number" min="0" step="0.5" formControlName="entryFee" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Currency</mat-label>
            <input matInput formControlName="currency" maxlength="3" />
          </mat-form-field>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <mat-form-field appearance="outline">
            <mat-label>Rules URL</mat-label>
            <input matInput formControlName="rulesUrl" placeholder="https://" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Poster URL</mat-label>
            <input matInput formControlName="posterUrl" placeholder="https://" />
          </mat-form-field>
        </div>

        <mat-form-field appearance="outline" class="w-full">
          <mat-label>Prizes</mat-label>
          <textarea matInput rows="2" formControlName="prizeInfo"></textarea>
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
export class CompetitionFormComponent implements OnInit {
  private fb     = inject(FormBuilder);
  private api    = inject(ApiService);
  private notify = inject(NotificationService);
  private router = inject(Router);
  private route  = inject(ActivatedRoute);

  readonly statuses = COMPETITION_STATUSES;
  readonly levels   = ['LOCAL', 'REGIONAL', 'NATIONAL', 'INTERNATIONAL'];
  readonly formats  = ['INDIVIDUAL', 'TEAM', 'RELAY', 'MIXED'];
  readonly humanize = humanize;

  saving      = signal(false);
  initLoading = signal(false);
  clubs       = signal<Club[]>([]);
  isEdit      = false;
  competitionId?: string;

  form = this.fb.group({
    name:                 ['', [Validators.required, Validators.maxLength(200)]],
    edition:              [''],
    sport:                ['Athletics', Validators.required],
    level:                ['NATIONAL'],
    format:               ['INDIVIDUAL'],
    status:               ['DRAFT'],
    description:          [''],
    startDate:            ['', Validators.required],
    endDate:              ['', Validators.required],
    registrationDeadline: [''],
    venueName:            [''],
    venueCity:            [''],
    hostClubId:           [null as string | null],
    maxParticipants:      [null as number | null],
    maxPerClub:           [null as number | null],
    entryFee:             [null as number | null],
    currency:             ['TND'],
    rulesUrl:             [''],
    posterUrl:            [''],
    prizeInfo:            [''],
  }, { validators: dateOrder });

  get f() { return this.form.controls; }

  ngOnInit(): void {
    this.competitionId = this.route.snapshot.paramMap.get('id') ?? undefined;
    this.isEdit = !!this.competitionId;
    this.api.getPaged<Club>('/clubs', { size: 200, sort: 'name,asc', status: 'ACTIVE' })
      .subscribe(p => this.clubs.set(p.content));

    if (this.isEdit) {
      this.initLoading.set(true);
      this.api.get<Competition>(`/competitions/${this.competitionId}`).subscribe({
        next: c => { this.form.patchValue(c as any); this.initLoading.set(false); },
        error: () => this.initLoading.set(false),
      });
    }
  }

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    const v = this.form.getRawValue();
    const body = { ...v, registrationDeadline: v.registrationDeadline || null };
    const req$ = this.isEdit
      ? this.api.put<Competition>(`/competitions/${this.competitionId}`, body)
      : this.api.post<Competition>('/competitions', body);
    req$.subscribe({
      next: c => {
        this.notify.success(this.isEdit ? 'Competition updated.' : 'Competition created — now add its events.');
        this.router.navigate(['/admin/competitions', c.id]);
      },
      error: () => this.saving.set(false),
    });
  }

  cancel(): void {
    this.router.navigate(this.isEdit ? ['/admin/competitions', this.competitionId] : ['/admin/competitions']);
  }
}
