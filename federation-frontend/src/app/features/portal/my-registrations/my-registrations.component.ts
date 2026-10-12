import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { HttpErrorResponse } from '@angular/common/http';

import { ApiService } from '@core/services/api.service';
import { NotificationService } from '@core/services/notification.service';
import { Registration } from '@core/models';
import { PageHeaderComponent } from '@shared/components/page-header/page-header.component';
import { StatusChipComponent } from '@shared/components/status-chip/status-chip.component';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';
import { EmptyStateComponent } from '@shared/components/empty-state/empty-state.component';
import { PromptDialogComponent } from '@shared/components/prompt-dialog/prompt-dialog.component';
import { PerformancePipe } from '@shared/pipes';

@Component({
  selector:   'app-my-registrations',
  standalone: true,
  imports: [
    CommonModule, RouterModule, MatButtonModule, MatIconModule,
    PageHeaderComponent, StatusChipComponent, LoadingSpinnerComponent, EmptyStateComponent, PerformancePipe,
  ],
  template: `
    <app-page-header
      title="My Registrations"
      subtitle="Competitions you are entered in"
      [breadcrumbs]="[{ label: 'Portal' }, { label: 'My Registrations' }]">
      <a mat-flat-button color="primary" routerLink="/competitions" actions>
        <mat-icon>search</mat-icon> Find a competition
      </a>
    </app-page-header>

    <div class="card relative">
      @if (loading()) { <app-loading-spinner [overlay]="true" /> }
      @if (noProfile()) {
        <app-empty-state icon="person_off" title="No athlete profile linked"
          subtitle="Your account is not linked to an athlete licence yet. Contact your club or the federation." />
      } @else if (!loading() && registrations().length === 0) {
        <app-empty-state icon="how_to_reg" title="No registrations yet"
          subtitle="Open a competition with registration open and press “Register”." />
      } @else {
        <ul class="divide-y divide-surface-100">
          @for (r of registrations(); track r.id) {
            <li class="flex flex-wrap items-center gap-4 px-6 py-4">
              <div class="flex-1 min-w-56">
                <a [routerLink]="['/competitions', r.competitionId]" class="font-semibold text-surface-900 hover:text-primary-600">
                  {{ r.competitionName }}
                </a>
                <p class="text-sm text-surface-600">{{ r.eventName }}</p>
                <p class="text-xs text-surface-400 mt-0.5">
                  {{ r.registrationNumber }}
                  @if (r.competitionStartDate) { · {{ r.competitionStartDate | date:'d MMM y' }} }
                  @if (r.seedValue) { · seed {{ r.seedValue | performance:r.seedUnit }} }
                  @if (r.bibNumber) { · <strong>bib {{ r.bibNumber }}</strong> }
                </p>
                @if (r.cancellationReason) {
                  <p class="text-xs text-red-600 mt-0.5">{{ r.cancellationReason }}</p>
                }
              </div>
              <div class="text-right">
                <app-status-chip [status]="r.status" />
                @if (r.waitlistPosition) {
                  <p class="text-xs text-blue-600 mt-1">Position {{ r.waitlistPosition }} on the waiting list</p>
                }
                @if (r.feeAmount) {
                  <p class="text-xs mt-1" [class]="r.feePaid ? 'text-green-700' : 'text-amber-700'">
                    Fee {{ r.feeAmount }} {{ r.feeCurrency }} — {{ r.feePaid ? 'paid' : 'due' }}
                  </p>
                }
              </div>
              @if ((r.status === 'PENDING' || r.status === 'CONFIRMED' || r.status === 'WAITLISTED')
                   && !['ONGOING', 'COMPLETED', 'CANCELLED'].includes(r.competitionStatus ?? '')) {
                <button mat-stroked-button color="warn" (click)="withdraw(r)">Withdraw</button>
              }
            </li>
          }
        </ul>
      }
    </div>
  `,
})
export class MyRegistrationsComponent implements OnInit {
  private api    = inject(ApiService);
  private notify = inject(NotificationService);
  private dialog = inject(MatDialog);

  registrations = signal<Registration[]>([]);
  loading       = signal(true);
  noProfile     = signal(false);

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.api.getPaged<Registration>('/registrations/me', { size: 100 }).subscribe({
      next: p => { this.registrations.set(p.content); this.loading.set(false); },
      error: (e: HttpErrorResponse) => { this.noProfile.set(e.status === 404); this.loading.set(false); },
    });
  }

  withdraw(r: Registration): void {
    this.dialog.open(PromptDialogComponent, {
      data: {
        title: 'Withdraw registration',
        message: `${r.competitionName} — ${r.eventName}`,
        label: 'Reason (optional)',
        confirmText: 'Withdraw',
        danger: true,
      },
    }).afterClosed().subscribe(reason => {
      if (reason === undefined) return;
      this.api.patch(`/registrations/${r.id}/status`, { status: 'CANCELLED', reason: reason || null }).subscribe(() => {
        this.notify.success('Registration withdrawn.');
        this.load();
      });
    });
  }
}
