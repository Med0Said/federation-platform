import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule }   from '@angular/common';
import { MatIconModule }  from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

/**
 * Empty state placeholder displayed when a list or table has no data.
 *
 * Usage:
 *   <app-empty-state
 *     icon="group"
 *     title="No athletes yet"
 *     subtitle="Add your first athlete to get started"
 *     actionLabel="Add Athlete"
 *     (action)="openAddDialog()" />
 */
@Component({
  selector:   'app-empty-state',
  standalone: true,
  imports:    [CommonModule, MatIconModule, MatButtonModule],
  template: `
    <div class="flex flex-col items-center justify-center py-16 px-6 text-center">
      <div class="w-16 h-16 rounded-full bg-surface-100 flex items-center justify-center mb-4">
        <mat-icon class="!text-4xl !w-10 !h-10 text-surface-300">{{ icon }}</mat-icon>
      </div>
      <h3 class="text-base font-semibold text-surface-700 mb-1">{{ title }}</h3>
      <p class="text-sm text-surface-400 max-w-xs">{{ subtitle }}</p>
      @if (actionLabel) {
        <button
          mat-flat-button
          color="primary"
          class="mt-6"
          (click)="action.emit()">
          {{ actionLabel }}
        </button>
      }
    </div>
  `,
})
export class EmptyStateComponent {
  @Input() icon        = 'inbox';
  @Input() title       = 'No data found';
  @Input() subtitle    = '';
  @Input() actionLabel = '';
  @Output() action     = new EventEmitter<void>();
}
