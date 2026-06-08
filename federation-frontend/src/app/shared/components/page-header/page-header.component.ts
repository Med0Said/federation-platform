import { Component, Input, ContentChild, TemplateRef } from '@angular/core';
import { CommonModule }   from '@angular/common';
import { MatIconModule }  from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { RouterModule }   from '@angular/router';

export interface Breadcrumb {
  label: string;
  path?: string;
}

/**
 * Standardised page header — title, subtitle, breadcrumbs, and an actions slot.
 *
 * Usage:
 *   <app-page-header
 *     title="Athletes"
 *     subtitle="Manage federation athletes"
 *     [breadcrumbs]="[{ label: 'Admin', path: '/admin' }, { label: 'Athletes' }]">
 *     <button mat-flat-button color="primary" actions>Add Athlete</button>
 *   </app-page-header>
 */
@Component({
  selector:   'app-page-header',
  standalone: true,
  imports:    [CommonModule, MatIconModule, MatButtonModule, RouterModule],
  template: `
    <div class="mb-6">
      <!-- Breadcrumbs -->
      @if (breadcrumbs.length > 0) {
        <nav class="flex items-center gap-1 text-xs text-surface-400 mb-2">
          @for (crumb of breadcrumbs; track crumb.label; let last = $last) {
            @if (crumb.path && !last) {
              <a [routerLink]="crumb.path"
                 class="hover:text-primary-600 transition-colors">{{ crumb.label }}</a>
              <mat-icon class="text-xs !w-3 !h-3">chevron_right</mat-icon>
            } @else {
              <span [class]="last ? 'text-surface-700 font-medium' : ''">{{ crumb.label }}</span>
            }
          }
        </nav>
      }

      <!-- Title row -->
      <div class="flex items-center justify-between gap-4">
        <div>
          <h1 class="text-2xl font-bold text-surface-900">{{ title }}</h1>
          @if (subtitle) {
            <p class="mt-0.5 text-sm text-surface-500">{{ subtitle }}</p>
          }
        </div>

        <!-- Actions slot -->
        <div class="flex items-center gap-2 flex-shrink-0">
          <ng-content select="[actions]" />
        </div>
      </div>
    </div>
  `,
})
export class PageHeaderComponent {
  @Input() title       = '';
  @Input() subtitle    = '';
  @Input() breadcrumbs: Breadcrumb[] = [];
}
