import { Component, HostListener, signal, inject } from '@angular/core';
import { CommonModule }    from '@angular/common';
import { RouterOutlet }    from '@angular/router';
import { MatIconModule }   from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

import { SidebarComponent }    from '@layouts/components/sidebar/sidebar.component';
import { UserMenuComponent }   from '@layouts/components/user-menu/user-menu.component';
import { BreadcrumbComponent } from '@layouts/components/breadcrumb/breadcrumb.component';
import { AuthService }         from '@core/services/auth.service';

/**
 * Admin layout shell — rendered for all /admin and /portal routes.
 *
 * Responsive behaviour:
 *  - Desktop (≥1024px): persistent sidebar, collapsible to icon-only width.
 *  - Mobile  (< 1024px): sidebar hidden by default, slides in as drawer on toggle.
 *
 * Structure:
 *   <sidebar />                   (sticky left panel)
 *   <div.main-area>
 *     <topbar>                    (search + user menu)
 *     <main>
 *       <breadcrumb />
 *       <router-outlet />         (feature content)
 *     </main>
 *   </div>
 */
@Component({
  selector:   'app-admin-layout',
  standalone: true,
  imports: [
    CommonModule, RouterOutlet,
    MatIconModule, MatButtonModule,
    SidebarComponent, UserMenuComponent, BreadcrumbComponent,
  ],
  template: `
    <div class="flex h-screen overflow-hidden bg-surface-50">

      <!-- ── Sidebar ──────────────────────────────────────────────────── -->
      <app-sidebar
        [collapsed]="sidebarCollapsed()"
        [open]="sidebarOpen()"
        [isMobile]="isMobile()"
        (close)="sidebarOpen.set(false)"
        (collapsedChange)="sidebarCollapsed.set($event)" />

      <!-- ── Main area ─────────────────────────────────────────────────── -->
      <div class="flex flex-col flex-1 min-w-0 overflow-hidden">

        <!-- Top bar -->
        <header class="flex items-center justify-between h-16 px-4 lg:px-6
                        bg-white border-b border-surface-200 flex-shrink-0 z-30">

          <!-- Left: mobile menu toggle + page title signal -->
          <div class="flex items-center gap-3">
            <button mat-icon-button
                    class="lg:hidden"
                    (click)="sidebarOpen.set(!sidebarOpen())"
                    aria-label="Toggle menu">
              <mat-icon>menu</mat-icon>
            </button>

            <!-- Search (desktop) -->
            <div class="hidden md:flex items-center gap-2 bg-surface-100 rounded-lg
                         px-3 py-2 w-64 xl:w-80">
              <mat-icon class="!text-lg text-surface-400">search</mat-icon>
              <input
                type="text"
                placeholder="Search athletes, clubs…"
                class="bg-transparent text-sm text-surface-700 placeholder:text-surface-400
                       outline-none flex-1 min-w-0"
                (keyup.enter)="onSearch($event)" />
            </div>
          </div>

          <!-- Right: user menu -->
          <app-user-menu />
        </header>

        <!-- Scrollable content -->
        <main class="flex-1 overflow-y-auto">
          <div class="page-container">
            <app-breadcrumb />
            <router-outlet />
          </div>
        </main>

      </div>
    </div>
  `,
})
export class AdminLayoutComponent {
  readonly auth = inject(AuthService);

  // ── Responsive state ────────────────────────────────────────────────────

  readonly sidebarCollapsed = signal<boolean>(false);
  readonly sidebarOpen      = signal<boolean>(false);   // mobile only
  readonly isMobile         = signal<boolean>(false);

  constructor() {
    this.checkViewport();
  }

  @HostListener('window:resize')
  onResize(): void {
    this.checkViewport();
  }

  private checkViewport(): void {
    const mobile = window.innerWidth < 1024;
    this.isMobile.set(mobile);
    if (!mobile) {
      this.sidebarOpen.set(false);     // close drawer on desktop
    }
  }

  onSearch(event: Event): void {
    const term = (event.target as HTMLInputElement).value.trim();
    if (term) {
      // Route to a global search page (implement when building features)
      console.log('Search:', term);
    }
  }
}
