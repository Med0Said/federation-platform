import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { PageHeaderComponent } from '@shared/components/page-header/page-header.component';
import { EmptyStateComponent } from '@shared/components/empty-state/empty-state.component';

@Component({
  selector:   'app-my-registrations',
  standalone: true,
  imports:    [CommonModule, RouterModule, MatButtonModule, PageHeaderComponent, EmptyStateComponent],
  template: `
    <app-page-header
      title="My Registrations"
      subtitle="Track your competition registrations"
      [breadcrumbs]="[{ label: 'Portal' }, { label: 'My Registrations' }]">
      <a mat-flat-button color="primary" routerLink="/competitions" actions>
        Browse Competitions
      </a>
    </app-page-header>
    <div class="card">
      <app-empty-state
        icon="how_to_reg"
        title="No registrations yet"
        subtitle="Browse upcoming competitions and register to compete."
        actionLabel="View Competitions"
        (action)="null" />
    </div>
  `,
})
export class MyRegistrationsComponent {}
