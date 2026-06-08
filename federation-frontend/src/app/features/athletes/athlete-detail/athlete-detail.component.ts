import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule }    from '@angular/common';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule }   from '@angular/material/icon';
import { MatTabsModule }   from '@angular/material/tabs';

import { ApiService }              from '@core/services/api.service';
import { AuthService }             from '@core/services/auth.service';
import { UserRole }                from '@core/models';
import { PageHeaderComponent }     from '@shared/components/page-header/page-header.component';
import { StatusChipComponent }     from '@shared/components/status-chip/status-chip.component';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';
import { InitialsPipe }            from '@shared/pipes';
import { Athlete }                 from '../athletes-list/athletes-list.component';

@Component({
  selector:   'app-athlete-detail',
  standalone: true,
  imports: [
    CommonModule, RouterModule, MatButtonModule, MatIconModule, MatTabsModule,
    PageHeaderComponent, StatusChipComponent, LoadingSpinnerComponent, InitialsPipe,
  ],
  template: `
    @if (loading()) {
      <div class="flex justify-center py-20">
        <app-loading-spinner [size]="48" message="Loading athlete profile…" />
      </div>
    } @else if (athlete()) {
      <app-page-header
        [title]="athlete()!.firstName + ' ' + athlete()!.lastName"
        [subtitle]="athlete()!.licenseNumber"
        [breadcrumbs]="[
          { label: 'Admin', path: '/admin' },
          { label: 'Athletes', path: '/admin/athletes' },
          { label: athlete()!.firstName + ' ' + athlete()!.lastName }
        ]">
        @if (canManage) {
          <a mat-stroked-button [routerLink]="['..', athlete()!.id, 'edit']" actions>
            <mat-icon>edit</mat-icon> Edit
          </a>
        }
      </app-page-header>

      <div class="grid grid-cols-1 lg:grid-cols-4 gap-6">

        <!-- Profile card -->
        <div class="lg:col-span-1 space-y-4">
          <div class="card-padded text-center">
            @if (athlete()!.photoUrl) {
              <img [src]="athlete()!.photoUrl" [alt]="athlete()!.firstName"
                   class="w-24 h-24 rounded-full object-cover mx-auto mb-4" />
            } @else {
              <div class="w-24 h-24 rounded-full bg-primary-100 flex items-center justify-center
                          text-primary-700 text-2xl font-bold mx-auto mb-4">
                {{ (athlete()!.firstName + ' ' + athlete()!.lastName) | initials }}
              </div>
            }

            <h2 class="font-bold text-surface-900 text-lg">
              {{ athlete()!.firstName }} {{ athlete()!.lastName }}
            </h2>
            <p class="text-sm text-surface-500 mb-3">{{ athlete()!.clubName ?? 'Independent' }}</p>
            <app-status-chip [status]="athlete()!.status" />
          </div>

          <!-- Info -->
          <div class="card-padded space-y-3">
            @for (info of athleteInfo(); track info.label) {
              <div class="flex justify-between text-sm">
                <span class="text-surface-500">{{ info.label }}</span>
                <span class="font-medium text-surface-800">{{ info.value }}</span>
              </div>
            }
          </div>
        </div>

        <!-- Tabs -->
        <div class="lg:col-span-3">
          <mat-tab-group animationDuration="200ms" class="card">
            <mat-tab label="Competition History">
              <div class="p-6 text-center text-surface-400 py-16">
                <mat-icon class="!text-5xl text-surface-200">emoji_events</mat-icon>
                <p class="mt-3 text-sm">Competition history will appear here.</p>
              </div>
            </mat-tab>
            <mat-tab label="Results & Records">
              <div class="p-6 text-center text-surface-400 py-16">
                <mat-icon class="!text-5xl text-surface-200">leaderboard</mat-icon>
                <p class="mt-3 text-sm">Personal bests and records will appear here.</p>
              </div>
            </mat-tab>
            <mat-tab label="Club History">
              <div class="p-6 text-center text-surface-400 py-16">
                <mat-icon class="!text-5xl text-surface-200">history</mat-icon>
                <p class="mt-3 text-sm">Club transfer history will appear here.</p>
              </div>
            </mat-tab>
          </mat-tab-group>
        </div>
      </div>
    }
  `,
})
export class AthleteDetailComponent implements OnInit {
  private api   = inject(ApiService);
  private auth  = inject(AuthService);
  private route = inject(ActivatedRoute);

  athlete = signal<Athlete | null>(null);
  loading = signal(true);

  get canManage(): boolean {
    return this.auth.hasAnyRole([UserRole.ADMIN, UserRole.FEDERATION_STAFF, UserRole.CLUB_MANAGER]);
  }

  athleteInfo = () => {
    const a = this.athlete();
    if (!a) return [];
    return [
      { label: 'License',      value: a.licenseNumber },
      { label: 'Gender',       value: a.gender },
      { label: 'Category',     value: a.category },
      { label: 'Nationality',  value: a.nationality },
      { label: 'Date of Birth',value: a.dateOfBirth ? new Date(a.dateOfBirth).toLocaleDateString() : '—' },
    ];
  };

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.api.get<Athlete>(`/athletes/${id}`).subscribe({
      next:  a => { this.athlete.set(a); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }
}
