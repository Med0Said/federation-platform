import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormArray, FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { concat, Observable, of } from 'rxjs';
import { catchError, toArray } from 'rxjs/operators';

import { ApiService } from '@core/services/api.service';
import { NotificationService } from '@core/services/notification.service';
import {
  AthleteDetail, Competition, CompetitionEvent, humanize, Registration, Result, RESULT_STATUSES,
} from '@core/models';
import { PageHeaderComponent } from '@shared/components/page-header/page-header.component';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';
import { EmptyStateComponent } from '@shared/components/empty-state/empty-state.component';

type Row = FormGroup<{
  resultId:         FormControl<string | null>;
  athleteId:        FormControl<string>;
  athleteName:      FormControl<string>;
  clubName:         FormControl<string>;
  laneNumber:       FormControl<number | null>;
  performanceValue: FormControl<number | null>;
  performanceText:  FormControl<string>;
  windSpeed:        FormControl<number | null>;
  status:           FormControl<string>;
  personalBest:     FormControl<boolean>;
}>;

/**
 * Two modes:
 *  - /admin/results/:id/edit → edit one result
 *  - /admin/results/new      → enter all results of an event round at once; rows are
 *    pre-filled with existing results and the athletes registered for the event.
 */
@Component({
  selector: 'app-result-form',
  standalone: true,
  imports: [
    CommonModule, RouterModule, ReactiveFormsModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, MatIconModule, MatCheckboxModule,
    PageHeaderComponent, LoadingSpinnerComponent, EmptyStateComponent,
  ],
  template: `
    <app-page-header
      [title]="editId ? 'Edit Result' : 'Enter Results'"
      [subtitle]="editId ? 'Rankings are recalculated when you save' : 'Record the performances of an event round'"
      [breadcrumbs]="[
        { label: 'Admin', path: '/admin' },
        { label: 'Results', path: '/admin/results' },
        { label: editId ? 'Edit' : 'Enter' }
      ]">
    </app-page-header>

    <div class="card-padded mb-4">
      <div class="flex flex-wrap gap-3">
        <mat-form-field appearance="outline" class="flex-1 min-w-64">
          <mat-label>Competition</mat-label>
          <mat-select [formControl]="competitionCtrl">
            @for (c of competitions(); track c.id) { <mat-option [value]="c.id">{{ c.name }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline" class="w-72">
          <mat-label>Event</mat-label>
          <mat-select [formControl]="eventCtrl">
            @for (e of events(); track e.id) { <mat-option [value]="e.id">{{ e.name }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline" class="w-40">
          <mat-label>Round</mat-label>
          <input matInput [formControl]="roundCtrl" placeholder="FINAL" />
        </mat-form-field>
      </div>
      @if (event(); as ev) {
        <p class="text-xs text-surface-500 -mt-2">
          Scoring in {{ ev.scoringUnit || 'units' }} — {{ ev.lowerIsBetter ? 'lowest value wins' : 'highest value wins' }}.
          Leave the value empty for DNS / DNF / DSQ.
        </p>
      }
    </div>

    <div class="card relative">
      @if (loading()) { <app-loading-spinner [overlay]="true" /> }
      @if (!eventCtrl.value) {
        <app-empty-state icon="edit_note" title="Choose an event" subtitle="Select a competition and an event to enter results." />
      } @else {
        <form [formGroup]="form" (ngSubmit)="save()">
          <div class="table-container">
            <table class="w-full text-sm">
              <thead class="bg-surface-50 text-xs uppercase text-surface-500">
                <tr>
                  <th class="text-left px-4 py-2">Athlete</th>
                  <th class="px-2 py-2 w-20">Lane</th>
                  <th class="px-2 py-2 w-32">Value</th>
                  <th class="px-2 py-2 w-32">Display</th>
                  <th class="px-2 py-2 w-24">Wind</th>
                  <th class="px-2 py-2 w-40">Status</th>
                  <th class="px-2 py-2 w-16">PB</th>
                  <th class="w-12"></th>
                </tr>
              </thead>
              <tbody formArrayName="rows">
                @for (row of rows.controls; track $index; let i = $index) {
                  <tr [formGroupName]="i" class="border-t border-surface-100">
                    <td class="px-4 py-1">
                      <p class="font-medium text-surface-900">{{ row.value.athleteName }}</p>
                      <p class="text-xs text-surface-400">{{ row.value.clubName }}{{ row.value.resultId ? ' · recorded' : '' }}</p>
                    </td>
                    <td class="px-2"><input class="cell-input" type="number" min="1" formControlName="laneNumber" /></td>
                    <td class="px-2"><input class="cell-input" type="number" step="0.001" min="0" formControlName="performanceValue" /></td>
                    <td class="px-2"><input class="cell-input" formControlName="performanceText" placeholder="auto" /></td>
                    <td class="px-2"><input class="cell-input" type="number" step="0.1" formControlName="windSpeed" /></td>
                    <td class="px-2">
                      <select class="cell-input" formControlName="status">
                        @for (s of statuses; track s) { <option [value]="s">{{ humanize(s) }}</option> }
                      </select>
                    </td>
                    <td class="px-2 text-center"><mat-checkbox formControlName="personalBest" /></td>
                    <td>
                      @if (!row.value.resultId) {
                        <button mat-icon-button type="button" (click)="rows.removeAt(i)"><mat-icon>close</mat-icon></button>
                      }
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>

          <div class="flex flex-wrap items-center gap-3 p-4 border-t border-surface-100">
            @if (!editId) {
              <mat-form-field appearance="outline" class="w-80" subscriptSizing="dynamic">
                <mat-label>Add athlete</mat-label>
                <mat-select [formControl]="addAthleteCtrl">
                  @for (a of addableAthletes(); track a.id) {
                    <mat-option [value]="a.id">{{ a.lastName }} {{ a.firstName }} — {{ a.clubName || 'Independent' }}</mat-option>
                  }
                </mat-select>
              </mat-form-field>
            }
            <span class="flex-1"></span>
            <button mat-stroked-button type="button" routerLink="/admin/results">Cancel</button>
            <button mat-flat-button color="primary" type="submit" [disabled]="saving() || rows.length === 0">
              <mat-icon>save</mat-icon> {{ saving() ? 'Saving…' : 'Save results' }}
            </button>
          </div>
        </form>
      }
    </div>
  `,
  styles: [`
    .cell-input {
      width: 100%; padding: 6px 8px; border: 1px solid rgb(226 232 240);
      border-radius: 6px; background: transparent; font: inherit;
    }
    .cell-input:focus { outline: 2px solid rgb(59 130 246 / .4); border-color: transparent; }
  `],
})
export class ResultFormComponent implements OnInit {
  private api    = inject(ApiService);
  private notify = inject(NotificationService);
  private router = inject(Router);
  private route  = inject(ActivatedRoute);
  private fb     = inject(FormBuilder);
  readonly statuses = RESULT_STATUSES;
  readonly humanize = humanize;

