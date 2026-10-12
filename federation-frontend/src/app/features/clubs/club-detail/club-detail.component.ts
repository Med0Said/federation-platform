import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';

import { ApiService } from '@core/services/api.service';
import { AuthService } from '@core/services/auth.service';
import { NotificationService } from '@core/services/notification.service';
import { AthleteDetail, Club, UserRole } from '@core/models';
import { PageHeaderComponent } from '@shared/components/page-header/page-header.component';
import { StatusChipComponent } from '@shared/components/status-chip/status-chip.component';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';
import { EmptyStateComponent } from '@shared/components/empty-state/empty-state.component';
import { ConfirmDialogComponent } from '@shared/components/confirm-dialog/confirm-dialog.component';
import { InitialsPipe } from '@shared/pipes';
import { pageContext } from '@shared/utils/page-context';

@Component({
  selector: 'app-club-detail',
  standalone: true,
  imports: [
    CommonModule, RouterModule, MatButtonModule, MatIconModule,
    PageHeaderComponent, StatusChipComponent, LoadingSpinnerComponent, EmptyStateComponent, InitialsPipe,
  ],
  template: `
    @if (loading()) {
      <app-loading-spinner [size]="48" message="Loading club…" />
    } @else { @if (club(); as c) {
      <app-page-header [title]="c.name" [subtitle]="c.licenseNumber"
                       [breadcrumbs]="ctx.crumbs({ label: c.shortName || c.name })">
        @if (canEdit()) {
          <a mat-stroked-button [routerLink]="[ctx.base, c.id, 'edit']" actions>
            <mat-icon>edit</mat-icon> Edit
          </a>
        }
        @if (canDelete()) {
          <button mat-stroked-button color="warn" (click)="confirmDelete(c)" actions>
            <mat-icon>delete</mat-icon> Delete
          </button>
        }
      </app-page-header>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div class="space-y-4">
          <div class="card-padded text-center">
            @if (c.logoUrl) {
              <img [src]="c.logoUrl" [alt]="c.name" class="w-24 h-24 object-contain mx-auto mb-4" />
            } @else {
              <div class="w-24 h-24 rounded-xl bg-primary-100 flex items-center justify-center
                          text-primary-700 text-3xl font-bold mx-auto mb-4">
                {{ (c.shortName || c.name).charAt(0) }}
              </div>
            }
            <h2 class="font-bold text-surface-900 text-lg">{{ c.name }}</h2>
            <p class="text-sm text-surface-500 mb-3">{{ c.city }}{{ c.region ? ', ' + c.region : '' }}</p>
            <app-status-chip [status]="c.status" />
          </div>

          <div class="card-padded space-y-3 text-sm">
            @for (row of info(); track row.label) {
              <div class="flex justify-between gap-4">
                <span class="text-surface-500">{{ row.label }}</span>
                <span class="font-medium text-surface-800 text-right break-all">{{ row.value }}</span>
              </div>
            }
            @if (c.website) {
              <a [href]="c.website" target="_blank" rel="noopener"
                 class="flex items-center gap-1 text-primary-600 hover:underline">
                <mat-icon class="!text-base !w-4 !h-4">open_in_new</mat-icon> Website
              </a>
            }
          </div>
        </div>

        <div class="lg:col-span-2 space-y-4">
          @if (c.description) {
            <div class="card-padded">
              <h3 class="font-semibold text-surface-800 mb-2">About</h3>
              <p class="text-sm text-surface-600 whitespace-pre-line">{{ c.description }}</p>
            </div>
          }

          <div class="card">
            <div class="flex items-center justify-between px-6 py-4 border-b border-surface-100">
              <h3 class="font-semibold text-surface-800">Athletes ({{ c.activeAthletes }} active)</h3>
              @if (canEdit() && ctx.backOffice) {
                <a mat-button color="primary" routerLink="/admin/athletes/new">
                  <mat-icon>person_add</mat-icon> Add athlete
                </a>
              }
            </div>
            @if (athletes().length === 0) {
              <app-empty-state icon="directions_run" title="No athletes yet" />
            } @else {
              <ul class="divide-y divide-surface-100">
                @for (a of athletes(); track a.id) {
                  <li>
                    <a [routerLink]="[athleteBase, a.id]"
                       class="flex items-center gap-3 px-6 py-3 hover:bg-surface-50 transition-colors">
                      <div class="w-9 h-9 rounded-full bg-primary-100 flex items-center justify-center
                                  text-primary-700 text-xs font-semibold flex-shrink-0">
                        {{ (a.firstName + ' ' + a.lastName) | initials }}
                      </div>
                      <div class="flex-1 min-w-0">
                        <p class="text-sm font-medium text-surface-900">{{ a.firstName }} {{ a.lastName }}</p>
                        <p class="text-xs text-surface-400">{{ a.category }} · {{ a.gender }}</p>
                      </div>
                      <app-status-chip [status]="a.status" />
                    </a>
                  </li>
                }
              </ul>
            }
          </div>
        </div>
      </div>
    } }
  `,
})
export class ClubDetailComponent implements OnInit {
  private api    = inject(ApiService);
  private auth   = inject(AuthService);
  private notify = inject(NotificationService);
  private dialog = inject(MatDialog);
  private route  = inject(ActivatedRoute);
  private router = inject(Router);
  readonly ctx   = pageContext('clubs', 'Clubs');
  readonly athleteBase = this.ctx.backOffice ? '/admin/athletes' : '/athletes';

  club     = signal<Club | null>(null);
  athletes = signal<AthleteDetail[]>([]);
  loading  = signal(true);

  canEdit = computed(() => {
    const c = this.club();
    if (!c || !this.ctx.backOffice) return false;
    if (this.auth.hasAnyRole([UserRole.ADMIN, UserRole.FEDERATION_STAFF])) return true;
    return this.auth.hasRole(UserRole.CLUB_MANAGER) && c.managerId === this.auth.currentUser()?.id;
  });
  canDelete = computed(() => this.ctx.backOffice && this.auth.hasRole(UserRole.ADMIN));

  info = computed(() => {
    const c = this.club();
    if (!c) return [];
    return [
      { label: 'Short name', value: c.shortName || '—' },
      { label: 'Founded',    value: c.foundedYear ?? '—' },
      { label: 'Country',    value: c.country },
      { label: 'Manager',    value: c.managerName || '—' },
      { label: 'E-mail',     value: c.email || '—' },
      { label: 'Phone',      value: c.phone || '—' },
      { label: 'Address',    value: [c.address, c.city].filter(Boolean).join(', ') || '—' },
    ];
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.api.get<Club>(`/clubs/${id}`).subscribe({
      next: c => { this.club.set(c); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
    this.api.getPaged<AthleteDetail>('/athletes', { clubId: id, size: 100, sort: 'lastName,asc' })
      .subscribe(p => this.athletes.set(p.content));
  }

  confirmDelete(club: Club): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete club',
        message: `Permanently delete ${club.name}? Its athletes become independent.`,
        confirmText: 'Delete', danger: true,
      },
    }).afterClosed().subscribe(ok => {
      if (!ok) return;
      this.api.delete(`/clubs/${club.id}`).subscribe(() => {
        this.notify.success('Club deleted.');
        this.router.navigate([this.ctx.base]);
      });
    });
  }
}
