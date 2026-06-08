import { Component, Input, Output, EventEmitter, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, RouterLink, RouterLinkActive } from '@angular/router';
import { MatIconModule }   from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';

import { AuthService }     from '@core/services/auth.service';
import { UserRole, NavItem } from '@core/models';

@Component({
  selector:   'app-sidebar',
  standalone: true,
  imports: [
    CommonModule, RouterModule, RouterLink, RouterLinkActive,
    MatIconModule, MatButtonModule, MatTooltipModule,
  ],
  template: `
    <!-- ── Overlay (mobile) ─────────────────────────────────────────── -->
    @if (open && isMobile) {
      <div class="fixed inset-0 bg-black/30 z-40 lg:hidden"
           (click)="close.emit()"></div>
    }

    <!-- ── Sidebar panel ────────────────────────────────────────────── -->
    <aside [class]="sidebarClass">

      <!-- Logo / collapse toggle -->
      <div class="flex items-center px-4 h-16 border-b border-surface-100 flex-shrink-0">
        @if (!collapsed) {
          <a routerLink="/admin/dashboard"
             class="flex items-center gap-2 flex-1 min-w-0">
            <div class="w-8 h-8 rounded-lg bg-primary-600 flex items-center justify-center flex-shrink-0">
              <mat-icon class="text-white !text-lg">emoji_events</mat-icon>
            </div>
            <span class="font-bold text-surface-900 truncate">
              Sports<span class="text-primary-600">Fed</span>
            </span>
          </a>
        }
        <button mat-icon-button
                class="ml-auto flex-shrink-0"
                (click)="onToggleCollapse()"
                [matTooltip]="collapsed ? 'Expand sidebar' : 'Collapse sidebar'">
          <mat-icon>{{ collapsed ? 'menu_open' : 'menu' }}</mat-icon>
        </button>
      </div>

      <!-- Navigation -->
      <nav class="flex-1 overflow-y-auto py-4 px-2">
        @for (section of visibleSections; track section.label) {

          <!-- Section label -->
          @if (!collapsed && section.label) {
            <p class="px-3 mb-1 text-2xs font-semibold uppercase tracking-wider
                       text-surface-400">
              {{ section.label }}
            </p>
          }

          @for (item of section.items; track item.path) {
            <a [routerLink]="item.path"
               routerLinkActive="nav-item-active"
               [matTooltip]="collapsed ? item.label : ''"
               matTooltipPosition="right"
               class="flex items-center gap-3 px-3 py-2.5 rounded-lg mb-0.5 cursor-pointer
                      text-surface-600 hover:bg-surface-100 hover:text-surface-900
                      transition-all group"
               [class.justify-center]="collapsed">
              <mat-icon class="flex-shrink-0 !text-xl group-hover:text-primary-600">
                {{ item.icon }}
              </mat-icon>
              @if (!collapsed) {
                <span class="text-sm font-medium truncate">{{ item.label }}</span>
              }
            </a>
          }

          @if (!collapsed && !$last) {
            <div class="my-3 border-t border-surface-100"></div>
          }
        }
      </nav>

      <!-- User info footer -->
      @if (!collapsed) {
        <div class="border-t border-surface-100 p-4 flex-shrink-0">
          <div class="flex items-center gap-3">
            <div class="w-8 h-8 rounded-full bg-primary-600 flex items-center
                        justify-center text-white text-xs font-semibold flex-shrink-0">
              {{ initials }}
            </div>
            <div class="flex-1 min-w-0">
              <p class="text-sm font-medium text-surface-900 truncate">
                {{ auth.currentUser()?.fullName }}
              </p>
              <p class="text-xs text-surface-500 truncate">
                {{ auth.currentUser()?.email }}
              </p>
            </div>
            <button mat-icon-button
                    (click)="onLogout()"
                    matTooltip="Logout"
                    class="flex-shrink-0 !w-8 !h-8">
              <mat-icon class="!text-base text-surface-400">logout</mat-icon>
            </button>
          </div>
        </div>
      }
    </aside>
  `,
})
export class SidebarComponent {
  @Input()  collapsed = false;
  @Input()  open      = true;
  @Input()  isMobile  = false;
  @Output() close           = new EventEmitter<void>();
  @Output() collapsedChange = new EventEmitter<boolean>();

  readonly auth = inject(AuthService);

  get sidebarClass(): string {
    const base = 'flex flex-col bg-white border-r border-surface-200 transition-all duration-250 z-50';
    const width = this.collapsed ? 'w-sidebar-collapsed' : 'w-sidebar';
    const position = this.isMobile
      ? `fixed inset-y-0 left-0 ${this.open ? 'translate-x-0' : '-translate-x-full'} shadow-xl`
      : 'sticky top-0 h-screen flex-shrink-0';
    return `${base} ${width} ${position}`;
  }

  get initials(): string {
    const name = this.auth.currentUser()?.fullName ?? '';
    return name.split(' ').slice(0, 2).map(n => n[0]).join('').toUpperCase() || '?';
  }

  // ── Nav sections with role filtering ────────────────────────────────────

  private readonly allSections: Array<{ label: string; items: NavItem[] }> = [
    {
      label: 'Overview',
      items: [
        { label: 'Dashboard', path: '/admin/dashboard', icon: 'dashboard',
          roles: [UserRole.ADMIN, UserRole.FEDERATION_STAFF, UserRole.CLUB_MANAGER] },
      ],
    },
    {
      label: 'Management',
      items: [
        { label: 'Users',        path: '/admin/users',        icon: 'people',
          roles: [UserRole.ADMIN] },
        { label: 'Clubs',        path: '/admin/clubs',        icon: 'groups',
          roles: [UserRole.ADMIN, UserRole.FEDERATION_STAFF, UserRole.CLUB_MANAGER] },
        { label: 'Athletes',     path: '/admin/athletes',     icon: 'directions_run',
          roles: [UserRole.ADMIN, UserRole.FEDERATION_STAFF, UserRole.CLUB_MANAGER] },
        { label: 'Competitions', path: '/admin/competitions', icon: 'emoji_events',
          roles: [UserRole.ADMIN, UserRole.FEDERATION_STAFF] },
        { label: 'Results',      path: '/admin/results',      icon: 'leaderboard',
          roles: [UserRole.ADMIN, UserRole.FEDERATION_STAFF] },
        { label: 'News',         path: '/admin/news',         icon: 'article',
          roles: [UserRole.ADMIN, UserRole.FEDERATION_STAFF] },
      ],
    },
    {
      label: 'My Account',
      items: [
        { label: 'My Profile',       path: '/admin/profile',            icon: 'manage_accounts' },
        { label: 'My Registrations', path: '/portal/registrations',     icon: 'how_to_reg',
          roles: [UserRole.ATHLETE] },
        { label: 'My Results',       path: '/portal/results',           icon: 'bar_chart',
          roles: [UserRole.ATHLETE] },
      ],
    },
  ];

  get visibleSections() {
    return this.allSections
      .map(section => ({
        ...section,
        items: section.items.filter(item =>
          !item.roles || this.auth.hasAnyRole(item.roles)
        ),
      }))
      .filter(section => section.items.length > 0);
  }

  onToggleCollapse(): void {
    this.collapsed = !this.collapsed;
    this.collapsedChange.emit(this.collapsed);
  }

  onLogout(): void {
    this.auth.logout().subscribe();
  }
}
