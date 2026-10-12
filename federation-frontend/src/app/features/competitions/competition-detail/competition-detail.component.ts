import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTableModule } from '@angular/material/table';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog } from '@angular/material/dialog';

import { ApiService } from '@core/services/api.service';
import { AuthService } from '@core/services/auth.service';
import { NotificationService } from '@core/services/notification.service';
import {
  Club, Competition, CompetitionEvent, humanize, Ranking, Registration, RegistrationStatus, UserRole,
} from '@core/models';
import { PageHeaderComponent } from '@shared/components/page-header/page-header.component';
import { StatusChipComponent } from '@shared/components/status-chip/status-chip.component';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';
import { EmptyStateComponent } from '@shared/components/empty-state/empty-state.component';
import { ConfirmDialogComponent } from '@shared/components/confirm-dialog/confirm-dialog.component';
import { PromptDialogComponent } from '@shared/components/prompt-dialog/prompt-dialog.component';
import { PerformancePipe } from '@shared/pipes';
import { pageContext } from '@shared/utils/page-context';
import { EventDialogComponent } from '../event-dialog/event-dialog.component';
import { RegisterDialogComponent, RegisterDialogResult } from '../register-dialog/register-dialog.component';

@Component({
  selector: 'app-competition-detail',
  standalone: true,
  imports: [
    CommonModule, RouterModule, ReactiveFormsModule,
    MatButtonModule, MatIconModule, MatTabsModule, MatTableModule, MatMenuModule, MatDividerModule,
    MatSelectModule, MatFormFieldModule, MatTooltipModule,
    PageHeaderComponent, StatusChipComponent, LoadingSpinnerComponent, EmptyStateComponent, PerformancePipe,
  ],
  template: `
    @if (loading()) {
      <app-loading-spinner [size]="48" message="Loading competition…" />
    } @else { @if (competition(); as c) {
      <app-page-header [title]="c.name"
        [subtitle]="(c.startDate | date:'d MMM') + ' – ' + (c.endDate | date:'d MMM y') + (c.venueCity ? ' · ' + c.venueCity : '')"
        [breadcrumbs]="ctx.crumbs({ label: c.name })">
        @if (canRegister()) {
          <button mat-flat-button color="primary" (click)="openRegister()" actions>
            <mat-icon>how_to_reg</mat-icon> Register
          </button>
        }
        @if (!auth.isAuthenticated() && c.status === 'REGISTRATION_OPEN') {
          <a mat-flat-button color="primary" routerLink="/auth/login"
             [queryParams]="{ returnUrl: '/competitions/' + c.id }" actions>
            <mat-icon>login</mat-icon> Log in to register
          </a>
        }
        @if (isStaff()) {
          <a mat-stroked-button [routerLink]="['/admin/competitions', c.id, 'edit']" actions>
            <mat-icon>edit</mat-icon> Edit
          </a>
        }
        @if (isAdmin()) {
          <button mat-stroked-button color="warn" (click)="deleteCompetition(c)" actions>
            <mat-icon>delete</mat-icon>
          </button>
        }
      </app-page-header>

      <div class="flex flex-wrap gap-2 mb-4">
        <app-status-chip [status]="c.status" />
        <span class="text-xs px-2.5 py-0.5 rounded-full bg-surface-100 text-surface-600">{{ c.sport }}</span>
        <span class="text-xs px-2.5 py-0.5 rounded-full bg-surface-100 text-surface-600">{{ humanize(c.level) }}</span>
        <span class="text-xs px-2.5 py-0.5 rounded-full bg-surface-100 text-surface-600">{{ humanize(c.format) }}</span>
      </div>

      <mat-tab-group animationDuration="150ms" class="card" [(selectedIndex)]="tabIndex">
        <!-- ── Overview ─────────────────────────────────────────── -->
        <mat-tab label="Overview">
          <div class="p-6 grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div class="lg:col-span-2 space-y-4">
              @if (c.description) {
                <p class="text-sm text-surface-700 whitespace-pre-line">{{ c.description }}</p>
              } @else {
                <p class="text-sm text-surface-400">No description yet.</p>
              }
              @if (c.prizeInfo) {
                <div>
                  <h4 class="font-semibold text-surface-800 mb-1">Prizes</h4>
                  <p class="text-sm text-surface-600 whitespace-pre-line">{{ c.prizeInfo }}</p>
                </div>
              }
              @if (c.rulesUrl) {
                <a [href]="c.rulesUrl" target="_blank" rel="noopener" class="text-sm text-primary-600 hover:underline inline-flex items-center gap-1">
                  <mat-icon class="!text-base !w-4 !h-4">description</mat-icon> Rules & regulations
                </a>
              }
            </div>
            <dl class="space-y-3 text-sm">
              @for (row of facts(); track row.label) {
                <div class="flex justify-between gap-4">
                  <dt class="text-surface-500">{{ row.label }}</dt>
                  <dd class="font-medium text-surface-800 text-right">{{ row.value }}</dd>
                </div>
              }
            </dl>
          </div>
        </mat-tab>

        <!-- ── Events ───────────────────────────────────────────── -->
        <mat-tab [label]="'Events (' + events().length + ')'">
          <div class="p-4">
            @if (isStaff()) {
              <div class="flex justify-end mb-3">
                <button mat-stroked-button color="primary" (click)="editEvent()">
                  <mat-icon>add</mat-icon> Add event
                </button>
              </div>
            }
            @if (events().length === 0) {
              <app-empty-state icon="sports" title="No events yet"
                [subtitle]="isStaff() ? 'Add the disciplines of this competition.' : 'The programme has not been published yet.'" />
            } @else {
              <div class="table-container">
                <table mat-table [dataSource]="events()" class="w-full">
                  <ng-container matColumnDef="name">
                    <th mat-header-cell *matHeaderCellDef>Event</th>
                    <td mat-cell *matCellDef="let e">
                      <p class="font-medium text-surface-900">{{ e.name }}</p>
                      <p class="text-xs text-surface-400">{{ e.discipline }}{{ e.code ? ' · ' + e.code : '' }}</p>
                    </td>
                  </ng-container>
                  <ng-container matColumnDef="category">
                    <th mat-header-cell *matHeaderCellDef>Category</th>
                    <td mat-cell *matCellDef="let e" class="text-sm text-surface-600">
                      {{ e.genderCategory ? humanize(e.genderCategory) : 'Open' }} · {{ e.ageCategory ? humanize(e.ageCategory) : 'All ages' }}
                    </td>
                  </ng-container>
                  <ng-container matColumnDef="schedule">
                    <th mat-header-cell *matHeaderCellDef>Schedule</th>
                    <td mat-cell *matCellDef="let e" class="text-sm text-surface-600">
                      {{ e.scheduledAt ? (e.scheduledAt | date:'EEE d MMM, HH:mm') : 'TBA' }}
                    </td>
                  </ng-container>
                  <ng-container matColumnDef="entries">
                    <th mat-header-cell *matHeaderCellDef>Entries</th>
                    <td mat-cell *matCellDef="let e" class="text-sm">
                      {{ e.registrationCount }}{{ e.maxParticipants ? ' / ' + e.maxParticipants : '' }}
                      @if (e.waitlistCount) {
                        <span class="text-xs text-blue-600 ml-1">+{{ e.waitlistCount }} waiting</span>
                      }
                    </td>
                  </ng-container>
                  <ng-container matColumnDef="status">
                    <th mat-header-cell *matHeaderCellDef>Status</th>
                    <td mat-cell *matCellDef="let e"><app-status-chip [status]="e.status" /></td>
                  </ng-container>
                  <ng-container matColumnDef="actions">
                    <th mat-header-cell *matHeaderCellDef></th>
                    <td mat-cell *matCellDef="let e" class="text-right whitespace-nowrap">
                      <button mat-icon-button matTooltip="Leaderboard" (click)="showLeaderboard(e)">
                        <mat-icon>leaderboard</mat-icon>
                      </button>
                      @if (canRegister()) {
                        <button mat-icon-button matTooltip="Register" (click)="openRegister(e.id)">
                          <mat-icon>how_to_reg</mat-icon>
                        </button>
                      }
                      @if (isStaff()) {
                        <button mat-icon-button matTooltip="Edit" (click)="editEvent(e)"><mat-icon>edit</mat-icon></button>
                        <button mat-icon-button matTooltip="Delete" (click)="deleteEvent(e)"><mat-icon>delete</mat-icon></button>
                      }
                    </td>
                  </ng-container>
                  <tr mat-header-row *matHeaderRowDef="eventColumns"></tr>
                  <tr mat-row *matRowDef="let row; columns: eventColumns"></tr>
                </table>
              </div>
            }
          </div>
        </mat-tab>

        <!-- ── Registrations (staff and club managers) ─────────── -->
        @if (canSeeRegistrations()) {
          <mat-tab label="Registrations">
            <div class="p-4">
              <div class="flex flex-wrap gap-3 items-center mb-3">
                <mat-form-field appearance="outline" class="w-64" subscriptSizing="dynamic">
                  <mat-label>Event</mat-label>
                  <mat-select [formControl]="regEventCtrl">
                    <mat-option value="">All events</mat-option>
                    @for (e of events(); track e.id) { <mat-option [value]="e.id">{{ e.name }}</mat-option> }
                  </mat-select>
                </mat-form-field>
                <mat-form-field appearance="outline" class="w-48" subscriptSizing="dynamic">
                  <mat-label>Status</mat-label>
                  <mat-select [formControl]="regStatusCtrl">
                    <mat-option value="">All</mat-option>
                    @for (s of registrationStatuses; track s) { <mat-option [value]="s">{{ humanize(s) }}</mat-option> }
                  </mat-select>
                </mat-form-field>
                @if (myClubs().length > 1) {
                  <mat-form-field appearance="outline" class="w-64" subscriptSizing="dynamic">
                    <mat-label>Club</mat-label>
                    <mat-select [formControl]="regClubCtrl">
                      @for (cl of myClubs(); track cl.id) { <mat-option [value]="cl.id">{{ cl.name }}</mat-option> }
                    </mat-select>
                  </mat-form-field>
                }
                <span class="flex-1"></span>
                @if (isStaff()) {
                  <button mat-stroked-button (click)="assignBibs()">
                    <mat-icon>pin</mat-icon> Assign bib numbers
                  </button>
                }
              </div>

              @if (registrations().length === 0) {
                <app-empty-state icon="how_to_reg" title="No registrations" subtitle="Registrations matching the filters appear here." />
              } @else {
                <div class="table-container">
                  <table mat-table [dataSource]="registrations()" class="w-full">
                    <ng-container matColumnDef="number">
                      <th mat-header-cell *matHeaderCellDef>No.</th>
                      <td mat-cell *matCellDef="let r" class="text-xs text-surface-500 whitespace-nowrap">
                        {{ r.registrationNumber }}
                        @if (r.bibNumber) { <span class="block font-semibold text-surface-800">Bib {{ r.bibNumber }}</span> }
                      </td>
                    </ng-container>
                    <ng-container matColumnDef="athlete">
                      <th mat-header-cell *matHeaderCellDef>Athlete</th>
                      <td mat-cell *matCellDef="let r">
                        <p class="font-medium text-surface-900">{{ r.athleteName }}</p>
                        <p class="text-xs text-surface-400">{{ r.clubName || 'Independent' }} · {{ r.licenseNumber }}</p>
                      </td>
                    </ng-container>
                    <ng-container matColumnDef="event">
                      <th mat-header-cell *matHeaderCellDef>Event</th>
                      <td mat-cell *matCellDef="let r" class="text-sm">{{ r.eventName }}</td>
                    </ng-container>
                    <ng-container matColumnDef="seed">
                      <th mat-header-cell *matHeaderCellDef>Seed</th>
                      <td mat-cell *matCellDef="let r" class="text-sm">{{ r.seedValue | performance:r.seedUnit }}</td>
                    </ng-container>
                    <ng-container matColumnDef="status">
                      <th mat-header-cell *matHeaderCellDef>Status</th>
                      <td mat-cell *matCellDef="let r">
                        <app-status-chip [status]="r.status" />
                        @if (r.waitlistPosition) { <span class="text-xs text-surface-500 ml-1">#{{ r.waitlistPosition }}</span> }
                      </td>
                    </ng-container>
                    <ng-container matColumnDef="fee">
                      <th mat-header-cell *matHeaderCellDef>Fee</th>
                      <td mat-cell *matCellDef="let r" class="text-sm">
                        @if (r.feeAmount) {
                          <span [class]="r.feePaid ? 'text-green-700' : 'text-amber-700'">
                            {{ r.feePaid ? 'Paid' : 'Due' }}
                          </span>
                        } @else { — }
                      </td>
                    </ng-container>
                    <ng-container matColumnDef="actions">
                      <th mat-header-cell *matHeaderCellDef></th>
                      <td mat-cell *matCellDef="let r" class="text-right">
                        @if (!isTerminal(r.status)) {
                          <button mat-icon-button [matMenuTriggerFor]="regMenu" [matMenuTriggerData]="{ r: r }">
                            <mat-icon>more_vert</mat-icon>
                          </button>
                        }
                      </td>
                    </ng-container>
                    <tr mat-header-row *matHeaderRowDef="registrationColumns"></tr>
                    <tr mat-row *matRowDef="let row; columns: registrationColumns"></tr>
                  </table>
                </div>
              }
            </div>
          </mat-tab>
        }

        <!-- ── Results ──────────────────────────────────────────── -->
        <mat-tab label="Results">
          <div class="p-4">
            <div class="flex flex-wrap gap-3 items-center mb-3">
              <mat-form-field appearance="outline" class="w-72" subscriptSizing="dynamic">
                <mat-label>Event</mat-label>
                <mat-select [formControl]="boardEventCtrl">
                  @for (e of events(); track e.id) { <mat-option [value]="e.id">{{ e.name }}</mat-option> }
                </mat-select>
              </mat-form-field>
              <span class="flex-1"></span>
              @if (isStaff() && boardEventCtrl.value) {
                <a mat-stroked-button color="primary" routerLink="/admin/results/new"
                   [queryParams]="{ competitionId: c.id, eventId: boardEventCtrl.value }">
                  <mat-icon>edit_note</mat-icon> Enter results
                </a>
              }
            </div>
            @if (!boardEventCtrl.value) {
              <app-empty-state icon="leaderboard" title="Pick an event" subtitle="Choose an event to see its standings." />
            } @else if (leaderboard().length === 0) {
              <app-empty-state icon="leaderboard" title="No results yet" subtitle="Standings appear as soon as results are recorded." />
            } @else {
              <ol class="divide-y divide-surface-100">
                @for (rk of leaderboard(); track rk.id) {
                  <li class="flex items-center gap-4 py-3 px-2">
                    <span class="w-9 h-9 rounded-full flex items-center justify-center font-bold text-sm flex-shrink-0"
                          [ngClass]="medalClass(rk.medal)">{{ rk.rankPosition }}</span>
                    <div class="flex-1 min-w-0">
                      <a [routerLink]="[athleteBase, rk.athleteId]" class="font-medium text-surface-900 hover:text-primary-600">
                        {{ rk.athleteName }}
                      </a>
                      <p class="text-xs text-surface-400">{{ rk.clubName || 'Independent' }}</p>
                    </div>
                    <span class="font-mono font-semibold text-surface-900">
                      {{ rk.performanceText || (rk.performanceValue | performance:boardUnit()) }}
                    </span>
                    @if (!rk.official) {
                      <span class="text-[10px] uppercase tracking-wide text-amber-600" matTooltip="Not yet official">prov.</span>
                    }
                  </li>
                }
              </ol>
            }
          </div>
        </mat-tab>
      </mat-tab-group>

      <mat-menu #regMenu="matMenu">
        <ng-template matMenuContent let-r="r">
          @if (isStaff()) {
            @if (r.status !== 'CONFIRMED') {
              <button mat-menu-item (click)="setStatus(r, 'CONFIRMED')"><mat-icon>check_circle</mat-icon> Confirm</button>
            }
            @if (r.status === 'CONFIRMED' || r.status === 'WAITLISTED') {
              <button mat-menu-item (click)="setStatus(r, 'PENDING')"><mat-icon>hourglass_empty</mat-icon> Back to pending</button>
            }
            @if (r.status !== 'WAITLISTED' && r.status !== 'CONFIRMED') {
              <button mat-menu-item (click)="setStatus(r, 'WAITLISTED')"><mat-icon>queue</mat-icon> Move to waiting list</button>
            }
            <button mat-menu-item (click)="togglePaid(r)">
              <mat-icon>payments</mat-icon> {{ r.feePaid ? 'Mark fee unpaid' : 'Mark fee paid' }}
            </button>
            <mat-divider />
          }
          <button mat-menu-item (click)="cancel(r)"><mat-icon>cancel</mat-icon> Cancel registration</button>
          @if (isStaff() && r.status !== 'WAITLISTED') {
            <button mat-menu-item class="!text-red-600" (click)="disqualify(r)">
              <mat-icon class="!text-red-600">gpp_bad</mat-icon> Disqualify
            </button>
          }
        </ng-template>
      </mat-menu>
    } }
  `,
})
export class CompetitionDetailComponent implements OnInit {
  private api    = inject(ApiService);
  readonly auth  = inject(AuthService);
  private notify = inject(NotificationService);
  private dialog = inject(MatDialog);
  private route  = inject(ActivatedRoute);
  private router = inject(Router);
  readonly ctx   = pageContext('competitions', 'Competitions');
  readonly humanize = humanize;
  readonly athleteBase = this.ctx.backOffice ? '/admin/athletes' : '/athletes';

