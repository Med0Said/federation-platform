import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PageHeaderComponent } from '@shared/components/page-header/page-header.component';
import { EmptyStateComponent } from '@shared/components/empty-state/empty-state.component';

@Component({
  selector:   'app-my-results',
  standalone: true,
  imports:    [CommonModule, PageHeaderComponent, EmptyStateComponent],
  template: `
    <app-page-header
      title="My Results"
      subtitle="Your competition results and personal bests"
      [breadcrumbs]="[{ label: 'Portal' }, { label: 'My Results' }]">
    </app-page-header>
    <div class="card">
      <app-empty-state
        icon="bar_chart"
        title="No results yet"
        subtitle="Your competition results will appear here once they are officially recorded." />
    </div>
  `,
})
export class MyResultsComponent {}
