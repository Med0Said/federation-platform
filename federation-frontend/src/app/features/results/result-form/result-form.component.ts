import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { PageHeaderComponent } from '@shared/components/page-header/page-header.component';
@Component({ selector: 'app-result-form', standalone: true,
  imports: [CommonModule, RouterModule, MatButtonModule, MatIconModule, PageHeaderComponent],
  template: `<app-page-header title="Enter Results" [breadcrumbs]="[{ label: 'Results', path: '/admin/results' }, { label: 'Form' }]"><a mat-stroked-button routerLink="/admin/results" actions><mat-icon>arrow_back</mat-icon> Back</a></app-page-header><div class="card-padded py-20 text-center"><mat-icon class="!text-5xl text-surface-200">leaderboard</mat-icon><p class="text-sm text-surface-400 mt-3">Result entry form — coming next.</p></div>`,
})
export class ResultFormComponent {}
