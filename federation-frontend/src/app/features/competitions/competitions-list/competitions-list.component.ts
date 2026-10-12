import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { ApiService } from '@core/services/api.service';
import { AuthService } from '@core/services/auth.service';
import { Competition, COMPETITION_STATUSES, humanize, UserRole } from '@core/models';
import { PageHeaderComponent } from '@shared/components/page-header/page-header.component';
import { StatusChipComponent } from '@shared/components/status-chip/status-chip.component';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';
import { EmptyStateComponent } from '@shared/components/empty-state/empty-state.component';
import { pageContext } from '@shared/utils/page-context';

type When = 'upcoming' | 'past' | 'all';

@Component({
  selector: 'app-competitions-list',
  standalone: true,
  imports: [
    CommonModule, RouterModule, ReactiveFormsModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, MatIconModule,
    MatPaginatorModule, MatButtonToggleModule,
    PageHeaderComponent, StatusChipComponent, LoadingSpinnerComponent, EmptyStateComponent,
  ],
  template: `
    <app-page-header title="Competitions"
      [subtitle]="ctx.backOffice ? 'Plan competitions, events and registrations' : 'Calendar of federation competitions'"
      [breadcrumbs]="ctx.crumbs()">
      @if (canCreate) {
        <a mat-flat-button color="primary" routerLink="new" actions>
          <mat-icon>add</mat-icon> New Competition
        </a>
      }
    </app-page-header>

    <div class="card-padded mb-4">
      <div class="flex flex-wrap gap-3 items-center">
        <mat-form-field appearance="outline" class="flex-1 min-w-48">
          <mat-label>Search by name, sport or city…</mat-label>
          <input matInput [formControl]="searchCtrl" autocomplete="off" />
          <mat-icon matPrefix>search</mat-icon>
        </mat-form-field>
        <mat-form-field appearance="outline" class="w-56">
          <mat-label>Status</mat-label>
          <mat-select [formControl]="statusCtrl">
            <mat-option value="">All</mat-option>
            @for (s of statuses; track s) {
              <mat-option [value]="s">{{ humanize(s) }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-button-toggle-group [value]="when()" (change)="setWhen($event.value)" class="h-12">
          <mat-button-toggle value="upcoming">Upcoming</mat-button-toggle>
          <mat-button-toggle value="past">Past</mat-button-toggle>
          <mat-button-toggle value="all">All</mat-button-toggle>
        </mat-button-toggle-group>
      </div>
    </div>

    <div class="card relative">
      @if (loading()) { <app-loading-spinner [overlay]="true" message="Loading competitions…" /> }
      @if (!loading() && competitions().length === 0) {
        <app-empty-state icon="emoji_events" title="No competitions found"
          subtitle="Try another period or remove filters." />
      } @else {
        <ul class="divide-y divide-surface-100">
          @for (c of competitions(); track c.id) {
            <li>
              <a [routerLink]="[c.id]" class="flex items-center gap-4 px-6 py-4 hover:bg-surface-50 transition-colors group">
                <div class="w-14 flex-shrink-0 text-center rounded-lg border border-surface-200 py-1.5">
                  <p class="text-xs uppercase text-primary-600 font-semibold">{{ c.startDate | date:'MMM' }}</p>
                  <p class="text-xl font-bold text-surface-900 leading-tight">{{ c.startDate | date:'d' }}</p>
                  <p class="text-[10px] text-surface-400">{{ c.startDate | date:'y' }}</p>
                </div>
                <div class="flex-1 min-w-0">
                  <p class="font-semibold text-surface-900 truncate group-hover:text-primary-600 transition-colors">
                    {{ c.name }}
                  </p>
                  <p class="text-xs text-surface-500 mt-0.5">
                    {{ c.sport }} · {{ humanize(c.level) }}
                    @if (c.venueCity) { · {{ c.venueName ? c.venueName + ', ' : '' }}{{ c.venueCity }} }
                  </p>
                  <p class="text-xs text-surface-400 mt-0.5">
                    {{ c.startDate | date:'d MMM' }} – {{ c.endDate | date:'d MMM y' }}
                    · {{ c.eventCount }} event{{ c.eventCount === 1 ? '' : 's' }}
                    @if (c.status === 'REGISTRATION_OPEN' && c.registrationDeadline) {
                      · registration until {{ c.registrationDeadline | date:'d MMM' }}
                    }
                  </p>
                </div>
                <app-status-chip [status]="c.status" />
              </a>
            </li>
          }
        </ul>
        <div class="border-t border-surface-100">
          <mat-paginator [length]="total()" [pageSize]="pageSize()" [pageSizeOptions]="[10, 20, 50]"
            [pageIndex]="pageIndex()" (page)="onPage($event)" showFirstLastButtons />
        </div>
      }
    </div>
  `,
})
export class CompetitionsListComponent implements OnInit {
  private api  = inject(ApiService);
  private auth = inject(AuthService);
  readonly ctx = pageContext('competitions', 'Competitions');
  readonly humanize = humanize;

  /** Drafts are internal: the public calendar never offers them as a filter. */
  readonly statuses = this.ctx.backOffice ? COMPETITION_STATUSES : COMPETITION_STATUSES.filter(s => s !== 'DRAFT');

  competitions = signal<Competition[]>([]);
  loading      = signal(true);
  total        = signal(0);
  pageIndex    = signal(0);
  pageSize     = signal(20);
  when         = signal<When>('upcoming');
  searchCtrl   = new FormControl('');
  statusCtrl   = new FormControl('');

  get canCreate(): boolean {
    return this.ctx.backOffice && this.auth.hasAnyRole([UserRole.ADMIN, UserRole.FEDERATION_STAFF]);
  }

  ngOnInit(): void {
    this.load();
    this.searchCtrl.valueChanges.pipe(debounceTime(350), distinctUntilChanged()).subscribe(() => this.reload());
    this.statusCtrl.valueChanges.subscribe(() => this.reload());
  }

  setWhen(w: When): void { this.when.set(w); this.reload(); }

  load(): void {
    this.loading.set(true);
    const today = new Date().toISOString().substring(0, 10);
    const upcoming = this.when() === 'upcoming';
    const params: Record<string, any> = {
      page: this.pageIndex(),
      size: this.pageSize(),
      sort: upcoming ? 'startDate,asc' : 'startDate,desc',
    };
    if (upcoming) params['from'] = today;
    if (this.when() === 'past') params['to'] = today;
    if (this.searchCtrl.value) params['search'] = this.searchCtrl.value;
    if (this.statusCtrl.value) params['status'] = this.statusCtrl.value;

    this.api.getPaged<Competition>('/competitions', params).subscribe({
      next: p => {
        this.competitions.set(p.content);
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

  private reload(): void { this.pageIndex.set(0); this.load(); }
}
