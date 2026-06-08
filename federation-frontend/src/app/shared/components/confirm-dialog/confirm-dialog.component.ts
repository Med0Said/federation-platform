import { Component, Inject } from '@angular/core';
import { CommonModule }      from '@angular/common';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule }   from '@angular/material/button';
import { MatIconModule }     from '@angular/material/icon';

export interface ConfirmDialogData {
  title:       string;
  message:     string;
  confirmText?: string;
  cancelText?:  string;
  danger?:      boolean;   // shows confirm button in red
}

/**
 * Generic confirmation dialog.
 *
 * Usage:
 *   const ref = this.dialog.open(ConfirmDialogComponent, {
 *     data: { title: 'Delete Club', message: 'Are you sure?', danger: true }
 *   });
 *   ref.afterClosed().subscribe(confirmed => { if (confirmed) … });
 */
@Component({
  selector:   'app-confirm-dialog',
  standalone: true,
  imports:    [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <div class="p-6 max-w-md">
      <!-- Header -->
      <div class="flex items-start gap-4 mb-4">
        <div [class]="data.danger
          ? 'w-10 h-10 rounded-full bg-danger-light flex items-center justify-center flex-shrink-0'
          : 'w-10 h-10 rounded-full bg-primary-100 flex items-center justify-center flex-shrink-0'">
          <mat-icon [class]="data.danger ? 'text-danger-DEFAULT' : 'text-primary-600'">
            {{ data.danger ? 'warning' : 'help_outline' }}
          </mat-icon>
        </div>
        <div>
          <h2 class="text-lg font-semibold text-surface-900">{{ data.title }}</h2>
          <p class="mt-1 text-sm text-surface-500">{{ data.message }}</p>
        </div>
      </div>

      <!-- Actions -->
      <div class="flex justify-end gap-3 mt-6">
        <button mat-stroked-button (click)="onCancel()">
          {{ data.cancelText ?? 'Cancel' }}
        </button>
        <button
          mat-flat-button
          [color]="data.danger ? 'warn' : 'primary'"
          (click)="onConfirm()">
          {{ data.confirmText ?? 'Confirm' }}
        </button>
      </div>
    </div>
  `,
})
export class ConfirmDialogComponent {
  constructor(
    @Inject(MAT_DIALOG_DATA) public data: ConfirmDialogData,
    private dialogRef: MatDialogRef<ConfirmDialogComponent>
  ) {}

  onConfirm(): void { this.dialogRef.close(true);  }
  onCancel():  void { this.dialogRef.close(false); }
}
