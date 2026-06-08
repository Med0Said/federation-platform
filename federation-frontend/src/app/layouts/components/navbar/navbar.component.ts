import { Component, inject } from '@angular/core';
import { CommonModule }      from '@angular/common';
import { RouterModule, RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule }  from '@angular/material/toolbar';
import { MatButtonModule }   from '@angular/material/button';
import { MatIconModule }     from '@angular/material/icon';
import { MatMenuModule }     from '@angular/material/menu';

import { AuthService }       from '@core/services/auth.service';
import { ThemeService }      from '@core/services/theme.service';
import { UserRole }          from '@core/models';
import { InitialsPipe }      from '@shared/pipes';

interface NavLink {
  label: string;
  path:  string;
  exact?: boolean;
}

@Component({
  selector:   'app-navbar',
  standalone: true,
  imports: [
    CommonModule, RouterModule, RouterLink, RouterLinkActive,
    MatToolbarModule, MatButtonModule, MatIconModule, MatMenuModule,
    InitialsPipe,
  ],
  template: `
    <header class="sticky top-0 z-50 bg-white/95 backdrop-blur-sm shadow-navbar">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center justify-between h-16">

          <!-- ── Logo ─────────────────────────────────────────────────── -->
          <a routerLink="/" class="flex items-center gap-2.5 flex-shrink-0 group">
            <div class="w-8 h-8 rounded-lg bg-primary-600 flex items-center justify-center
                        group-hover:bg-primary-700 transition-colors">
              <mat-icon class="text-white !text-lg">emoji_events</mat-icon>
            </div>
            <span class="font-bold text-surface-900 text-lg leading-none">
              Sports<span class="text-primary-600">Fed</span>
            </span>
          </a>

          <!-- ── Desktop nav links ─────────────────────────────────────── -->
          <nav class="hidden md:flex items-center gap-1">
            @for (link of navLinks; track link.path) {
              <a [routerLink]="link.path"
                 routerLinkActive="text-primary-600 bg-primary-50"
                 [routerLinkActiveOptions]="{ exact: link.exact ?? false }"
                 class="px-3 py-2 rounded-lg text-sm font-medium text-surface-600
                        hover:text-primary-600 hover:bg-surface-100 transition-all">
                {{ link.label }}
              </a>
            }
          </nav>

          <!-- ── Right actions ─────────────────────────────────────────── -->
          <div class="flex items-center gap-2">

            <!-- Theme toggle -->
            <button mat-icon-button
                    (click)="themeService.toggle()"
                    [attr.aria-label]="themeService.isDark() ? 'Switch to light mode' : 'Switch to dark mode'">
              <mat-icon>{{ themeService.isDark() ? 'light_mode' : 'dark_mode' }}</mat-icon>
            </button>

            @if (auth.isAuthenticated()) {
              <!-- Admin dashboard shortcut -->
              @if (auth.canAccessAdmin()) {
                <a mat-stroked-button routerLink="/admin/dashboard"
                   class="hidden sm:inline-flex !text-sm">
                  <mat-icon class="!text-base mr-1">dashboard</mat-icon>
                  Dashboard
                </a>
              }

              <!-- User menu -->
              <button mat-icon-button [matMenuTriggerFor]="userMenu"
                      class="!rounded-full">
                <div class="w-8 h-8 rounded-full bg-primary-600 flex items-center
                            justify-center text-white text-xs font-semibold">
                  {{ auth.currentUser()?.fullName | initials }}
                </div>
              </button>

              <mat-menu #userMenu="matMenu" xPosition="before">
                <div class="px-4 py-3 border-b border-surface-100">
                  <p class="text-sm font-semibold text-surface-900">
                    {{ auth.currentUser()?.fullName }}
                  </p>
                  <p class="text-xs text-surface-500 mt-0.5">
                    {{ auth.currentUser()?.email }}
                  </p>
                </div>
                <a mat-menu-item routerLink="/admin/profile">
                  <mat-icon>person</mat-icon> My Profile
                </a>
                <button mat-menu-item (click)="onLogout()">
                  <mat-icon>logout</mat-icon> Logout
                </button>
              </mat-menu>

            } @else {
              <a mat-stroked-button routerLink="/auth/login"  class="!text-sm">Login</a>
              <a mat-flat-button   color="primary"
                 routerLink="/auth/register" class="!text-sm hidden sm:inline-flex">
                Register
              </a>
            }

            <!-- Mobile menu toggle -->
            <button mat-icon-button class="md:hidden" (click)="mobileOpen = !mobileOpen"
                    aria-label="Toggle menu">
              <mat-icon>{{ mobileOpen ? 'close' : 'menu' }}</mat-icon>
            </button>
          </div>
        </div>

        <!-- ── Mobile nav ─────────────────────────────────────────────── -->
        @if (mobileOpen) {
          <nav class="md:hidden border-t border-surface-100 py-2 animate-fade-in">
            @for (link of navLinks; track link.path) {
              <a [routerLink]="link.path"
                 routerLinkActive="text-primary-600 bg-primary-50"
                 (click)="mobileOpen = false"
                 class="flex items-center px-4 py-2.5 text-sm font-medium
                        text-surface-600 hover:bg-surface-50 rounded-lg mx-1">
                {{ link.label }}
              </a>
            }
          </nav>
        }
      </div>
    </header>
  `,
})
export class NavbarComponent {
  readonly auth         = inject(AuthService);
  readonly themeService = inject(ThemeService);

  mobileOpen = false;

  readonly navLinks: NavLink[] = [
    { label: 'Home',         path: '/home',         exact: true },
    { label: 'Competitions', path: '/competitions' },
    { label: 'Clubs',        path: '/clubs' },
    { label: 'Athletes',     path: '/athletes' },
    { label: 'Results',      path: '/results' },
    { label: 'News',         path: '/news' },
  ];

  onLogout(): void {
    this.auth.logout().subscribe();
  }
}
