import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { PageHeaderComponent } from '@shared/components/page-header/page-header.component';
@Component({ selector: 'app-competitions-list', standalone: true,
  imports: [CommonModule, RouterModule, MatButtonModule, MatIconModule, PageHeaderComponent],
  template: `<app-page-header title="Competitions" [breadcrumbs]="[{ label: 'Admin' }, { label: 'Competitions' }]"><a mat-flat-button color="primary" routerLink="new" actions><mat-icon>add</mat-icon> New</a></app-page-header><div class="card-padded py-20 text-center"><mat-icon class="!text-5xl text-surface-200">emoji_events</mat-icon><p class="text-sm text-surface-400 mt-3">Competitions list — coming next.</p></div>`,
})
export class CompetitionsListComponent {}
