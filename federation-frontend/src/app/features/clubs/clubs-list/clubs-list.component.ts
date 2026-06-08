import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { MatTableModule }    from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule }    from '@angular/material/input';
import { MatButtonModule }   from '@angular/material/button';
import { MatIconModule }     from '@angular/material/icon';
import { MatMenuModule }     from '@angular/material/menu';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { ApiService }              from '@core/services/api.service';
import { AuthService }             from '@core/services/auth.service';
import { UserRole }                from '@core/models';
import { PageHeaderComponent }     from '@shared/components/page-header/page-header.component';
import { StatusChipComponent }     from '@shared/components/status-chip/status-chip.component';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';
import { EmptyStateComponent }     from '@shared/components/empty-state/empty-state.component';

export interface Club {
  id: string;
  name: string;
  shortName?: string;
  city?: string;
  region?: string;
  country: string;
  status: string;
  foundedYear?: number;
  logoUrl?: string;
  activeAthletes?: number;
}

@Component({
  selector:   'app-clubs-list',
  standalone: true,
  imports: [
    CommonModule, RouterModule, ReactiveFormsModule,
    MatTableModule, MatPaginatorModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatMenuModule,
    PageHeaderComponent, StatusChipComponent, LoadingSpinnerComponent, EmptyStateComponent,
  ],
  template: `
    <app-page-header title="Clubs"
      [breadcrumbs]="[{ label: 'Admin', path: '/admin' }, { label: 'Clubs' }]">
      @if (canManage) {
        <a mat-flat-button color="primary" routerLink="new" actions>
          <mat-icon>add</mat-icon> New Club
        </a>
      }
    </app-page-header>

    <div class="card-padded mb-4">
      <mat-form-field appearance="outline" class="w-full max-w-md">
        <mat-label>Search clubs…</mat-label>
        <input matInput [formControl]="searchCtrl" autocomplete="off" />
        <mat-icon matPrefix>search</mat-icon>
      </mat-form-field>
    </div>

    <div class="card relative">
      @if (loading()) { <app-loading-spinner [overlay]="true" /> }
      @if (!loading() && clubs().length === 0) {
        <app-empty-state icon="groups" title="No clubs found"
          subtitle="Register your first club to get started."
          [actionLabel]="canManage ? 'New Club' : ''" />
      } @else {
        <!-- Grid layout for clubs -->
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 p-4">
          @for (club of clubs(); track club.id) {
            <a [routerLink]="[club.id]"
               class="border border-surface-200 rounded-xl p-4 hover:border-primary-300
                      hover:shadow-card-md transition-all group flex items-center gap-4">
              @if (club.logoUrl) {
                <img [src]="club.logoUrl" [alt]="club.name"
                     class="w-12 h-12 rounded-lg object-contain flex-shrink-0 bg-surface-50 p-1" />
              } @else {
                <div class="w-12 h-12 rounded-lg bg-primary-100 flex items-center justify-center
                            text-primary-700 font-bold text-lg flex-shrink-0">
                  {{ (club.shortName ?? club.name).charAt(0) }}
                </div>
              }
              <div class="flex-1 min-w-0">
                <p class="font-semibold text-surface-900 truncate group-hover:text-primary-600
                           transition-colors">{{ club.name }}</p>
                <p class="text-xs text-surface-400 mt-0.5">{{ club.city }}{{ club.region ? ', ' + club.region : '' }}</p>
                <div class="flex items-center gap-2 mt-1.5">
                  <app-status-chip [status]="club.status" />
                  @if (club.activeAthletes !== undefined) {
                    <span class="text-xs text-surface-500">{{ club.activeAthletes }} athletes</span>
                  }
                </div>
              </div>
            </a>
          }
        </div>
        <div class="border-t border-surface-100">
          <mat-paginator [length]="total()" [pageSize]="24" [pageSizeOptions]="[12,24,48]"
            [pageIndex]="pageIndex()" (page)="onPage($event)" showFirstLastButtons />
        </div>
      }
    </div>
  `,
})
export class ClubsListComponent implements OnInit {
  private api  = inject(ApiService);
  readonly auth = inject(AuthService);

  clubs     = signal<Club[]>([]);
  loading   = signal(true);
  total     = signal(0);
  pageIndex = signal(0);
  searchCtrl = new FormControl('');

  get canManage(): boolean {
    return this.auth.hasAnyRole([UserRole.ADMIN, UserRole.FEDERATION_STAFF, UserRole.CLUB_MANAGER]);
  }

  ngOnInit(): void {
    this.load();
    this.searchCtrl.valueChanges.pipe(debounceTime(350), distinctUntilChanged())
      .subscribe(() => { this.pageIndex.set(0); this.load(); });
  }

  load(): void {
    this.loading.set(true);
    const params: Record<string, any> = { page: this.pageIndex(), size: 24 };
    if (this.searchCtrl.value) params['search'] = this.searchCtrl.value;

    this.api.getPaged<Club>('/clubs', params).subscribe({
      next:  p => { this.clubs.set(p.content); this.total.set(p.totalElements); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  onPage(e: PageEvent): void { this.pageIndex.set(e.pageIndex); this.load(); }
}
