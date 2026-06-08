import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule }   from '@angular/common';
import { RouterModule }   from '@angular/router';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule }    from '@angular/material/input';
import { MatSelectModule }   from '@angular/material/select';
import { MatButtonModule }   from '@angular/material/button';
import { MatIconModule }     from '@angular/material/icon';
import { MatMenuModule }     from '@angular/material/menu';
import { MatDividerModule }  from '@angular/material/divider';
import { MatDialog }         from '@angular/material/dialog';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { ApiService }              from '@core/services/api.service';
import { AuthService }             from '@core/services/auth.service';
import { NotificationService }     from '@core/services/notification.service';
import { PagedResponse, UserRole } from '@core/models';
import { PageHeaderComponent }     from '@shared/components/page-header/page-header.component';
import { StatusChipComponent }     from '@shared/components/status-chip/status-chip.component';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';
import { EmptyStateComponent }     from '@shared/components/empty-state/empty-state.component';
import { ConfirmDialogComponent }  from '@shared/components/confirm-dialog/confirm-dialog.component';
import { InitialsPipe } from '@shared/pipes';

export interface Athlete {
  id: string;
  licenseNumber: string;
  firstName: string;
  lastName: string;
  fullName?: string;
  gender: string;
  category: string;
  status: string;
  clubName?: string;
  clubId?: string;
  nationality: string;
  dateOfBirth: string;
  photoUrl?: string;
  createdAt: string;
}

