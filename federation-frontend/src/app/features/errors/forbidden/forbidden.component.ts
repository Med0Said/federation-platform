import { Component, inject } from '@angular/core';
import { CommonModule }  from '@angular/common';
import { RouterModule }  from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule }   from '@angular/material/icon';
import { AuthService }   from '@core/services/auth.service';
import { UserRole }      from '@core/models';

@Component({
  selector:   'app-forbidden',
  standalone: true,
  imports:    [CommonModule, RouterModule, MatButtonModule, MatIconModule],
  template: `
    <div class="min-h-screen flex items-center justify-center bg-surface-50 px-4">
      <div class="text-center max-w-md animate-fade-in">
        <div class="w-20 h-20 rounded-full bg-red-100 flex items-center
                    justify-center mx-auto mb-6">
          <mat-icon class="!text-4xl text-red-500">lock</mat-icon>
        </div>
        <p class="text-6xl font-black text-surface-200 leading-none select-none mb-4">403</p>
        <h1 class="text-2xl font-bold text-surface-900 mb-2">Access Denied</h1>
        <p class="text-surface-500 mb-8">
          You don't have permission to view this page.
          Contact your administrator if you believe this is a mistake.
        </p>
        <div class="flex flex-wrap gap-3 justify-center">
          <a mat-flat-button color="primary"
             [routerLink]="homeRoute"
             class="!rounded-xl">
            <mat-icon>dashboard</mat-icon> Go to Dashboard
          </a>
          <button mat-stroked-button (click)="history.back()" class="!rounded-xl">
            <mat-icon>arrow_back</mat-icon> Go Back
          </button>
        </div>
      </div>
    </div>
  `,
})
export class ForbiddenComponent {
  private auth = inject(AuthService);
  readonly history = window.history;

  get homeRoute(): string {
    if (this.auth.hasAnyRole([UserRole.ADMIN, UserRole.FEDERATION_STAFF])) return '/admin/dashboard';
    if (this.auth.hasRole(UserRole.CLUB_MANAGER)) return '/admin/clubs';
    if (this.auth.hasRole(UserRole.ATHLETE))      return '/portal/profile';
    return '/home';
  }
}
