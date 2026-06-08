import { Component, inject } from '@angular/core';
import { CommonModule }      from '@angular/common';
import { RouterModule }      from '@angular/router';
import { MatButtonModule }   from '@angular/material/button';
import { MatIconModule }     from '@angular/material/icon';
import { MatMenuModule }     from '@angular/material/menu';
import { MatDividerModule }  from '@angular/material/divider';

import { AuthService }       from '@core/services/auth.service';
import { ThemeService }      from '@core/services/theme.service';
import { InitialsPipe, RoleLabelPipe }      from '@shared/pipes';
import { RoleBadgeComponent } from '@shared/components/status-chip/status-chip.component';

/**
 * Compact user avatar button + dropdown menu for the admin top bar.
 * Shows profile link, theme toggle, and logout.
 */
@Component({
  selector:   'app-user-menu',
  standalone: true,
  imports: [
    CommonModule, RouterModule,
    MatButtonModule, MatIconModule, MatMenuModule, MatDividerModule,
    InitialsPipe, RoleLabelPipe, RoleBadgeComponent,
  ],
  template: `
    <div class="flex items-center gap-2">

      <!-- Theme toggle -->
      <button mat-icon-button
              (click)="themeService.toggle()"
              [attr.aria-label]="themeService.isDark() ? 'Light mode' : 'Dark mode'">
        <mat-icon>{{ themeService.isDark() ? 'light_mode' : 'dark_mode' }}</mat-icon>
      </button>

      <!-- Notifications placeholder -->
      <button mat-icon-button aria-label="Notifications">
        <mat-icon>notifications_none</mat-icon>
      </button>

      <!-- Avatar trigger -->
      <button mat-button
              [matMenuTriggerFor]="menu"
              class="!px-2 !rounded-xl">
        <div class="flex items-center gap-2">
          <div class="w-8 h-8 rounded-full bg-primary-600 flex items-center
                      justify-center text-white text-xs font-bold flex-shrink-0">
            {{ auth.currentUser()?.fullName | initials }}
          </div>
          <div class="hidden md:block text-left">
            <p class="text-sm font-medium text-surface-900 leading-none">
              {{ auth.currentUser()?.firstName }}
            </p>
            <p class="text-xs text-surface-400 mt-0.5">
              {{ auth.currentUser()?.role | roleLabel }}
            </p>
          </div>
          <mat-icon class="!text-base text-surface-400">expand_more</mat-icon>
        </div>
      </button>

      <mat-menu #menu="matMenu" xPosition="before" class="!rounded-xl">
        <!-- User info header -->
        <div class="px-4 py-3 pointer-events-none">
          <p class="text-sm font-semibold text-surface-900">
            {{ auth.currentUser()?.fullName }}
          </p>
          <p class="text-xs text-surface-500 mt-0.5">{{ auth.currentUser()?.email }}</p>
          <app-role-badge [role]="auth.currentUser()?.role ?? ''" class="mt-1.5" />
        </div>

        <mat-divider />

        <a mat-menu-item routerLink="/admin/profile">
          <mat-icon>manage_accounts</mat-icon>
          My Profile
        </a>
        <a mat-menu-item routerLink="/admin/profile" [queryParams]="{ tab: 'security' }">
          <mat-icon>lock</mat-icon>
          Change Password
        </a>

        <mat-divider />

        <button mat-menu-item (click)="themeService.toggle()">
          <mat-icon>{{ themeService.isDark() ? 'light_mode' : 'dark_mode' }}</mat-icon>
          {{ themeService.isDark() ? 'Light Mode' : 'Dark Mode' }}
        </button>

        <mat-divider />

        <button mat-menu-item class="text-danger-DEFAULT" (click)="onLogout()">
          <mat-icon class="text-danger-DEFAULT">logout</mat-icon>
          Logout
        </button>
      </mat-menu>
    </div>
  `,
})
export class UserMenuComponent {
  readonly auth         = inject(AuthService);
  readonly themeService = inject(ThemeService);

  onLogout(): void {
    this.auth.logout().subscribe();
  }
}
