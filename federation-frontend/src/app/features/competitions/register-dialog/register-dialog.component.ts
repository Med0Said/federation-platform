import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { HttpClient } from '@angular/common/http';
import { map } from 'rxjs/operators';

import { ApiService } from '@core/services/api.service';
import { AuthService } from '@core/services/auth.service';
import { ApiResponse, AthleteDetail, Club, Competition, CompetitionEvent, Registration, UserRole } from '@core/models';
import { environment } from '@environments/environment';

export interface RegisterDialogData {
  competition: Competition;
  events:      CompetitionEvent[];
  eventId?:    string;
}

export interface RegisterDialogResult {
  registration: Registration;
  message:      string;
}

/**
 * Registers an athlete for an event.
 * Athletes register themselves; club managers pick one of their athletes;
 * staff can pick any athlete.
 */
@Component({
  selector: 'app-register-dialog',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatButtonModule, MatCheckboxModule, MatIconModule,
  ],
  template: `
    <h2 mat-dialog-title>Register — {{ data.competition.name }}</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <mat-dialog-content class="!pt-2 space-y-1">
        <mat-form-field appearance="outline" class="w-full">
          <mat-label>Event</mat-label>
          <mat-select formControlName="eventId">
            @for (e of data.events; track e.id) {
              <mat-option [value]="e.id">
                {{ e.name }}
                @if (e.maxParticipants) { — {{ e.registrationCount }}/{{ e.maxParticipants }} }
              </mat-option>
            }
          </mat-select>
          @if (selectedEvent(); as ev) {
            @if (ev.maxParticipants && ev.registrationCount >= ev.maxParticipants) {
              <mat-hint class="!text-amber-600">Event is full — new entries join the waiting list.</mat-hint>
            }
          }
        </mat-form-field>

        @if (!selfService) {
          <mat-form-field appearance="outline" class="w-full">
            <mat-label>Athlete</mat-label>
            <mat-select formControlName="athleteId">
              @for (a of athletes(); track a.id) {
                <mat-option [value]="a.id">{{ a.lastName }} {{ a.firstName }} — {{ a.clubName || 'Independent' }}</mat-option>
              }
            </mat-select>
            @if (athletes().length === 0) { <mat-hint>No eligible athletes found.</mat-hint> }
          </mat-form-field>
        }

        <div class="grid grid-cols-2 gap-3">
          <mat-form-field appearance="outline">
            <mat-label>Entry mark (seed)</mat-label>
            <input matInput type="number" step="0.01" min="0" formControlName="seedValue" />
            <mat-hint>Your best performance{{ selectedEvent()?.scoringUnit ? ' in ' + selectedEvent()!.scoringUnit : '' }}</mat-hint>
          </mat-form-field>
          <div class="flex items-center">
            <mat-checkbox formControlName="medicalWaiver">Medical waiver signed</mat-checkbox>
          </div>
        </div>

        <mat-form-field appearance="outline" class="w-full">
          <mat-label>Notes</mat-label>
          <textarea matInput rows="2" formControlName="notes"></textarea>
        </mat-form-field>

        @if (data.competition.entryFee) {
          <p class="text-sm text-surface-500 flex items-center gap-1">
            <mat-icon class="!text-base !w-4 !h-4">payments</mat-icon>
            Entry fee: {{ data.competition.entryFee }} {{ data.competition.currency }}
          </p>
        }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Cancel</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="saving()">
          {{ saving() ? 'Submitting…' : 'Register' }}
        </button>
      </mat-dialog-actions>
    </form>
  `,
})
export class RegisterDialogComponent implements OnInit {
  readonly data  = inject<RegisterDialogData>(MAT_DIALOG_DATA);
  private ref    = inject(MatDialogRef<RegisterDialogComponent, RegisterDialogResult>);
  private api    = inject(ApiService);
  private http   = inject(HttpClient);
  private auth   = inject(AuthService);
  private fb     = inject(FormBuilder);

  /** Athletes register themselves — the API infers the athlete from the account. */
  readonly selfService = this.auth.hasRole(UserRole.ATHLETE);

  athletes = signal<AthleteDetail[]>([]);
  saving   = signal(false);

  form = this.fb.group({
    eventId:       [this.data.eventId ?? null as string | null, Validators.required],
    athleteId:     [null as string | null, this.selfService ? [] : [Validators.required]],
    seedValue:     [null as number | null],
    medicalWaiver: [false],
    notes:         [''],
  });

  private eventIdValue = signal(this.form.controls.eventId.value);
  selectedEvent = computed(() => this.data.events.find(e => e.id === this.eventIdValue()));

  ngOnInit(): void {
    this.form.controls.eventId.valueChanges.subscribe(v => this.eventIdValue.set(v));
    if (this.selfService) return;

    if (this.auth.hasRole(UserRole.CLUB_MANAGER)) {
      // Only athletes of the clubs this manager runs
      this.api.get<Club[]>('/clubs/mine').subscribe(clubs => {
        clubs.forEach(c =>
          this.api.getPaged<AthleteDetail>('/athletes', { clubId: c.id, status: 'ACTIVE', size: 200, sort: 'lastName,asc' })
            .subscribe(p => this.athletes.update(list => [...list, ...p.content])));
      });
    } else {
      this.api.getPaged<AthleteDetail>('/athletes', { status: 'ACTIVE', size: 200, sort: 'lastName,asc' })
        .subscribe(p => this.athletes.set(p.content));
    }
  }

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    const v = this.form.getRawValue();
    const body: Record<string, unknown> = {
      eventId: v.eventId, seedValue: v.seedValue, medicalWaiver: v.medicalWaiver, notes: v.notes || null,
    };
    if (!this.selfService) body['athleteId'] = v.athleteId;

    // Raw call: the success message tells whether the athlete was waitlisted
    this.http.post<ApiResponse<Registration>>(
      `${environment.apiBaseUrl}/competitions/${this.data.competition.id}/registrations`, body)
      .pipe(map(r => ({ registration: r.data, message: r.message ?? 'Registration received.' })))
      .subscribe({
        next:  result => this.ref.close(result),
        error: () => this.saving.set(false),
      });
  }
}