  readonly eventColumns        = ['name', 'category', 'schedule', 'entries', 'status', 'actions'];
  readonly registrationColumns = ['number', 'athlete', 'event', 'seed', 'status', 'fee', 'actions'];
  readonly registrationStatuses: RegistrationStatus[] = ['PENDING', 'CONFIRMED', 'WAITLISTED', 'CANCELLED', 'DISQUALIFIED'];

  competition   = signal<Competition | null>(null);
  events        = signal<CompetitionEvent[]>([]);
  registrations = signal<Registration[]>([]);
  leaderboard   = signal<Ranking[]>([]);
  myClubs       = signal<Club[]>([]);
  loading       = signal(true);
  tabIndex      = 0;

  regEventCtrl   = new FormControl('');
  regStatusCtrl  = new FormControl('');
  regClubCtrl    = new FormControl('');
  boardEventCtrl = new FormControl('');

  isStaff = () => this.auth.hasAnyRole([UserRole.ADMIN, UserRole.FEDERATION_STAFF]);
  isAdmin = () => this.auth.hasRole(UserRole.ADMIN);
  isManager = () => this.auth.hasRole(UserRole.CLUB_MANAGER);

  canSeeRegistrations = computed(() =>
    this.auth.isAuthenticated() && (this.isStaff() || (this.isManager() && this.myClubs().length > 0)));

