import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatDialog } from '@angular/material/dialog';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { ApiService } from '@core/services/api.service';
import { NotificationService } from '@core/services/notification.service';
import { humanize, ManagedUser, UserRole, UserStatus } from '@core/models';
import { PageHeaderComponent } from '@shared/components/page-header/page-header.component';
import { RoleBadgeComponent, StatusChipComponent } from '@shared/components/status-chip/status-chip.component';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';
import { EmptyStateComponent } from '@shared/components/empty-state/empty-state.component';
import { InitialsPipe, RelativeTimePipe, RoleLabelPipe } from '@shared/pipes';
import { UserCreateDialogComponent } from '../user-create-dialog/user-create-dialog.component';

@Component({
  selector: 'app-users-list',
  standalone: true,
  imports: [
    CommonModule, RouterModule, ReactiveFormsModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, MatIconModule,
    MatTableModule, MatPaginatorModule,
    PageHeaderComponent, RoleBadgeComponent, StatusChipComponent, LoadingSpinnerComponent, EmptyStateComponent,
    InitialsPipe, RelativeTimePipe, RoleLabelPipe,
  ],
  template: `
    <app-page-header title="Users" subtitle="Accounts, roles and access"
      [breadcrumbs]="[{ label: 'Admin', path: '/admin' }, { label: 'Users' }]">
      <button mat-flat-button color="primary" (click)="create()" actions>
        <mat-icon>person_add</mat-icon> New User
      </button>
    </app-page-header>

    <div class="card-padded mb-4">
      <div class="flex flex-wrap gap-3 items-center">
        <mat-form-field appearance="outline" class="flex-1 min-w-48">
          <mat-label>Search name, e-mail or username…</mat-label>
          <input matInput [formControl]="searchCtrl" autocomplete="off" />
          <mat-icon matPrefix>search</mat-icon>
        </mat-form-field>
        <mat-form-field appearance="outline" class="w-52">
          <mat-label>Role</mat-label>
          <mat-select [formControl]="roleCtrl">
            <mat-option value="">All</mat-option>
            @for (r of roles; track r) { <mat-option [value]="r">{{ r | roleLabel }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline" class="w-52">
          <mat-label>Status</mat-label>
          <mat-select [formControl]="statusCtrl">
            <mat-option value="">All</mat-option>
            @for (s of statuses; track s) { <mat-option [value]="s">{{ humanize(s) }}</mat-option> }
          </mat-select>
        </mat-form-field>
      </div>
    </div>

    <div class="card relative">
      @if (loading()) { <app-loading-spinner [overlay]="true" message="Loading users…" /> }
      @if (!loading() && users().length === 0) {
        <app-empty-state icon="people" title="No users found" subtitle="Try adjusting the filters." />
      } @else {
        <div class="table-container">
          <table mat-table [dataSource]="users()" class="w-full">
            <ng-container matColumnDef="user">
              <th mat-header-cell *matHeaderCellDef class="!pl-6">User</th>
              <td mat-cell *matCellDef="let u" class="!pl-6 !py-3">
                <div class="flex items-center gap-3">
                  <div class="w-9 h-9 rounded-full bg-primary-100 flex items-center justify-center text-primary-700 text-xs font-semibold">
                    {{ u.fullName | initials }}
                  </div>
                  <div>
                    <p class="font-medium text-surface-900 text-sm">{{ u.fullName }}</p>
                    <p class="text-xs text-surface-400">{{ u.email }}</p>
                  </div>
                </div>
              </td>
            </ng-container>
            <ng-container matColumnDef="role">
              <th mat-header-cell *matHeaderCellDef>Role</th>
              <td mat-cell *matCellDef="let u"><app-role-badge [role]="u.role" /></td>
            </ng-container>
            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef>Status</th>
              <td mat-cell *matCellDef="let u"><app-status-chip [status]="u.status" /></td>
            </ng-container>
            <ng-container matColumnDef="lastLogin">
              <th mat-header-cell *matHeaderCellDef class="hidden lg:table-cell">Last login</th>
              <td mat-cell *matCellDef="let u" class="hidden lg:table-cell text-sm text-surface-500">
                {{ u.lastLogin ? (u.lastLogin | relativeTime) : 'Never' }}
              </td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="columns" class="!bg-surface-50"></tr>
            <tr mat-row *matRowDef="let row; columns: columns" class="hover:bg-surface-50 cursor-pointer"
                [routerLink]="[row.id]"></tr>
          </table>
        </div>
        <mat-paginator [length]="total()" [pageSize]="pageSize()" [pageSizeOptions]="[25, 50, 100]"
          [pageIndex]="pageIndex()" (page)="onPage($event)" showFirstLastButtons />
      }
    </div>
  `,
})
export class UsersListComponent implements OnInit {
  private api    = inject(ApiService);
  private notify = inject(NotificationService);
  private dialog = inject(MatDialog);
  private router = inject(Router);
  readonly humanize = humanize;
  readonly roles    = Object.values(UserRole);
  readonly statuses = Object.values(UserStatus);
  readonly columns  = ['user', 'role', 'status', 'lastLogin'];

  users     = signal<ManagedUser[]>([]);
  loading   = signal(true);
  total     = signal(0);
  pageIndex = signal(0);
  pageSize  = signal(25);

  searchCtrl = new FormControl('');
  roleCtrl   = new FormControl('');
  statusCtrl = new FormControl('');

  ngOnInit(): void {
    this.load();
    this.searchCtrl.valueChanges.pipe(debounceTime(350), distinctUntilChanged()).subscribe(() => this.reload());
    this.roleCtrl.valueChanges.subscribe(() => this.reload());
    this.statusCtrl.valueChanges.subscribe(() => this.reload());
  }

  load(): void {
    this.loading.set(true);
    const params: Record<string, any> = { page: this.pageIndex(), size: this.pageSize(), sort: 'createdAt,desc' };
    if (this.searchCtrl.value) params['search'] = this.searchCtrl.value;
    if (this.roleCtrl.value)   params['role']   = this.roleCtrl.value;
    if (this.statusCtrl.value) params['status'] = this.statusCtrl.value;
    this.api.getPaged<ManagedUser>('/users', params).subscribe({
      next: p => { this.users.set(p.content); this.total.set(p.totalElements); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  onPage(e: PageEvent): void { this.pageIndex.set(e.pageIndex); this.pageSize.set(e.pageSize); this.load(); }

  create(): void {
    this.dialog.open(UserCreateDialogComponent, { width: '560px' }).afterClosed().subscribe(user => {
      if (!user) return;
      this.notify.success(`Account created for ${user.fullName}.`);
      this.router.navigate(['/admin/users', user.id]);
    });
  }

  private reload(): void { this.pageIndex.set(0); this.load(); }
}
