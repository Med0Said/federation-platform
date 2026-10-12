import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';

import { ApiService } from '@core/services/api.service';
import { AuthService } from '@core/services/auth.service';
import { Competition, CompetitionEvent, humanize, Result, RESULT_STATUSES, UserRole } from '@core/models';
import { PageHeaderComponent } from '@shared/components/page-header/page-header.component';
import { StatusChipComponent } from '@shared/components/status-chip/status-chip.component';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';
import { EmptyStateComponent } from '@shared/components/empty-state/empty-state.component';
import { PerformancePipe } from '@shared/pipes';
import { pageContext } from '@shared/utils/page-context';

@Component({
  selector: 'app-results-list',
  standalone: true,
  imports: [
    CommonModule, RouterModule, ReactiveFormsModule,
    MatFormFieldModule, MatSelectModule, MatButtonModule, MatIconModule, MatTableModule, MatPaginatorModule,
    PageHeaderComponent, StatusChipComponent, LoadingSpinnerComponent, EmptyStateComponent, PerformancePipe,
  ],
  template: `
    <app-page-header title="Results"
      [subtitle]="ctx.backOffice ? 'Record and verify performances' : 'Official results of federation competitions'"
      [breadcrumbs]="ctx.crumbs()">
      @if (canEnter) {
        <a mat-flat-button color="primary" routerLink="new"
           [queryParams]="{ competitionId: competitionCtrl.value || null, eventId: eventCtrl.value || null }" actions>
          <mat-icon>edit_note</mat-icon> Enter Results
        </a>
      }
    </app-page-header>

    <div class="card-padded mb-4">
      <div class="flex flex-wrap gap-3 items-center">
        <mat-form-field appearance="outline" class="flex-1 min-w-64">
          <mat-label>Competition</mat-label>
          <mat-select [formControl]="competitionCtrl">
            <mat-option value="">All competitions</mat-option>
            @for (c of competitions(); track c.id) {
              <mat-option [value]="c.id">{{ c.name }} ({{ c.startDate | date:'y' }})</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline" class="w-64">
          <mat-label>Event</mat-label>
          <mat-select [formControl]="eventCtrl" [disabled]="!competitionCtrl.value">
            <mat-option value="">All events</mat-option>
            @for (e of events(); track e.id) { <mat-option [value]="e.id">{{ e.name }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline" class="w-44">
          <mat-label>Status</mat-label>
          <mat-select [formControl]="statusCtrl">
            <mat-option value="">All</mat-option>
            @for (s of statuses; track s) { <mat-option [value]="s">{{ humanize(s) }}</mat-option> }
          </mat-select>
        </mat-form-field>
      </div>
    </div>

    <div class="card relative">
      @if (loading()) { <app-loading-spinner [overlay]="true" message="Loading results…" /> }
      @if (!loading() && results().length === 0) {
        <app-empty-state icon="leaderboard" title="No results found" subtitle="Try another competition or event." />
      } @else {
        <div class="table-container">
          <table mat-table [dataSource]="results()" class="w-full">
            <ng-container matColumnDef="rank">
              <th mat-header-cell *matHeaderCellDef class="!pl-6">Rank</th>
              <td mat-cell *matCellDef="let r" class="!pl-6">
                @if (r.rankPosition) {
                  <span class="inline-flex w-8 h-8 rounded-full items-center justify-center text-sm font-bold"
                        [ngClass]="medalClass(r.medal)">{{ r.rankPosition }}</span>
                } @else { <span class="text-surface-300">—</span> }
              </td>
            </ng-container>
            <ng-container matColumnDef="athlete">
              <th mat-header-cell *matHeaderCellDef>Athlete</th>
              <td mat-cell *matCellDef="let r">
                <p class="font-medium text-surface-900">{{ r.athleteName }}</p>
                <p class="text-xs text-surface-400">{{ r.clubName || 'Independent' }}</p>
              </td>
            </ng-container>
            <ng-container matColumnDef="event">
              <th mat-header-cell *matHeaderCellDef>Event</th>
              <td mat-cell *matCellDef="let r">
                <p class="text-sm text-surface-800">{{ r.eventName }}</p>
                <p class="text-xs text-surface-400">{{ r.competitionName }} · {{ humanize(r.round) }}</p>
              </td>
            </ng-container>
            <ng-container matColumnDef="performance">
              <th mat-header-cell *matHeaderCellDef>Performance</th>
              <td mat-cell *matCellDef="let r" class="font-mono font-semibold">
                {{ r.performanceText || (r.performanceValue | performance:r.performanceUnit) }}
                @if (r.windSpeed !== null && r.windSpeed !== undefined) {
                  <span class="text-xs text-surface-400 font-normal ml-1">({{ r.windSpeed }} m/s)</span>
                }
                @if (r.nationalRecord) { <span class="ml-1 text-xs font-sans text-red-600">NR</span> }
                @else if (r.competitionRecord) { <span class="ml-1 text-xs font-sans text-purple-600">CR</span> }
                @else if (r.personalBest) { <span class="ml-1 text-xs font-sans text-green-600">PB</span> }
              </td>
            </ng-container>
            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef>Status</th>
              <td mat-cell *matCellDef="let r"><app-status-chip [status]="r.status" /></td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="columns" class="!bg-surface-50"></tr>
            <tr mat-row *matRowDef="let row; columns: columns"
                class="hover:bg-surface-50 cursor-pointer" [routerLink]="[row.id]"></tr>
          </table>
        </div>
        <mat-paginator [length]="total()" [pageSize]="pageSize()" [pageSizeOptions]="[25, 50, 100]"
          [pageIndex]="pageIndex()" (page)="onPage($event)" showFirstLastButtons />
      }
    </div>
  `,
})
export class ResultsListComponent implements OnInit {
  private api  = inject(ApiService);
  private auth = inject(AuthService);
  private route = inject(ActivatedRoute);
  readonly ctx = pageContext('results', 'Results');
  readonly humanize = humanize;
  readonly statuses = RESULT_STATUSES;
  readonly columns  = ['rank', 'athlete', 'event', 'performance', 'status'];

