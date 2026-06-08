import { Component, inject, OnInit } from '@angular/core';
import { CommonModule }    from '@angular/common';
import { RouterModule }    from '@angular/router';
import { MatIconModule }   from '@angular/material/icon';
import { Router, ActivatedRoute, NavigationEnd } from '@angular/router';
import { filter, distinctUntilChanged } from 'rxjs/operators';

interface Crumb { label: string; url: string; }

/**
 * Auto-generates breadcrumbs from the current route tree.
 * Reads `data.breadcrumb` from each activated route snapshot.
 *
 * Route config example:
 *   { path: 'athletes', data: { breadcrumb: 'Athletes' }, ... }
 */
@Component({
  selector:   'app-breadcrumb',
  standalone: true,
  imports:    [CommonModule, RouterModule, MatIconModule],
  template: `
    @if (crumbs.length > 1) {
      <nav class="flex items-center gap-1 text-xs text-surface-400 mb-4" aria-label="Breadcrumb">
        @for (crumb of crumbs; track crumb.url; let last = $last) {
          @if (!last) {
            <a [routerLink]="crumb.url"
               class="hover:text-primary-600 transition-colors truncate max-w-32">
              {{ crumb.label }}
            </a>
            <mat-icon class="!text-xs !w-3 !h-3 flex-shrink-0">chevron_right</mat-icon>
          } @else {
            <span class="text-surface-700 font-medium truncate">{{ crumb.label }}</span>
          }
        }
      </nav>
    }
  `,
})
export class BreadcrumbComponent implements OnInit {
  private router = inject(Router);
  private route  = inject(ActivatedRoute);

  crumbs: Crumb[] = [];

  ngOnInit(): void {
    this.router.events.pipe(
      filter(e => e instanceof NavigationEnd),
      distinctUntilChanged(),
    ).subscribe(() => this.buildCrumbs());

    this.buildCrumbs();
  }

  private buildCrumbs(): void {
    this.crumbs = this.getCrumbs(this.route.root);
  }

  private getCrumbs(route: ActivatedRoute, url = '', crumbs: Crumb[] = []): Crumb[] {
    const routeUrl = route.snapshot.url.map(s => s.path).join('/');
    const nextUrl  = routeUrl ? `${url}/${routeUrl}` : url;

    const label = route.snapshot.data['breadcrumb'];
    if (label) {
      crumbs.push({ label, url: nextUrl });
    }

    if (route.firstChild) {
      return this.getCrumbs(route.firstChild, nextUrl, crumbs);
    }
    return crumbs;
  }
}