  editId = this.route.snapshot.paramMap.get('id');

  competitions = signal<Competition[]>([]);
  events       = signal<CompetitionEvent[]>([]);
  athletes     = signal<AthleteDetail[]>([]);
  loading      = signal(false);
  saving       = signal(false);
  private rowAthleteIds = signal<string[]>([]);

  competitionCtrl = new FormControl<string | null>(null);
  eventCtrl       = new FormControl<string | null>(null);
  roundCtrl       = new FormControl('FINAL', { nonNullable: true });
  addAthleteCtrl  = new FormControl<string | null>(null);
  form = this.fb.group({ rows: this.fb.array<Row>([]) });
  get rows(): FormArray<Row> { return this.form.controls.rows; }

  private eventId = signal<string | null>(null);
  event = computed(() => this.events().find(e => e.id === this.eventId()));
  addableAthletes = computed(() => {
    const taken = new Set(this.rowAthleteIds());
    const ev = this.event();
    return this.athletes().filter(a => !taken.has(a.id)
      && (!ev?.genderCategory || ev.genderCategory === a.gender)
      && (!ev?.ageCategory || ev.ageCategory === a.category));
  });

  ngOnInit(): void {
    this.api.getPaged<Competition>('/competitions', { size: 100, sort: 'startDate,desc' })
      .subscribe(p => this.competitions.set(p.content));
    this.api.getPaged<AthleteDetail>('/athletes', { status: 'ACTIVE', size: 200, sort: 'lastName,asc' })
      .subscribe(p => this.athletes.set(p.content));

    this.competitionCtrl.valueChanges.subscribe(id => {
      this.events.set([]);
      if (id) this.api.get<CompetitionEvent[]>(`/competitions/${id}/events`).subscribe(e => this.events.set(e));
    });
    this.eventCtrl.valueChanges.subscribe(id => { this.eventId.set(id); if (!this.editId) this.loadRows(); });
    this.roundCtrl.valueChanges.subscribe(() => { if (!this.editId) this.loadRows(); });
    this.addAthleteCtrl.valueChanges.subscribe(id => {
      const a = this.athletes().find(x => x.id === id);
      if (a) { this.addRow({ athleteId: a.id, athleteName: `${a.firstName} ${a.lastName}`, clubName: a.clubName ?? '' }); }
      this.addAthleteCtrl.setValue(null, { emitEvent: false });
    });

    if (this.editId) {
      this.loading.set(true);
      this.api.get<Result>(`/results/${this.editId}`).subscribe(r => {
        this.competitionCtrl.setValue(r.competitionId);
        this.eventCtrl.setValue(r.eventId, { emitEvent: false });
        this.eventId.set(r.eventId);
        this.roundCtrl.setValue(r.round, { emitEvent: false });
        this.competitionCtrl.disable(); this.eventCtrl.disable(); this.roundCtrl.disable();
        this.addRow(fromResult(r));
        this.loading.set(false);
      });
    } else {
      const q = this.route.snapshot.queryParamMap;
      if (q.get('competitionId')) this.competitionCtrl.setValue(q.get('competitionId'));
      if (q.get('eventId')) this.eventCtrl.setValue(q.get('eventId'));
    }
  }