  competitions = signal<Competition[]>([]);
  events       = signal<CompetitionEvent[]>([]);
  results      = signal<Result[]>([]);
  loading      = signal(true);
  total        = signal(0);
  pageIndex    = signal(0);
  pageSize     = signal(50);

  competitionCtrl = new FormControl('');
  eventCtrl       = new FormControl('');
  statusCtrl      = new FormControl('');

  get canEnter(): boolean {
    return this.ctx.backOffice && this.auth.hasAnyRole([UserRole.ADMIN, UserRole.FEDERATION_STAFF]);
  }

  ngOnInit(): void {
    this.api.getPaged<Competition>('/competitions', { size: 100, sort: 'startDate,desc' })
      .subscribe(p => this.competitions.set(p.content));
    this.competitionCtrl.valueChanges.subscribe(id => {
      this.events.set([]);
      this.eventCtrl.setValue('', { emitEvent: false });
      if (id) this.api.get<CompetitionEvent[]>(`/competitions/${id}/events`).subscribe(e => this.events.set(e));
      this.reload();
    });
    this.eventCtrl.valueChanges.subscribe(() => this.reload());
    this.statusCtrl.valueChanges.subscribe(() => this.reload());

    // Deep links such as /admin/results?competitionId=…&eventId=… (used after entering results)
    const q = this.route.snapshot.queryParamMap;
    if (q.get('competitionId')) {
      this.competitionCtrl.setValue(q.get('competitionId'));
      if (q.get('eventId')) this.eventCtrl.setValue(q.get('eventId'));
    } else {
      this.load();
    }
  }

  load(): void {
    this.loading.set(true);
    const eventSelected = !!this.eventCtrl.value;
    const params: Record<string, any> = {
      page: this.pageIndex(),
      size: this.pageSize(),
      // Within one event, order by mark so the table reads like a leaderboard
      sort: eventSelected ? 'performanceValue,asc' : 'createdAt,desc',
    };
    if (this.competitionCtrl.value) params['competitionId'] = this.competitionCtrl.value;
    if (this.eventCtrl.value)       params['eventId']       = this.eventCtrl.value;
    if (this.statusCtrl.value)      params['status']        = this.statusCtrl.value;

    this.api.getPaged<Result>('/results', params).subscribe({
      next: p => {
        const rows = eventSelected
          ? [...p.content].sort((a, b) => (a.rankPosition ?? 1e9) - (b.rankPosition ?? 1e9))
          : p.content;
        this.results.set(rows);
        this.total.set(p.totalElements);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  onPage(e: PageEvent): void {
    this.pageIndex.set(e.pageIndex);
    this.pageSize.set(e.pageSize);
    this.load();
  }

  medalClass(medal?: string): string {
    switch (medal) {
      case 'GOLD':   return 'bg-amber-400 text-white';
      case 'SILVER': return 'bg-slate-300 text-slate-800';
      case 'BRONZE': return 'bg-orange-300 text-orange-900';
      default:       return 'bg-surface-100 text-surface-600';
    }
  }

  private reload(): void { this.pageIndex.set(0); this.load(); }
}
