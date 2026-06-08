import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule }   from '@angular/common';
import { RouterModule }   from '@angular/router';
import { MatIconModule }  from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

import { AuthService }    from '@core/services/auth.service';
import { ApiService }     from '@core/services/api.service';
import { PageHeaderComponent } from '@shared/components/page-header/page-header.component';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';
import { RelativeTimePipe, RoleLabelPipe } from '@shared/pipes';
import { UserRole } from '@core/models';

interface StatCard {
  label:    string;
  value:    string | number;
  icon:     string;
  iconBg:   string;
  iconColor:string;
  change?:  string;
  path:     string;
}

@Component({
  selector:   'app-dashboard',
  standalone: true,
  imports: [
    CommonModule, RouterModule, MatIconModule, MatButtonModule,
    PageHeaderComponent, LoadingSpinnerComponent, RelativeTimePipe, RoleLabelPipe,
  ],
  template: `
    <app-page-header
      title="Dashboard"
      subtitle="Federation overview and quick actions"
      [breadcrumbs]="[{ label: 'Admin' }, { label: 'Dashboard' }]">
    </app-page-header>

    <!-- Welcome banner -->
    <div class="mb-6 p-5 rounded-xl bg-gradient-to-r from-primary-600 to-primary-700 text-white">
      <div class="flex items-center justify-between flex-wrap gap-4">
        <div>
          <p class="text-primary-100 text-sm mb-1">Good {{ greeting }},</p>
          <h2 class="text-xl font-bold">{{ auth.currentUser()?.fullName }}</h2>
          <p class="text-primary-200 text-sm mt-1">
            {{ auth.currentUser()?.role | roleLabel }}
          </p>
        </div>
        <div class="text-right">
          <p class="text-primary-100 text-xs">Last login</p>
          <p class="font-medium text-sm">
            {{ auth.currentUser()?.lastLogin | relativeTime }}
          </p>
        </div>
      </div>
    </div>

    <!-- Stat cards -->
    @if (loading()) {
      <div class="flex justify-center py-12">
        <app-loading-spinner [size]="48" message="Loading stats…" />
      </div>
    } @else {
      <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4 mb-8">
        @for (card of statCards; track card.label) {
          <a [routerLink]="card.path"
             class="stat-card hover:shadow-card-md hover:-translate-y-0.5 transition-all group">
            <div [class]="'stat-icon ' + card.iconBg">
              <mat-icon [class]="'!text-2xl ' + card.iconColor">{{ card.icon }}</mat-icon>
            </div>
            <div>
              <p class="stat-value group-hover:text-primary-600 transition-colors">
                {{ card.value }}
              </p>
              <p class="stat-label">{{ card.label }}</p>
              @if (card.change) {
                <p class="text-xs text-green-600 mt-0.5 font-medium">{{ card.change }}</p>
              }
            </div>
          </a>
        }
      </div>

      <!-- Quick actions -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">

        <!-- Quick links -->
        <div class="card-padded">
          <h3 class="font-semibold text-surface-800 mb-4">Quick Actions</h3>
          <div class="grid grid-cols-2 gap-3">
            @for (action of quickActions; track action.label) {
              @if (action.visible) {
                <a [routerLink]="action.path"
                   class="flex flex-col items-center gap-2 p-4 rounded-xl border border-surface-200
                          hover:border-primary-300 hover:bg-primary-50 transition-all group text-center">
                  <div [class]="'w-10 h-10 rounded-lg flex items-center justify-center ' + action.bg">
                    <mat-icon [class]="action.color + ' !text-xl'">{{ action.icon }}</mat-icon>
                  </div>
                  <span class="text-xs font-medium text-surface-700 group-hover:text-primary-700">
                    {{ action.label }}
                  </span>
                </a>
              }
            }
          </div>
        </div>

        <!-- Platform info -->
        <div class="card-padded">
          <h3 class="font-semibold text-surface-800 mb-4">Platform Status</h3>
          <div class="space-y-3">
            @for (item of statusItems; track item.label) {
              <div class="flex items-center justify-between py-2 border-b border-surface-100 last:border-0">
                <div class="flex items-center gap-2">
                  <div [class]="'w-2 h-2 rounded-full ' + item.dotColor"></div>
                  <span class="text-sm text-surface-600">{{ item.label }}</span>
                </div>
                <span class="text-sm font-medium text-surface-800">{{ item.value }}</span>
              </div>
            }
          </div>
        </div>
      </div>
    }
  `,
})
export class DashboardComponent implements OnInit {
  readonly auth    = inject(AuthService);
  private api      = inject(ApiService);

