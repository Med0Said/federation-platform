import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';

export interface PromptDialogData {
  title:        string;
  message?:     string;
  label:        string;
  confirmText?: string;
  required?:    boolean;
  danger?:      boolean;
}

/**
 * Asks for a short text (e.g. a cancellation reason).
 * Closes with the entered text, or {@code undefined} when cancelled.
 */
@Component({
  selector: 'app-prompt-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content>
      @if (data.message) { <p class="text-sm text-surface-600 mb-4">{{ data.message }}</p> }
      <mat-form-field appearance="outline" class="w-full">
        <mat-label>{{ data.label }}</mat-label>
        <textarea matInput rows="3" [formControl]="text" cdkFocusInitial></textarea>
        @if (text.hasError('required')) { <mat-error>Required</mat-error> }
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-flat-button [color]="data.danger ? 'warn' : 'primary'" (click)="confirm()">
        {{ data.confirmText ?? 'Confirm' }}
      </button>
    </mat-dialog-actions>
  `,
})
export class PromptDialogComponent {
  readonly data = inject<PromptDialogData>(MAT_DIALOG_DATA);
  private ref   = inject(MatDialogRef<PromptDialogComponent, string>);
  text = new FormControl('', this.data.required ? Validators.required : []);

  confirm(): void {
    if (this.text.invalid) { this.text.markAsTouched(); return; }
    this.ref.close(this.text.value ?? '');
  }
}
