import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from '@layouts/components/navbar/navbar.component';

/**
 * Public layout shell — used for home, competitions, clubs, news,
 * and the auth (login/register) pages.
 *
 * Structure:
 *   <navbar>
 *   <main>
 *     <router-outlet>    ← feature content renders here
 *   </main>
 *   <footer>
 */
@Component({
  selector:   'app-public-layout',
  standalone: true,
  imports:    [CommonModule, RouterOutlet, NavbarComponent],
  template: `
    <div class="min-h-screen flex flex-col">

      <app-navbar />

      <main class="flex-1">
        <router-outlet />
      </main>

      <!-- Footer -->
      <footer class="bg-surface-800 text-surface-300 mt-auto">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
          <div class="grid grid-cols-1 md:grid-cols-4 gap-8">

            <!-- Brand -->
            <div class="md:col-span-2">
              <div class="flex items-center gap-2 mb-4">
                <div class="w-8 h-8 rounded-lg bg-primary-600 flex items-center justify-center">
                  <span class="text-white text-sm font-bold">SF</span>
                </div>
                <span class="font-bold text-white text-lg">Sports Federation</span>
              </div>
              <p class="text-sm text-surface-400 max-w-xs leading-relaxed">
                The official platform for managing athletes, clubs, and competitions
                of the national sports federation.
              </p>
            </div>

            <!-- Quick links -->
            <div>
              <h4 class="font-semibold text-white mb-3 text-sm uppercase tracking-wider">
                Explore
              </h4>
              <ul class="space-y-2 text-sm">
                @for (link of footerLinks; track link.label) {
                  <li>
                    <a [href]="link.path"
                       class="hover:text-white transition-colors">{{ link.label }}</a>
                  </li>
                }
              </ul>
            </div>

            <!-- Legal -->
            <div>
              <h4 class="font-semibold text-white mb-3 text-sm uppercase tracking-wider">
                Legal
              </h4>
              <ul class="space-y-2 text-sm">
                <li><a href="#" class="hover:text-white transition-colors">Privacy Policy</a></li>
                <li><a href="#" class="hover:text-white transition-colors">Terms of Service</a></li>
                <li><a href="#" class="hover:text-white transition-colors">Contact Us</a></li>
              </ul>
            </div>
          </div>

          <div class="mt-10 pt-6 border-t border-surface-700 flex flex-col sm:flex-row
                      items-center justify-between gap-4 text-xs text-surface-500">
            <p>&copy; {{ currentYear }} Sports Federation. All rights reserved.</p>
            <p>Built with Angular 19 &amp; Spring Boot 3</p>
          </div>
        </div>
      </footer>

    </div>
  `,
})
export class PublicLayoutComponent {
  readonly currentYear = new Date().getFullYear();

  readonly footerLinks = [
    { label: 'Competitions',  path: '/competitions' },
    { label: 'Clubs',         path: '/clubs' },
    { label: 'Athletes',      path: '/athletes' },
    { label: 'Results',       path: '/results' },
    { label: 'News',          path: '/news' },
  ];
}
