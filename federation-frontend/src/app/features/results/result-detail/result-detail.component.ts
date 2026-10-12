import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';

import { ApiService } from '@core/services/api.service';
import { AuthService } from '@core/services/auth.service';
import { NotificationService } from '@core/services/notification.service';
import { humanize, Result, UserRole } from '@core/models';
import { PageHeaderComponent } from '@shared/components/page-header/page-header.component';
import { StatusChipComponent } from '@shared/components/status-chip/status-chip.component';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';
import { ConfirmDialogComponent } from '@shared/components/confirm-dialog/confirm-dialog.component';
import { PerformancePipe } from '@shared/pipes';
import { pageContext } from '@shared/utils/page-context';

@Component({
  selector: 'app-result-detail',
  standalone: true,
  imports: [
    CommonModule, RouterModule, MatButtonModule, MatIconModule,
    PageHeaderComponent, StatusChipComponent, LoadingSpinnerComponent, PerformancePipe,
  ],
  template: `
    @if (loading()) {
      <app-loading-spinner message="Loading result…" />
    } @else { @if (result(); as r) {
      <app-page-header [title]="r.athleteName" [subtitle]="r.eventName + ' — ' + r.competitionName"
                       [breadcrumbs]="ctx.crumbs({ label: r.athleteName })">
        @if (canManage) {
          <ng-container actions>
            <a mat-stroked-button [routerLink]="['/admin/results', r.id, 'edit']">
              <mat-icon>edit</mat-icon> Edit
            </a>
            <button mat-stroked-button color="warn" (click)="remove(r)">
              <mat-icon>delete</mat-icon> Delete
            </button>
          </ng-container>
        }
      </app-page-header>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div class="card-padded text-center">
          <p class="text-xs uppercase tracking-wide text-surface-400 mb-1">Performance</p>
          <p class="text-4xl font-bold font-mono text-surface-900">
            {{ r.performanceText || (r.performanceValue | performance:r.performanceUnit) }}
          </p>
          @if (r.rankPosition) {
            <p class="mt-3 text-sm text-surface-600">
              Rank <strong>{{ r.rankPosition }}</strong>
              @if (r.medal) { · <span class="font-semibold">{{ humanize(r.medal) }} medal</span> }
            </p>
          }
          <div class="mt-3 flex justify-center gap-2 flex-wrap">
            <app-status-chip [status]="r.status" />
            @for (flag of flags(); track flag) {
              <span class="text-xs px-2 py-0.5 rounded-full bg-primary-50 text-primary-700">{{ flag }}</span>
            }
          </div>
        </div>
        <div class="card-padded lg:col-span-2">
          <dl class="grid grid-cols-1 sm:grid-cols-2 gap-x-8 gap-y-3 text-sm">
            @for (row of facts(); track row.label) {
              <div class="flex justify-between gap-4 border-b border-surface-100 pb-2">
                <dt class="text-surface-500">{{ row.label }}</dt>
                <dd class="font-medium text-surface-800 text-right">{{ row.value }}</dd>
              </div>
            }
          </dl>
          @if (r.notes) { <p class="mt-4 text-sm text-surface-600 whitespace-pre-line">{{ r.notes }}</p> }
          <div class="mt-4 flex gap-3 text-sm">
            <a [routerLink]="[competitionBase, r.competitionId]" class="text-primary-600 hover:underline">Competition</a>
            <a [routerLink]="[athleteBase, r.athleteId]" class="text-primary-600 hover:underline">Athlete profile</a>
          </div>
        </div>
      </div>
    } }
  `,
})
export class ResultDetailComponent implements OnInit {
  private api    = inject(ApiService);
  private auth   = inject(AuthService);
  private notify = inject(NotificationService);
  private dialog = inject(MatDialog);
  private route  = inject(ActivatedRoute);
  private router = inject(Router);
  readonly ctx   = pageContext('results', 'Results');
  readonly humanize = humanize;
  readonly competitionBase = this.ctx.backOffice ? '/admin/competitions' : '/competitions';
  readonly athleteBase     = this.ctx.backOffice ? '/admin/athletes' : '/athletes';
  readonly canManage = this.ctx.backOffice && this.auth.hasAnyRole([UserRole.ADMIN, UserRole.FEDERATION_STAFF]);

  result  = signal<Result | null>(null);
  loading = signal(true);

  flags = computed(() => {
    const r = this.result();
    if (!r) return [];
    return [
      r.nationalRecord && 'National record',
      r.competitionRecord && 'Competition record',
      r.personalBest && 'Personal best',
      r.seasonBest && 'Season best',
    ].filter(Boolean) as string[];
  });

  facts = computed(() => {
    const r = this.result();
    if (!r) return [];
    return [
      { label: 'Round',    value: humanize(r.round) },
      { label: 'Heat',     value: r.heatNumber ?? '—' },
      { label: 'Lane',     value: r.laneNumber ?? '—' },
      { label: 'Wind',     value: r.windSpeed !== undefined && r.windSpeed !== null ? `${r.windSpeed} m/s` : '—' },
      { label: 'Points',   value: r.points ?? '—' },
      { label: 'Club',     value: r.clubName || 'Independent' },
      { label: 'License',  value: r.licenseNumber },
      { label: 'Category', value: r.category ? humanize(r.category) : '—' },
    ];
  });

  ngOnInit(): void {
    this.api.get<Result>(`/results/${this.route.snapshot.paramMap.get('id')}`).subscribe({
      next: r => { this.result.set(r); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  remove(r: Result): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Delete result', message: `Delete the result of ${r.athleteName}? Rankings are recalculated.`, confirmText: 'Delete', danger: true },
    }).afterClosed().subscribe(ok => {
      if (!ok) return;
      this.api.delete(`/results/${r.id}`).subscribe(() => {
        this.notify.success('Result deleted.');
        this.router.navigate(['/admin/results']);
      });
    });
  }
}
