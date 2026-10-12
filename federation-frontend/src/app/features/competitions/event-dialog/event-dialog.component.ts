import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';

import { ApiService } from '@core/services/api.service';
import { ATHLETE_CATEGORIES, COMPETITION_STATUSES, CompetitionEvent, humanize } from '@core/models';

export interface EventDialogData {
  competitionId: string;
  event?: CompetitionEvent;
}

/** Create / edit a discipline event of a competition. Closes with the saved event. */
@Component({
  selector: 'app-event-dialog',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatDialogModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, MatSlideToggleModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ data.event ? 'Edit event' : 'New event' }}</h2>
    <form [formGroup]="form" (ngSubmit)="save()">
      <mat-dialog-content class="!pt-2 space-y-1">
        <div class="grid grid-cols-3 gap-3">
          <mat-form-field appearance="outline" class="col-span-2">
            <mat-label>Name</mat-label>
            <input matInput formControlName="name" placeholder="100m Men Senior" />
            @if (form.controls.name.hasError('required')) { <mat-error>Required</mat-error> }
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Code</mat-label>
            <input matInput formControlName="code" placeholder="M-100-SR" />
          </mat-form-field>
        </div>
        <div class="grid grid-cols-2 gap-3">
          <mat-form-field appearance="outline">
            <mat-label>Discipline</mat-label>
            <input matInput formControlName="discipline" placeholder="100m Sprint" />
            @if (form.controls.discipline.hasError('required')) { <mat-error>Required</mat-error> }
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Status</mat-label>
            <mat-select formControlName="status">
              @for (s of statuses; track s) { <mat-option [value]="s">{{ humanize(s) }}</mat-option> }
            </mat-select>
          </mat-form-field>
        </div>
        <div class="grid grid-cols-2 gap-3">
          <mat-form-field appearance="outline">
            <mat-label>Gender</mat-label>
            <mat-select formControlName="genderCategory">
              <mat-option [value]="null">Open / mixed</mat-option>
              <mat-option value="MALE">Men</mat-option>
              <mat-option value="FEMALE">Women</mat-option>
            </mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Age category</mat-label>
            <mat-select formControlName="ageCategory">
              <mat-option [value]="null">All categories</mat-option>
              @for (c of categories; track c.value) { <mat-option [value]="c.value">{{ c.label }}</mat-option> }
            </mat-select>
          </mat-form-field>
        </div>
        <div class="grid grid-cols-3 gap-3">
          <mat-form-field appearance="outline">
            <mat-label>Scheduled at</mat-label>
            <input matInput type="datetime-local" formControlName="scheduledAt" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Duration (min)</mat-label>
            <input matInput type="number" min="1" formControlName="durationMinutes" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Max participants</mat-label>
            <input matInput type="number" min="1" formControlName="maxParticipants" />
            <mat-hint>Empty = unlimited</mat-hint>
          </mat-form-field>
        </div>
        <div class="grid grid-cols-2 gap-3 items-center">
          <mat-form-field appearance="outline">
            <mat-label>Scoring unit</mat-label>
            <mat-select formControlName="scoringUnit">
              <mat-option value="seconds">Seconds (time)</mat-option>
              <mat-option value="meters">Meters (distance / height)</mat-option>
              <mat-option value="points">Points</mat-option>
            </mat-select>
          </mat-form-field>
          <mat-slide-toggle formControlName="lowerIsBetter">Lower value wins</mat-slide-toggle>
        </div>
        <mat-form-field appearance="outline" class="w-full">
          <mat-label>Notes</mat-label>
          <textarea matInput rows="2" formControlName="notes"></textarea>
        </mat-form-field>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancel</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="saving()">
          {{ saving() ? 'Saving…' : 'Save' }}
        </button>
      </mat-dialog-actions>
    </form>
  `,
})
export class EventDialogComponent {
  readonly data   = inject<EventDialogData>(MAT_DIALOG_DATA);
  private ref     = inject(MatDialogRef<EventDialogComponent>);
  private api     = inject(ApiService);
  private fb      = inject(FormBuilder);

  readonly statuses   = COMPETITION_STATUSES;
  readonly categories = ATHLETE_CATEGORIES;
  readonly humanize   = humanize;
  saving = signal(false);

  form = this.fb.group({
    name:            [this.data.event?.name ?? '', Validators.required],
    code:            [this.data.event?.code ?? ''],
    discipline:      [this.data.event?.discipline ?? '', Validators.required],
    status:          [this.data.event?.status ?? 'DRAFT'],
    genderCategory:  [this.data.event?.genderCategory ?? null],
    ageCategory:     [this.data.event?.ageCategory ?? null],
    scheduledAt:     [toLocalInput(this.data.event?.scheduledAt)],
    durationMinutes: [this.data.event?.durationMinutes ?? null],
    maxParticipants: [this.data.event?.maxParticipants ?? null],
    scoringUnit:     [this.data.event?.scoringUnit ?? 'seconds'],
    lowerIsBetter:   [this.data.event?.lowerIsBetter ?? true],
    notes:           [this.data.event?.notes ?? ''],
  });

  constructor() {
    // Sensible default: times are ranked ascending, distances and points descending
    this.form.controls.scoringUnit.valueChanges.subscribe(unit => {
      if (!this.data.event) this.form.controls.lowerIsBetter.setValue(unit === 'seconds');
    });
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    const v = this.form.getRawValue();
    const body = {
      ...v,
      code: v.code || null,
      scheduledAt: v.scheduledAt ? new Date(v.scheduledAt).toISOString() : null,
    };
    const base = `/competitions/${this.data.competitionId}/events`;
    const req$ = this.data.event
      ? this.api.put<CompetitionEvent>(`${base}/${this.data.event.id}`, body)
      : this.api.post<CompetitionEvent>(base, body);
    req$.subscribe({
      next:  saved => this.ref.close(saved),
      error: () => this.saving.set(false),
    });
  }
}

/** ISO instant → value for <input type="datetime-local"> in the browser's time zone. */
function toLocalInput(iso?: string): string {
  if (!iso) return '';
  const d = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}
