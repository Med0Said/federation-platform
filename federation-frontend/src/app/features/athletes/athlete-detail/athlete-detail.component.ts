import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule }    from '@angular/common';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule }   from '@angular/material/icon';
import { MatTabsModule }   from '@angular/material/tabs';
import { ApiService }              from '@core/services/api.service';
import { AuthService }             from '@core/services/auth.service';
import { AthleteDetail, humanize, Registration, Result, UserRole } from '@core/models';
import { PageHeaderComponent }     from '@shared/components/page-header/page-header.component';
import { StatusChipComponent }     from '@shared/components/status-chip/status-chip.component';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';
import { EmptyStateComponent }     from '@shared/components/empty-state/empty-state.component';
import { InitialsPipe, PerformancePipe } from '@shared/pipes';
import { pageContext } from '@shared/utils/page-context';

@Component({
  selector:   'app-athlete-detail',
  standalone: true,
  imports: [
    CommonModule, RouterModule, MatButtonModule, MatIconModule, MatTabsModule,
    PageHeaderComponent, StatusChipComponent, LoadingSpinnerComponent, EmptyStateComponent,
    InitialsPipe, PerformancePipe,
  ],
  template: `
    @if (loading()) {
      <div class="flex justify-center py-20">
        <app-loading-spinner [size]="48" message="Loading athlete profile…" />
      </div>
    } @else { @if (athlete(); as a) {
      <app-page-header
        [title]="a.firstName + ' ' + a.lastName"
        [subtitle]="a.licenseNumber"
        [breadcrumbs]="ctx.crumbs({ label: a.firstName + ' ' + a.lastName })">
        @if (canManage()) {
          <a mat-stroked-button [routerLink]="['/admin/athletes', a.id, 'edit']" actions>
            <mat-icon>edit</mat-icon> Edit
          </a>
        }
      </app-page-header>

      <div class="grid grid-cols-1 lg:grid-cols-4 gap-6">
        <!-- Profile card -->
        <div class="lg:col-span-1 space-y-4">
          <div class="card-padded text-center">
            @if (a.photoUrl) {
              <img [src]="a.photoUrl" [alt]="a.firstName"
                   class="w-24 h-24 rounded-full object-cover mx-auto mb-4" />
            } @else {
              <div class="w-24 h-24 rounded-full bg-primary-100 flex items-center justify-center
                          text-primary-700 text-2xl font-bold mx-auto mb-4">
                {{ (a.firstName + ' ' + a.lastName) | initials }}
              </div>
            }
            <h2 class="font-bold text-surface-900 text-lg">{{ a.firstName }} {{ a.lastName }}</h2>
            @if (a.clubId) {
              <a [routerLink]="[clubBase, a.clubId]" class="text-sm text-primary-600 hover:underline block mb-3">{{ a.clubName }}</a>
            } @else {
              <p class="text-sm text-surface-500 mb-3">Independent</p>
            }
            <app-status-chip [status]="a.status" />
          </div>

          <div class="card-padded space-y-3">
            @for (info of athleteInfo(); track info.label) {
              <div class="flex justify-between gap-3 text-sm">
                <span class="text-surface-500">{{ info.label }}</span>
                <span class="font-medium text-surface-800 text-right break-all">{{ info.value }}</span>
              </div>
            }
          </div>
          @if (a.medicalNotes || a.notes) {
            <div class="card-padded text-sm space-y-2">
              @if (a.medicalNotes) { <p><span class="text-surface-500">Medical:</span> {{ a.medicalNotes }}</p> }
              @if (a.notes) { <p><span class="text-surface-500">Notes:</span> {{ a.notes }}</p> }
            </div>
          }
        </div>

        <!-- Tabs -->
        <div class="lg:col-span-3">
          <mat-tab-group animationDuration="200ms" class="card">
            <mat-tab [label]="'Results (' + results().length + ')'">
              @if (results().length === 0) {
                <app-empty-state icon="leaderboard" title="No results yet" />
              } @else {
                @if (personalBests().length) {
                  <div class="grid grid-cols-1 sm:grid-cols-3 gap-3 p-4 border-b border-surface-100">
                    @for (pb of personalBests(); track pb.discipline) {
                      <div class="rounded-lg bg-surface-50 p-3">
                        <p class="text-xs uppercase text-surface-400">Best · {{ pb.discipline }}</p>
                        <p class="text-xl font-bold font-mono">{{ pb.text || (pb.value | performance:pb.unit) }}</p>
                      </div>
                    }
                  </div>
                }
                <ul class="divide-y divide-surface-100">
                  @for (r of results(); track r.id) {
                    <li class="flex items-center gap-4 px-6 py-3">
                      <span class="w-8 text-center font-bold text-surface-700">{{ r.rankPosition ?? '—' }}</span>
                      <div class="flex-1 min-w-0">
                        <p class="text-sm font-medium text-surface-900">{{ r.eventName }}</p>
                        <a [routerLink]="[competitionBase, r.competitionId]" class="text-xs text-surface-500 hover:text-primary-600">
                          {{ r.competitionName }} · {{ humanize(r.round) }}
                        </a>
                      </div>
                      <span class="font-mono font-semibold text-sm">
                        {{ r.performanceText || (r.performanceValue | performance:r.performanceUnit) }}
                      </span>
                      @if (r.medal) { <span class="text-xs font-semibold">{{ humanize(r.medal) }}</span> }
                      <app-status-chip [status]="r.status" />
                    </li>
                  }
                </ul>
              }
            </mat-tab>
            @if (canSeeRegistrations()) {
              <mat-tab [label]="'Registrations (' + registrations().length + ')'">
                @if (registrations().length === 0) {
                  <app-empty-state icon="how_to_reg" title="No registrations" />
                } @else {
                  <ul class="divide-y divide-surface-100">
                    @for (r of registrations(); track r.id) {
                      <li class="flex items-center gap-4 px-6 py-3">
                        <div class="flex-1 min-w-0">
                          <p class="text-sm font-medium text-surface-900">{{ r.competitionName }}</p>
                          <p class="text-xs text-surface-500">{{ r.eventName }} · {{ r.registrationNumber }}</p>
                        </div>
                        @if (r.bibNumber) { <span class="text-xs font-semibold">Bib {{ r.bibNumber }}</span> }
                        <app-status-chip [status]="r.status" />
                      </li>
                    }
                  </ul>
                }
              </mat-tab>
            }
          </mat-tab-group>
        </div>
      </div>
    } }
  `,
})
export class AthleteDetailComponent implements OnInit {
  private api   = inject(ApiService);
  private auth  = inject(AuthService);
  private route = inject(ActivatedRoute);
  readonly ctx  = pageContext('athletes', 'Athletes');
  readonly humanize = humanize;
  readonly clubBase        = this.ctx.backOffice ? '/admin/clubs' : '/clubs';
  readonly competitionBase = this.ctx.backOffice ? '/admin/competitions' : '/competitions';