@Component({
  selector:   'app-athletes-list',
  standalone: true,
  imports: [
    CommonModule, RouterModule, ReactiveFormsModule,
    MatTableModule, MatPaginatorModule, MatSortModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatMenuModule, MatDividerModule,
    PageHeaderComponent, StatusChipComponent,
    LoadingSpinnerComponent, EmptyStateComponent,
    InitialsPipe,
  ],
  template: `
    <app-page-header
      title="Athletes"
      subtitle="Manage federation-registered athletes"
      [breadcrumbs]="[{ label: 'Admin', path: '/admin' }, { label: 'Athletes' }]">
      @if (canManage) {
        <a mat-flat-button color="primary" routerLink="new" actions>
          <mat-icon>person_add</mat-icon> Add Athlete
        </a>
      }
    </app-page-header>

    <!-- Filters -->
    <div class="card-padded mb-4">
      <div class="flex flex-wrap gap-3 items-center">
        <mat-form-field appearance="outline" class="flex-1 min-w-48">
          <mat-label>Search athletes…</mat-label>
          <input matInput [formControl]="searchCtrl" autocomplete="off" />
          <mat-icon matPrefix>search</mat-icon>
          @if (searchCtrl.value) {
            <button matSuffix mat-icon-button (click)="searchCtrl.reset()">
              <mat-icon>close</mat-icon>
            </button>
          }
        </mat-form-field>

        <mat-form-field appearance="outline" class="w-40">
          <mat-label>Gender</mat-label>
          <mat-select [formControl]="genderCtrl">
            <mat-option value="">All</mat-option>
            <mat-option value="MALE">Male</mat-option>
            <mat-option value="FEMALE">Female</mat-option>
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline" class="w-44">
          <mat-label>Category</mat-label>
          <mat-select [formControl]="categoryCtrl">
            <mat-option value="">All</mat-option>
            @for (cat of categories; track cat.value) {
              <mat-option [value]="cat.value">{{ cat.label }}</mat-option>
            }
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline" class="w-40">
          <mat-label>Status</mat-label>
          <mat-select [formControl]="statusCtrl">
            <mat-option value="">All</mat-option>
            <mat-option value="ACTIVE">Active</mat-option>
            <mat-option value="INACTIVE">Inactive</mat-option>
            <mat-option value="SUSPENDED">Suspended</mat-option>
            <mat-option value="INJURED">Injured</mat-option>
            <mat-option value="RETIRED">Retired</mat-option>
          </mat-select>
        </mat-form-field>

        <button mat-stroked-button (click)="clearFilters()"
                [disabled]="!hasActiveFilters()" class="h-14">
          <mat-icon>filter_list_off</mat-icon> Clear
        </button>
      </div>
    </div>

    <!-- Table card -->
    <div class="card relative">
      @if (loading()) {
        <app-loading-spinner [overlay]="true" message="Loading athletes…" />
      }

      @if (!loading() && athletes().length === 0) {
        <app-empty-state
          icon="directions_run"
          title="No athletes found"
          [subtitle]="hasActiveFilters() ? 'Try adjusting your filters.' : 'Add your first athlete to get started.'"
          [actionLabel]="canManage && !hasActiveFilters() ? 'Add Athlete' : ''" />
      } @else {
        <div class="table-container">
          <table mat-table [dataSource]="athletes()" matSort (matSortChange)="onSort($event)"
                 class="w-full">

            <!-- Avatar + Name -->
            <ng-container matColumnDef="name">
              <th mat-header-cell *matHeaderCellDef mat-sort-header="lastName"
                  class="!pl-6 !font-semibold !text-surface-600 !text-xs uppercase tracking-wide">
                Athlete
              </th>
              <td mat-cell *matCellDef="let a" class="!pl-6 !py-3">
                <div class="flex items-center gap-3">
                  @if (a.photoUrl) {
                    <img [src]="a.photoUrl" [alt]="a.fullName"
                         class="w-9 h-9 rounded-full object-cover flex-shrink-0" />
                  } @else {
                    <div class="w-9 h-9 rounded-full bg-primary-100 flex items-center
                                justify-center text-primary-700 text-xs font-semibold flex-shrink-0">
                      {{ (a.firstName + ' ' + a.lastName) | initials }}
                    </div>
                  }
                  <div>
                    <p class="font-medium text-surface-900 text-sm">{{ a.firstName }} {{ a.lastName }}</p>
                    <p class="text-xs text-surface-400">{{ a.licenseNumber }}</p>
                  </div>
                </div>
              </td>
            </ng-container>

            <!-- Club -->
            <ng-container matColumnDef="club">
              <th mat-header-cell *matHeaderCellDef
                  class="!font-semibold !text-surface-600 !text-xs uppercase tracking-wide">Club</th>
              <td mat-cell *matCellDef="let a" class="!py-3">
                <span class="text-sm text-surface-600">{{ a.clubName ?? '—' }}</span>
              </td>
            </ng-container>

            <!-- Category -->
            <ng-container matColumnDef="category">
              <th mat-header-cell *matHeaderCellDef mat-sort-header
                  class="!font-semibold !text-surface-600 !text-xs uppercase tracking-wide">Category</th>
              <td mat-cell *matCellDef="let a" class="!py-3">
                <span class="text-sm font-medium text-surface-700">{{ a.category }}</span>
                <span class="ml-1 text-xs text-surface-400">{{ a.gender === 'MALE' ? '♂' : '♀' }}</span>
              </td>
            </ng-container>

            <!-- Nationality -->
            <ng-container matColumnDef="nationality">
              <th mat-header-cell *matHeaderCellDef
                  class="!font-semibold !text-surface-600 !text-xs uppercase tracking-wide hidden lg:table-cell">
                Nationality
              </th>
              <td mat-cell *matCellDef="let a" class="!py-3 hidden lg:table-cell">
                <span class="text-sm text-surface-600">{{ a.nationality }}</span>
              </td>
            </ng-container>

            <!-- Status -->
            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef
                  class="!font-semibold !text-surface-600 !text-xs uppercase tracking-wide">Status</th>
              <td mat-cell *matCellDef="let a" class="!py-3">
                <app-status-chip [status]="a.status" />
              </td>
            </ng-container>

            <!-- Actions -->
            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef class="!w-14"></th>
              <td mat-cell *matCellDef="let a" class="!py-3 !pr-4">
                <button mat-icon-button [matMenuTriggerFor]="rowMenu" [matMenuTriggerData]="{ athlete: a }">
                  <mat-icon>more_vert</mat-icon>
                </button>
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="columns"
                class="!bg-surface-50 border-b border-surface-200"></tr>
            <tr mat-row *matRowDef="let row; columns: columns"
                class="hover:bg-surface-50 transition-colors cursor-pointer border-b border-surface-100 last:border-0"
                [routerLink]="[row.id]"></tr>
          </table>
        </div>

        <mat-paginator
          [length]="total()"
          [pageSize]="pageSize"
          [pageSizeOptions]="[10, 25, 50]"
          [pageIndex]="pageIndex()"
          (page)="onPage($event)"
          showFirstLastButtons />
      }
    </div>

    <!-- Row context menu -->
    <mat-menu #rowMenu="matMenu">
      <ng-template matMenuContent let-athlete="athlete">
        <a mat-menu-item [routerLink]="[athlete.id]">
          <mat-icon>visibility</mat-icon> View Profile
        </a>
        @if (canManage) {
          <a mat-menu-item [routerLink]="[athlete.id, 'edit']">
            <mat-icon>edit</mat-icon> Edit
          </a>
          <mat-divider />
          <button mat-menu-item class="!text-red-600" (click)="confirmDelete(athlete)">
            <mat-icon class="!text-red-600">delete</mat-icon> Delete
          </button>
        }
      </ng-template>
    </mat-menu>
  `,
})
export class AthletesListComponent implements OnInit {
  private api    = inject(ApiService);
  private auth   = inject(AuthService);
  private notify = inject(NotificationService);
  private dialog = inject(MatDialog);

