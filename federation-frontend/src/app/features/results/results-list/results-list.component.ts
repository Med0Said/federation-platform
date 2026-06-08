import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { PageHeaderComponent } from '@shared/components/page-header/page-header.component';
@Component({ selector: 'app-results-list', standalone: true,
  imports: [CommonModule, RouterModule, MatButtonModule, MatIconModule, PageHeaderComponent],
  template: `<app-page-header title="Results" [breadcrumbs]="[{ label: 'Admin' }, { label: 'Results' }]"><a mat-flat-button color="primary" routerLink="new" actions><mat-icon>add</mat-icon> Enter Results</a></app-page-header><div class="card-padded py-20 text-center"><mat-icon class="!text-5xl text-surface-200">leaderboard</mat-icon><p class="text-sm text-surface-400 mt-3">Results list — coming next.</p></div>`,
})
export class ResultsListComponent {}