  athlete       = signal<AthleteDetail | null>(null);
  results       = signal<Result[]>([]);
  registrations = signal<Registration[]>([]);
  loading       = signal(true);

  /** The API withholds private data (restricted) unless the caller is staff, the club manager or the athlete. */
  canSeeRegistrations = computed(() => {
    const a = this.athlete();
    return !!a && this.auth.isAuthenticated() && !a.restricted;
  });

  canManage = computed(() => {
    const a = this.athlete();
    if (!a || !this.ctx.backOffice) return false;
    if (this.auth.hasAnyRole([UserRole.ADMIN, UserRole.FEDERATION_STAFF])) return true;
    return this.auth.hasRole(UserRole.CLUB_MANAGER) && !a.restricted;
  });

  personalBests = computed(() => {
    const best = new Map<string, { discipline: string; value: number; unit?: string; text?: string }>();
    for (const r of this.results()) {
      if (r.performanceValue === null || r.performanceValue === undefined) continue;
      if (!['OFFICIAL', 'UNOFFICIAL'].includes(r.status)) continue;
      const lower = r.performanceUnit === 'seconds';
      const cur = best.get(r.discipline);
      if (!cur || (lower ? r.performanceValue < cur.value : r.performanceValue > cur.value)) {
        best.set(r.discipline, { discipline: r.discipline, value: r.performanceValue, unit: r.performanceUnit, text: r.performanceText });
      }
    }
    return [...best.values()].slice(0, 3);
  });

  athleteInfo = computed(() => {
    const a = this.athlete();
    if (!a) return [];
    const rows: { label: string; value: string | number }[] = [
      { label: 'Licence',       value: a.licenseNumber },
      { label: 'Gender',        value: humanize(a.gender) },
      { label: 'Category',      value: a.category ? humanize(a.category) : '—' },
      { label: 'Age',           value: a.age },
      { label: 'Nationality',   value: a.nationality },
    ];
    if (a.heightCm) rows.push({ label: 'Height', value: `${a.heightCm} cm` });
    if (a.weightKg) rows.push({ label: 'Weight', value: `${a.weightKg} kg` });
    if (a.email)    rows.push({ label: 'E-mail', value: a.email });
    if (a.phone)    rows.push({ label: 'Phone',  value: a.phone });
    if (a.licenseExpiry)        rows.push({ label: 'Licence expiry',    value: new Date(a.licenseExpiry).toLocaleDateString() });
    if (a.medicalClearanceDate) rows.push({ label: 'Medical clearance', value: new Date(a.medicalClearanceDate).toLocaleDateString() });
    if (a.emergencyContactName) rows.push({ label: 'Emergency', value: `${a.emergencyContactName} ${a.emergencyContactPhone ?? ''}` });
    return rows;
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.api.get<AthleteDetail>(`/athletes/${id}`).subscribe({
      next: a => {
        this.athlete.set(a);
        this.loading.set(false);
        if (this.canSeeRegistrations()) {
          this.api.getPaged<Registration>(`/athletes/${id}/registrations`, { size: 100 })
            .subscribe(p => this.registrations.set(p.content));
        }
      },
      error: () => this.loading.set(false),
    });
    this.api.getPaged<Result>('/results', { athleteId: id, size: 100, sort: 'createdAt,desc' })
      .subscribe(p => this.results.set(p.content));
  }
}
