import { Component, Input } from '@angular/core';
import { CommonModule }     from '@angular/common';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

/**
 * Reusable loading indicator.
 *
 * Usage:
 *   <app-loading-spinner />                        — centered, no overlay
 *   <app-loading-spinner [overlay]="true" />       — absolute overlay on parent
 *   <app-loading-spinner [size]="48" message="Saving…" />
 */
@Component({
  selector:   'app-loading-spinner',
  standalone: true,
  imports:    [CommonModule, MatProgressSpinnerModule],
  template: `
    <div
      [class]="overlay
        ? 'absolute inset-0 bg-white/75 flex flex-col items-center justify-center z-10 rounded-xl'
        : 'flex flex-col items-center justify-center py-12'">

      <mat-spinner [diameter]="size" color="primary" />

      @if (message) {
        <p class="mt-3 text-sm text-surface-500 animate-pulse">{{ message }}</p>
      }
    </div>
  `,
})
export class LoadingSpinnerComponent {
  @Input() overlay = false;
  @Input() size    = 40;
  @Input() message = '';
}
