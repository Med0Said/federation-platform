import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { PageHeaderComponent } from '@shared/components/page-header/page-header.component';

@Component({
  selector: 'app-club-form',
  standalone: true,
  imports: [CommonModule, RouterModule, MatButtonModule, MatIconModule, PageHeaderComponent],
  template: `
    <app-page-header title="Club Form"
      [breadcrumbs]="[{ label: 'Admin', path: '/admin' }, { label: 'Clubs', path: '/admin/clubs' }, { label: 'Form' }]">
      <a mat-stroked-button routerLink="/admin/clubs" actions><mat-icon>arrow_back</mat-icon> Back</a>
    </app-page-header>
    <div class="card-padded flex flex-col items-center justify-center py-20 text-center">
      <mat-icon class="!text-5xl text-surface-200 mb-3">groups</mat-icon>
      <p class="text-sm text-surface-400">Club form — coming in the next phase.</p>
    </div>
  `,
})
export class ClubFormComponent {}