  loading = signal(true);

  get greeting(): string {
    const h = new Date().getHours();
    if (h < 12) return 'morning';
    if (h < 17) return 'afternoon';
    return 'evening';
  }

  roleLabel(role: string | undefined): string {
    const map: Record<string, string> = {
      ROLE_ADMIN: 'Administrator', ROLE_FEDERATION_STAFF: 'Federation Staff',
      ROLE_CLUB_MANAGER: 'Club Manager', ROLE_ATHLETE: 'Athlete',
    };
    return map[role ?? ''] ?? role ?? '';
  }

  statCards: StatCard[] = [
    { label: 'Active Athletes',  value: '—', icon: 'directions_run',
      iconBg: 'bg-green-500',   iconColor: 'text-white', path: '/admin/athletes' },
    { label: 'Active Clubs',     value: '—', icon: 'groups',
      iconBg: 'bg-blue-500',    iconColor: 'text-white', path: '/admin/clubs' },
    { label: 'Competitions',     value: '—', icon: 'emoji_events',
      iconBg: 'bg-amber-500',   iconColor: 'text-white', path: '/admin/competitions' },
    { label: 'Published News',   value: '—', icon: 'article',
      iconBg: 'bg-purple-500',  iconColor: 'text-white', path: '/admin/news' },
  ];

  readonly quickActions = [
    { label: 'Add Athlete',     path: '/admin/athletes/new',      icon: 'person_add',
      bg: 'bg-green-100',  color: 'text-green-600',
      visible: this.auth.hasAnyRole([UserRole.ADMIN, UserRole.FEDERATION_STAFF, UserRole.CLUB_MANAGER]) },
    { label: 'New Competition', path: '/admin/competitions/new',  icon: 'add_circle',
      bg: 'bg-amber-100',  color: 'text-amber-600',
      visible: this.auth.hasAnyRole([UserRole.ADMIN, UserRole.FEDERATION_STAFF]) },
    { label: 'Enter Results',   path: '/admin/results/new',       icon: 'leaderboard',
      bg: 'bg-purple-100', color: 'text-purple-600',
      visible: this.auth.hasAnyRole([UserRole.ADMIN, UserRole.FEDERATION_STAFF]) },
    { label: 'Write News',      path: '/admin/news/new',          icon: 'edit_note',
      bg: 'bg-rose-100',   color: 'text-rose-600',
      visible: this.auth.hasAnyRole([UserRole.ADMIN, UserRole.FEDERATION_STAFF]) },
    { label: 'Manage Users',    path: '/admin/users',             icon: 'manage_accounts',
      bg: 'bg-red-100',    color: 'text-red-600',
      visible: this.auth.hasRole(UserRole.ADMIN) },
    { label: 'My Profile',      path: '/admin/profile',           icon: 'person',
      bg: 'bg-teal-100',   color: 'text-teal-600',
      visible: true },
  ];

  readonly statusItems = [
    { label: 'API Status',       value: 'Operational', dotColor: 'bg-green-500' },
    { label: 'Angular Version',  value: '19.x',        dotColor: 'bg-blue-500'  },
    { label: 'Spring Boot',      value: '3.2.x',       dotColor: 'bg-green-500' },
    { label: 'Database',         value: 'PostgreSQL 16',dotColor: 'bg-green-500' },
  ];

  ngOnInit(): void {
    // Load live stats — gracefully degrade if endpoint not ready
    this.api.get<any>('/actuator/health').subscribe({
      next:  () => this.loading.set(false),
      error: () => this.loading.set(false),
    });

    // TODO: Load real counts via /admin/stats endpoint
    setTimeout(() => {
      this.statCards[0].value = '1,240';
      this.statCards[1].value = '45';
      this.statCards[2].value = '12';
      this.statCards[3].value = '38';
      this.loading.set(false);
    }, 600);
  }
}
