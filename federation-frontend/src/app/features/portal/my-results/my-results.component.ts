import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { HttpErrorResponse } from '@angular/common/http';
import { switchMap } from 'rxjs/operators';

import { ApiService } from '@core/services/api.service';
import { AthleteDetail, Result } from '@core/models';
import { PageHeaderComponent } from '@shared/components/page-header/page-header.component';
import { StatusChipComponent } from '@shared/components/status-chip/status-chip.component';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';
import { EmptyStateComponent } from '@shared/components/empty-state/empty-state.component';
import { PerformancePipe } from '@shared/pipes';

interface PersonalBest {
  discipline: string;
  unit?:      string;
  value:      number;
  text?:      string;
  date:       string;
  competition: string;
}

@Component({
  selector:   'app-my-results',
  standalone: true,
  imports: [
    CommonModule, RouterModule, MatIconModule,
    PageHeaderComponent, StatusChipComponent, LoadingSpinnerComponent, EmptyStateComponent, PerformancePipe,
  ],
  template: `
    <app-page-header
      title="My Results"
      subtitle="Your competition results and personal bests"
      [breadcrumbs]="[{ label: 'Portal' }, { label: 'My Results' }]">
    </app-page-header>

    @if (loading()) {
      <app-loading-spinner message="Loading results…" />
    } @else if (noProfile()) {
      <div class="card">
        <app-empty-state icon="person_off" title="No athlete profile linked"
          subtitle="Your account is not linked to an athlete licence yet. Contact your club or the federation." />
      </div>
    } @else if (results().length === 0) {
      <div class="card">
        <app-empty-state icon="bar_chart" title="No results yet"
          subtitle="Your competition results will appear here once they are recorded." />
      </div>
    } @else {
      <div class="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <div class="stat-card"><div class="stat-icon bg-primary-500"><mat-icon class="text-white">sports_score</mat-icon></div>
          <div><p class="stat-value">{{ results().length }}</p><p class="stat-label">Results</p></div></div>
        <div class="stat-card"><div class="stat-icon bg-amber-400"><mat-icon class="text-white">workspace_premium</mat-icon></div>
          <div><p class="stat-value">{{ medals().gold }}</p><p class="stat-label">Gold</p></div></div>
        <div class="stat-card"><div class="stat-icon bg-slate-400"><mat-icon class="text-white">workspace_premium</mat-icon></div>
          <div><p class="stat-value">{{ medals().silver }}</p><p class="stat-label">Silver</p></div></div>
        <div class="stat-card"><div class="stat-icon bg-orange-400"><mat-icon class="text-white">workspace_premium</mat-icon></div>
          <div><p class="stat-value">{{ medals().bronze }}</p><p class="stat-label">Bronze</p></div></div>
      </div>

      <div class="card-padded mb-6">
        <h3 class="font-semibold text-surface-800 mb-3">Personal bests</h3>
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          @for (pb of personalBests(); track pb.discipline) {
            <div class="rounded-lg border border-surface-200 p-3">
              <p class="text-xs uppercase text-surface-400">{{ pb.discipline }}</p>
              <p class="text-2xl font-bold font-mono text-surface-900">{{ pb.text || (pb.value | performance:pb.unit) }}</p>
              <p class="text-xs text-surface-500">{{ pb.competition }} · {{ pb.date | date:'d MMM y' }}</p>
            </div>
          }
        </div>
      </div>

      <div class="card">
        <ul class="divide-y divide-surface-100">
          @for (r of results(); track r.id) {
            <li class="flex items-center gap-4 px-6 py-3">
              <span class="w-8 text-center font-bold text-surface-700">{{ r.rankPosition ?? '—' }}</span>
              <div class="flex-1 min-w-0">
                <p class="font-medium text-surface-900">{{ r.eventName }}</p>
                <a [routerLink]="['/competitions', r.competitionId]" class="text-xs text-surface-500 hover:text-primary-600">
                  {{ r.competitionName }}
                </a>
              </div>
              <span class="font-mono font-semibold">{{ r.performanceText || (r.performanceValue | performance:r.performanceUnit) }}</span>
              @if (r.medal) { <span class="text-xs font-semibold">{{ r.medal }}</span> }
              <app-status-chip [status]="r.status" />
            </li>
          }
        </ul>
      </div>
    }
  `,
})
export class MyResultsComponent implements OnInit {
  private api = inject(ApiService);

  results   = signal<Result[]>([]);
  loading   = signal(true);
  noProfile = signal(false);

  medals = computed(() => ({
    gold:   this.results().filter(r => r.medal === 'GOLD').length,
    silver: this.results().filter(r => r.medal === 'SILVER').length,
    bronze: this.results().filter(r => r.medal === 'BRONZE').length,
  }));

  /** Best mark per discipline: lowest time, highest distance / points. */
  personalBests = computed<PersonalBest[]>(() => {
    const best = new Map<string, PersonalBest>();
    for (const r of this.results()) {
      if (r.performanceValue === null || r.performanceValue === undefined) continue;
      if (!['OFFICIAL', 'UNOFFICIAL'].includes(r.status)) continue;
      const lowerIsBetter = r.performanceUnit === 'seconds';
      const current = best.get(r.discipline);
      const better = !current || (lowerIsBetter ? r.performanceValue < current.value : r.performanceValue > current.value);
      if (better) {
        best.set(r.discipline, {
          discipline: r.discipline, unit: r.performanceUnit, value: r.performanceValue,
          text: r.performanceText, date: r.createdAt, competition: r.competitionName,
        });
      }
    }
    return [...best.values()];
  });

  ngOnInit(): void {
    this.api.get<AthleteDetail>('/athletes/me').pipe(
      switchMap(me => this.api.getPaged<Result>('/results', { athleteId: me.id, size: 200, sort: 'createdAt,desc' })),
    ).subscribe({
      next: p => { this.results.set(p.content); this.loading.set(false); },
      error: (e: HttpErrorResponse) => { this.noProfile.set(e.status === 404); this.loading.set(false); },
    });
  }
}