  // ── State ──────────────────────────────────────────────────────────────
  athletes  = signal<Athlete[]>([]);
  loading   = signal(true);
  total     = signal(0);
  pageIndex = signal(0);

  readonly pageSize = 25;
  readonly columns  = ['name', 'club', 'category', 'nationality', 'status', 'actions'];

  readonly categories = [
    { value: 'YOUTH',        label: 'Youth (U18)' },
    { value: 'JUNIOR',       label: 'Junior (18–20)' },
    { value: 'SENIOR',       label: 'Senior (21–34)' },
    { value: 'MASTERS',      label: 'Masters (35–49)' },
    { value: 'GRAND_MASTERS',label: 'Grand Masters (50+)' },
  ];

  // ── Filter controls ────────────────────────────────────────────────────
  searchCtrl   = new FormControl('');
  genderCtrl   = new FormControl('');
  categoryCtrl = new FormControl('');
  statusCtrl   = new FormControl('ACTIVE');

  sortField = 'lastName';
  sortDir   = 'asc';

  get canManage(): boolean {
    return this.auth.hasAnyRole([UserRole.ADMIN, UserRole.FEDERATION_STAFF, UserRole.CLUB_MANAGER]);
  }

  hasActiveFilters(): boolean {
    return !!(this.searchCtrl.value || this.genderCtrl.value ||
              this.categoryCtrl.value || (this.statusCtrl.value !== 'ACTIVE'));
  }

  ngOnInit(): void {
    this.load();

    this.searchCtrl.valueChanges.pipe(debounceTime(350), distinctUntilChanged())
      .subscribe(() => this.resetAndLoad());

    this.genderCtrl.valueChanges.subscribe(()   => this.resetAndLoad());
    this.categoryCtrl.valueChanges.subscribe(()  => this.resetAndLoad());
    this.statusCtrl.valueChanges.subscribe(()    => this.resetAndLoad());
  }

  load(): void {
    this.loading.set(true);
    const params: Record<string, any> = {
      page:  this.pageIndex(),
      size:  this.pageSize,
      sort:  `${this.sortField},${this.sortDir}`,
    };
    if (this.searchCtrl.value)   params['search']   = this.searchCtrl.value;
    if (this.genderCtrl.value)   params['gender']   = this.genderCtrl.value;
    if (this.categoryCtrl.value) params['category'] = this.categoryCtrl.value;
    if (this.statusCtrl.value)   params['status']   = this.statusCtrl.value;

    this.api.getPaged<Athlete>('/athletes', params).subscribe({
      next: (page) => {
        this.athletes.set(page.content);
        this.total.set(page.totalElements);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  onPage(e: PageEvent): void {
    this.pageIndex.set(e.pageIndex);
    this.load();
  }

  onSort(e: Sort): void {
    this.sortField = e.active;
    this.sortDir   = e.direction || 'asc';
    this.resetAndLoad();
  }

  clearFilters(): void {
    this.searchCtrl.reset('');
    this.genderCtrl.reset('');
    this.categoryCtrl.reset('');
    this.statusCtrl.reset('ACTIVE');
  }

  confirmDelete(athlete: Athlete): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title:       'Delete Athlete',
        message:     `Permanently delete ${athlete.firstName} ${athlete.lastName}? This cannot be undone.`,
        confirmText: 'Delete',
        danger:      true,
      },
    });
    ref.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.api.delete(`/athletes/${athlete.id}`).subscribe({
          next: () => {
            this.notify.success('Athlete deleted.');
            this.resetAndLoad();
          },
        });
      }
    });
  }

  private resetAndLoad(): void {
    this.pageIndex.set(0);
    this.load();
  }
}