  canRegister = computed(() => {
    const c = this.competition();
    if (!c || !this.auth.isAuthenticated() || this.events().length === 0) return false;
    if (this.isStaff()) return ['PUBLISHED', 'REGISTRATION_OPEN', 'REGISTRATION_CLOSED'].includes(c.status);
    return c.status === 'REGISTRATION_OPEN'
      && this.auth.hasAnyRole([UserRole.ATHLETE, UserRole.CLUB_MANAGER]);
  });

  boardUnit = computed(() => this.events().find(e => e.id === this.boardEventValue())?.scoringUnit);
  private boardEventValue = signal<string | null>(null);

  facts = computed(() => {
    const c = this.competition();
    if (!c) return [];
    return [
      { label: 'Dates',          value: `${fmt(c.startDate)} – ${fmt(c.endDate)}` },
      { label: 'Venue',          value: [c.venueName, c.venueCity].filter(Boolean).join(', ') || 'TBA' },
      { label: 'Registration',   value: c.registrationDeadline ? `until ${fmt(c.registrationDeadline)}` : '—' },
      { label: 'Entry fee',      value: c.entryFee ? `${c.entryFee} ${c.currency}` : 'Free' },
      { label: 'Capacity',       value: c.maxParticipants ? `${c.maxParticipants} athletes` : 'Unlimited' },
      { label: 'Per club',       value: c.maxPerClub ? `${c.maxPerClub} entries` : 'Unlimited' },
      { label: 'Host club',      value: c.hostClubName || '—' },
      { label: 'Confirmed',      value: `${c.confirmedRegistrations} athlete(s)` },
    ];
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.api.get<Competition>(`/competitions/${id}`).subscribe({
      next: c => { this.competition.set(c); this.loading.set(false); },
      error: () => { this.loading.set(false); this.router.navigate([this.ctx.base]); },
    });
    this.loadEvents(id);

    if (this.isManager()) {
      this.api.get<Club[]>('/clubs/mine').subscribe(clubs => {
        this.myClubs.set(clubs);
        if (clubs.length) this.regClubCtrl.setValue(clubs[0].id);
      });
    } else if (this.isStaff()) {
      this.loadRegistrations();
    }

    this.regEventCtrl.valueChanges.subscribe(() => this.loadRegistrations());
    this.regStatusCtrl.valueChanges.subscribe(() => this.loadRegistrations());
    this.regClubCtrl.valueChanges.subscribe(() => this.loadRegistrations());
    this.boardEventCtrl.valueChanges.subscribe(v => { this.boardEventValue.set(v); this.loadLeaderboard(); });
  }

  // ── Events ──────────────────────────────────────────────────────────────

  private loadEvents(competitionId = this.competition()?.id): void {
    if (!competitionId) return;
    this.api.get<CompetitionEvent[]>(`/competitions/${competitionId}/events`).subscribe(events => {
      this.events.set(events);
      if (!this.boardEventCtrl.value && events.length) this.boardEventCtrl.setValue(events[0].id);
    });
  }

  editEvent(event?: CompetitionEvent): void {
    this.dialog.open(EventDialogComponent, {
      width: '640px',
      data: { competitionId: this.competition()!.id, event },
    }).afterClosed().subscribe(saved => {
      if (!saved) return;
      this.notify.success(event ? 'Event updated.' : 'Event created.');
      this.loadEvents();
    });
  }

  deleteEvent(event: CompetitionEvent): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Delete event', message: `Delete "${event.name}"?`, confirmText: 'Delete', danger: true },
    }).afterClosed().subscribe(ok => {
      if (!ok) return;
      this.api.delete(`/competitions/${this.competition()!.id}/events/${event.id}`).subscribe(() => {
        this.notify.success('Event deleted.');
        this.loadEvents();
      });
    });
  }

  showLeaderboard(event: CompetitionEvent): void {
    this.boardEventCtrl.setValue(event.id);
    this.tabIndex = this.canSeeRegistrations() ? 3 : 2;
  }

  // ── Registrations ───────────────────────────────────────────────────────

  openRegister(eventId?: string): void {
    const c = this.competition()!;
    this.dialog.open<RegisterDialogComponent, unknown, RegisterDialogResult>(RegisterDialogComponent, {
      width: '560px',
      data: { competition: c, events: this.events(), eventId },
    }).afterClosed().subscribe(result => {
      if (!result) return;
      if (result.registration.status === 'WAITLISTED') this.notify.warning(result.message);
      else this.notify.success(result.message);
      this.loadEvents();
      this.loadRegistrations();
    });
  }

  loadRegistrations(): void {
    const c = this.competition() ?? { id: this.route.snapshot.paramMap.get('id')! };
    if (!this.isStaff() && !this.regClubCtrl.value) return;
    const params: Record<string, any> = { size: 200 };
    if (this.regEventCtrl.value)  params['eventId'] = this.regEventCtrl.value;
    if (this.regStatusCtrl.value) params['status']  = this.regStatusCtrl.value;
    if (!this.isStaff())          params['clubId']  = this.regClubCtrl.value;
    this.api.getPaged<Registration>(`/competitions/${c.id}/registrations`, params)
      .subscribe(p => this.registrations.set(p.content));
  }

  setStatus(r: Registration, status: RegistrationStatus, reason?: string): void {
    this.api.patch<Registration>(`/registrations/${r.id}/status`, { status, reason }).subscribe(() => {
      this.notify.success(`Registration ${humanize(status).toLowerCase()}.`);
      this.loadRegistrations();
      this.loadEvents();
    });
  }

  cancel(r: Registration): void {
    this.dialog.open(PromptDialogComponent, {
      data: { title: 'Cancel registration', message: `${r.athleteName} — ${r.eventName}`, label: 'Reason (optional)', confirmText: 'Cancel registration', danger: true },
    }).afterClosed().subscribe(reason => {
      if (reason !== undefined) this.setStatus(r, 'CANCELLED', reason || undefined);
    });
  }

  disqualify(r: Registration): void {
    this.dialog.open(PromptDialogComponent, {
      data: { title: 'Disqualify', message: `${r.athleteName} — ${r.eventName}`, label: 'Reason', required: true, confirmText: 'Disqualify', danger: true },
    }).afterClosed().subscribe(reason => {
      if (reason) this.setStatus(r, 'DISQUALIFIED', reason);
    });
  }

  togglePaid(r: Registration): void {
    this.api.patch(`/registrations/${r.id}`, { feePaid: !r.feePaid }).subscribe(() => this.loadRegistrations());
  }

  assignBibs(): void {
    this.api.post<{ assigned: number }>(`/competitions/${this.competition()!.id}/registrations/bibs`).subscribe(res => {
      this.notify.success(`${res.assigned} bib number(s) assigned to confirmed athletes.`);
      this.loadRegistrations();
    });
  }

  isTerminal(status: RegistrationStatus): boolean {
    return status === 'CANCELLED' || status === 'DISQUALIFIED';
  }

  // ── Results ─────────────────────────────────────────────────────────────

  private loadLeaderboard(): void {
    const eventId = this.boardEventCtrl.value;
    if (!eventId) { this.leaderboard.set([]); return; }
    this.api.get<Ranking[]>(`/results/event/${eventId}/rankings`).subscribe(rows => this.leaderboard.set(rows));
  }

  medalClass(medal?: string): string {
    switch (medal) {
      case 'GOLD':   return 'bg-amber-400 text-white';
      case 'SILVER': return 'bg-slate-300 text-slate-800';
      case 'BRONZE': return 'bg-orange-300 text-orange-900';
      default:       return 'bg-surface-100 text-surface-600';
    }
  }

  deleteCompetition(c: Competition): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Delete competition', message: `Delete "${c.name}" and its events?`, confirmText: 'Delete', danger: true },
    }).afterClosed().subscribe(ok => {
      if (!ok) return;
      this.api.delete(`/competitions/${c.id}`).subscribe(() => {
        this.notify.success('Competition deleted.');
        this.router.navigate([this.ctx.base]);
      });
    });
  }
}

function fmt(date: string): string {
  return new Date(date).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' });
}