  /** Existing results of the round first, then registered athletes without a result. */
  private loadRows(): void {
    this.rows.clear();
    this.rowAthleteIds.set([]);
    const eventId = this.eventCtrl.value;
    const competitionId = this.competitionCtrl.value;
    if (!eventId || !competitionId) return;
    this.loading.set(true);
    const round = (this.roundCtrl.value || 'FINAL').toUpperCase();

    this.api.get<Result[]>(`/results/event/${eventId}`, { round }).subscribe(results => {
      results.forEach(r => this.addRow(fromResult(r)));
      this.api.getPaged<Registration>(`/competitions/${competitionId}/registrations`, { eventId, size: 500 })
        .subscribe({
          next: p => {
            p.content
              .filter(reg => reg.status === 'CONFIRMED' || reg.status === 'PENDING')
              .filter(reg => !results.some(r => r.athleteId === reg.athleteId))
              .forEach(reg => this.addRow({ athleteId: reg.athleteId, athleteName: reg.athleteName, clubName: reg.clubName ?? '' }));
            this.loading.set(false);
          },
          error: () => this.loading.set(false),
        });
    });
  }

  private addRow(v: Partial<Row['value']> & { athleteId: string; athleteName: string }): void {
    this.rows.push(this.fb.group({
      resultId:         new FormControl<string | null>(v.resultId ?? null),
      athleteId:        new FormControl(v.athleteId, { nonNullable: true, validators: Validators.required }),
      athleteName:      new FormControl(v.athleteName, { nonNullable: true }),
      clubName:         new FormControl(v.clubName ?? '', { nonNullable: true }),
      laneNumber:       new FormControl<number | null>(v.laneNumber ?? null),
      performanceValue: new FormControl<number | null>(v.performanceValue ?? null),
      performanceText:  new FormControl(v.performanceText ?? '', { nonNullable: true }),
      windSpeed:        new FormControl<number | null>(v.windSpeed ?? null),
      status:           new FormControl(v.status ?? 'UNOFFICIAL', { nonNullable: true }),
      personalBest:     new FormControl(v.personalBest ?? false, { nonNullable: true }),
    }) as Row);
    this.rowAthleteIds.update(ids => [...ids, v.athleteId]);
  }

  save(): void {
    const competitionId = this.competitionCtrl.value!;
    const eventId = this.eventCtrl.value!;
    const round = (this.roundCtrl.value || 'FINAL').toUpperCase();
    const unmarked = ['DNS', 'DNF', 'DSQ', 'DISQUALIFIED'];

    const requests: Observable<unknown>[] = this.rows.getRawValue()
      // Skip rows nobody filled in: no mark and still the default status
      .filter(r => r.resultId || r.performanceValue !== null || unmarked.includes(r.status))
      .map(r => {
        const body = {
          competitionId, eventId, round,
          athleteId: r.athleteId,
          laneNumber: r.laneNumber,
          performanceValue: r.performanceValue,
          performanceText: r.performanceText || (r.performanceValue !== null ? String(r.performanceValue) : r.status),
          windSpeed: r.windSpeed,
          status: r.status,
          personalBest: r.personalBest,
        };
        const req$ = r.resultId ? this.api.put(`/results/${r.resultId}`, body) : this.api.post('/results', body);
        return req$.pipe(catchError(() => of('failed')));
      });

    if (requests.length === 0) { this.notify.warning('Nothing to save — enter at least one performance.'); return; }
    this.saving.set(true);
    // Sequential so each ranking recalculation sees the previous rows
    concat(...requests).pipe(toArray()).subscribe(outcomes => {
      this.saving.set(false);
      const failed = outcomes.filter(o => o === 'failed').length;
      if (failed) {
        this.notify.error(`${outcomes.length - failed} saved, ${failed} failed — check the highlighted errors.`);
      } else {
        this.notify.success(`${outcomes.length} result(s) saved. Rankings updated.`);
        this.router.navigate(['/admin/results'], { queryParams: { competitionId, eventId } });
      }
      if (!this.editId) this.loadRows();
    });
  }
}

function fromResult(r: Result) {
  return {
    resultId: r.id, athleteId: r.athleteId, athleteName: r.athleteName, clubName: r.clubName ?? '',
    laneNumber: r.laneNumber ?? null, performanceValue: r.performanceValue ?? null,
    performanceText: r.performanceText ?? '', windSpeed: r.windSpeed ?? null,
    status: r.status, personalBest: r.personalBest,
  };
}
