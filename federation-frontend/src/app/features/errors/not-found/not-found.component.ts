import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule }   from '@angular/material/icon';

@Component({
  selector:   'app-not-found',
  standalone: true,
  imports:    [CommonModule, RouterModule, MatButtonModule, MatIconModule],
  template: `
    <div class="min-h-screen flex items-center justify-center bg-surface-50 px-4">
      <div class="text-center max-w-md animate-fade-in">
        <p class="text-9xl font-black text-surface-200 leading-none select-none">404</p>
        <div class="mt-4 mb-8">
          <h1 class="text-2xl font-bold text-surface-900 mb-2">Page not found</h1>
          <p class="text-surface-500">
            The page you're looking for doesn't exist or has been moved.
          </p>
        </div>
        <div class="flex flex-wrap gap-3 justify-center">
          <a mat-flat-button color="primary" routerLink="/home" class="!rounded-xl">
            <mat-icon>home</mat-icon> Go Home
          </a>
          <button mat-stroked-button (click)="history.back()" class="!rounded-xl">
            <mat-icon>arrow_back</mat-icon> Go Back
          </button>
        </div>
      </div>
    </div>
  `,
})
export class NotFoundComponent {
  readonly history = window.history;
}
