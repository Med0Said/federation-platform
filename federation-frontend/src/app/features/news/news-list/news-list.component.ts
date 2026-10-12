import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatMenuModule } from '@angular/material/menu';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatDialog } from '@angular/material/dialog';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { ApiService } from '@core/services/api.service';
import { AuthService } from '@core/services/auth.service';
import { NotificationService } from '@core/services/notification.service';
import { humanize, NEWS_CATEGORIES, NewsArticle, UserRole } from '@core/models';
import { PageHeaderComponent } from '@shared/components/page-header/page-header.component';
import { StatusChipComponent } from '@shared/components/status-chip/status-chip.component';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';
import { EmptyStateComponent } from '@shared/components/empty-state/empty-state.component';
import { ConfirmDialogComponent } from '@shared/components/confirm-dialog/confirm-dialog.component';
import { TruncatePipe } from '@shared/pipes';
import { pageContext } from '@shared/utils/page-context';

@Component({
  selector: 'app-news-list',
  standalone: true,
  imports: [
    CommonModule, RouterModule, ReactiveFormsModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, MatIconModule,
    MatTableModule, MatMenuModule, MatPaginatorModule,
    PageHeaderComponent, StatusChipComponent, LoadingSpinnerComponent, EmptyStateComponent, TruncatePipe,
  ],
  template: `
    <app-page-header title="News"
      [subtitle]="ctx.backOffice ? 'Write, review and publish articles' : 'Latest from the federation'"
      [breadcrumbs]="ctx.crumbs()">
      @if (ctx.backOffice) {
        <a mat-flat-button color="primary" routerLink="new" actions>
          <mat-icon>edit_note</mat-icon> Write Article
        </a>
      }
    </app-page-header>

    <div class="card-padded mb-4">
      <div class="flex flex-wrap gap-3 items-center">
        <mat-form-field appearance="outline" class="flex-1 min-w-48">
          <mat-label>Search articles…</mat-label>
          <input matInput [formControl]="searchCtrl" autocomplete="off" />
          <mat-icon matPrefix>search</mat-icon>
        </mat-form-field>
        <mat-form-field appearance="outline" class="w-48">
          <mat-label>Category</mat-label>
          <mat-select [formControl]="categoryCtrl">
            <mat-option value="">All</mat-option>
            @for (c of categories; track c) { <mat-option [value]="c">{{ humanize(c) }}</mat-option> }
          </mat-select>
        </mat-form-field>
        @if (ctx.backOffice) {
          <mat-form-field appearance="outline" class="w-40">
            <mat-label>Status</mat-label>
            <mat-select [formControl]="statusCtrl">
              <mat-option value="">All</mat-option>
              @for (s of statuses; track s) { <mat-option [value]="s">{{ humanize(s) }}</mat-option> }
            </mat-select>
          </mat-form-field>
        }
      </div>
    </div>

    <div class="card relative">
      @if (loading()) { <app-loading-spinner [overlay]="true" message="Loading articles…" /> }
      @if (!loading() && articles().length === 0) {
        <app-empty-state icon="article" title="No articles found"
          [subtitle]="ctx.backOffice ? 'Write the first article.' : 'Come back soon for federation news.'" />
      } @else if (ctx.backOffice) {
        <div class="table-container">
          <table mat-table [dataSource]="articles()" class="w-full">
            <ng-container matColumnDef="title">
              <th mat-header-cell *matHeaderCellDef class="!pl-6">Title</th>
              <td mat-cell *matCellDef="let n" class="!pl-6 !py-3">
                <p class="font-medium text-surface-900">{{ n.title }}</p>
                <p class="text-xs text-surface-400">{{ humanize(n.category) }} · {{ n.authorName || 'Unknown author' }}</p>
              </td>
            </ng-container>
            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef>Status</th>
              <td mat-cell *matCellDef="let n"><app-status-chip [status]="n.status" /></td>
            </ng-container>
            <ng-container matColumnDef="date">
              <th mat-header-cell *matHeaderCellDef>Published</th>
              <td mat-cell *matCellDef="let n" class="text-sm text-surface-600">
                {{ n.publishedAt ? (n.publishedAt | date:'d MMM y') : '—' }}
              </td>
            </ng-container>
            <ng-container matColumnDef="views">
              <th mat-header-cell *matHeaderCellDef>Views</th>
              <td mat-cell *matCellDef="let n" class="text-sm text-surface-600">{{ n.viewCount }}</td>
            </ng-container>
            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef></th>
              <td mat-cell *matCellDef="let n" class="text-right">
                <button mat-icon-button [matMenuTriggerFor]="menu" [matMenuTriggerData]="{ n: n }">
                  <mat-icon>more_vert</mat-icon>
                </button>
              </td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="columns" class="!bg-surface-50"></tr>
            <tr mat-row *matRowDef="let row; columns: columns" class="hover:bg-surface-50"></tr>
          </table>
        </div>
        <mat-paginator [length]="total()" [pageSize]="pageSize()" [pageSizeOptions]="[12, 24, 48]"
          [pageIndex]="pageIndex()" (page)="onPage($event)" showFirstLastButtons />
      } @else {
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5 p-4">
          @for (n of articles(); track n.id) {
            <a [routerLink]="['/news', n.slug]"
               class="rounded-xl border border-surface-200 overflow-hidden hover:shadow-card-md hover:-translate-y-0.5
                      transition-all group flex flex-col">
              @if (n.coverUrl) {
                <img [src]="n.coverUrl" [alt]="n.coverAltText || n.title" class="h-40 w-full object-cover" />
              } @else {
                <div class="h-40 w-full bg-gradient-to-br from-primary-500 to-primary-700 flex items-center justify-center">
                  <mat-icon class="!text-5xl !w-12 !h-12 text-white/70">article</mat-icon>
                </div>
              }
              <div class="p-4 flex flex-col flex-1">
                <p class="text-xs font-semibold uppercase tracking-wide text-primary-600">{{ humanize(n.category) }}</p>
                <h3 class="font-semibold text-surface-900 mt-1 group-hover:text-primary-600 transition-colors">{{ n.title }}</h3>
                <p class="text-sm text-surface-500 mt-2 flex-1">{{ (n.excerpt || n.content) | truncate:140 }}</p>
                <p class="text-xs text-surface-400 mt-3">{{ n.publishedAt | date:'d MMMM y' }}</p>
              </div>
            </a>
          }
        </div>
        <mat-paginator [length]="total()" [pageSize]="pageSize()" [pageSizeOptions]="[12, 24, 48]"
          [pageIndex]="pageIndex()" (page)="onPage($event)" showFirstLastButtons />
      }
    </div>

    <mat-menu #menu="matMenu">
      <ng-template matMenuContent let-n="n">
        @if (n.status === 'PUBLISHED') {
          <a mat-menu-item [routerLink]="['/news', n.slug]"><mat-icon>visibility</mat-icon> View</a>
        }
        <a mat-menu-item [routerLink]="[n.id, 'edit']"><mat-icon>edit</mat-icon> Edit</a>
        @if (n.status !== 'PUBLISHED') {
          <button mat-menu-item (click)="publish(n)"><mat-icon>publish</mat-icon> Publish</button>
        }
        @if (isAdmin) {
          <button mat-menu-item class="!text-red-600" (click)="remove(n)">
            <mat-icon class="!text-red-600">delete</mat-icon> Delete
          </button>
        }
      </ng-template>
    </mat-menu>
  `,
})
export class NewsListComponent implements OnInit {
  private api    = inject(ApiService);
  private auth   = inject(AuthService);
  private notify = inject(NotificationService);
  private dialog = inject(MatDialog);
  readonly ctx   = pageContext('news', 'News');
  readonly humanize   = humanize;
  readonly categories = NEWS_CATEGORIES;
  readonly statuses   = ['DRAFT', 'REVIEW', 'PUBLISHED', 'ARCHIVED'];
  readonly columns    = ['title', 'status', 'date', 'views', 'actions'];
  readonly isAdmin    = this.auth.hasRole(UserRole.ADMIN);

  articles  = signal<NewsArticle[]>([]);
  loading   = signal(true);
  total     = signal(0);
  pageIndex = signal(0);
  pageSize  = signal(12);

  searchCtrl   = new FormControl('');
  categoryCtrl = new FormControl('');
  statusCtrl   = new FormControl('');

  ngOnInit(): void {
    this.load();
    this.searchCtrl.valueChanges.pipe(debounceTime(350), distinctUntilChanged()).subscribe(() => this.reload());
    this.categoryCtrl.valueChanges.subscribe(() => this.reload());
    this.statusCtrl.valueChanges.subscribe(() => this.reload());
  }

  load(): void {
    this.loading.set(true);
    const params: Record<string, any> = {
      page: this.pageIndex(), size: this.pageSize(),
      sort: this.ctx.backOffice ? 'createdAt,desc' : 'publishedAt,desc',
    };
    if (this.searchCtrl.value)   params['search']   = this.searchCtrl.value;
    if (this.categoryCtrl.value) params['category'] = this.categoryCtrl.value;
    if (this.statusCtrl.value)   params['status']   = this.statusCtrl.value;
    this.api.getPaged<NewsArticle>('/news', params).subscribe({
      next: p => { this.articles.set(p.content); this.total.set(p.totalElements); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  onPage(e: PageEvent): void { this.pageIndex.set(e.pageIndex); this.pageSize.set(e.pageSize); this.load(); }

  publish(n: NewsArticle): void {
    this.api.patch(`/news/${n.id}/publish`).subscribe(() => { this.notify.success('Article published.'); this.load(); });
  }

  remove(n: NewsArticle): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Delete article', message: `Delete "${n.title}"?`, confirmText: 'Delete', danger: true },
    }).afterClosed().subscribe(ok => {
      if (ok) this.api.delete(`/news/${n.id}`).subscribe(() => { this.notify.success('Article deleted.'); this.load(); });
    });
  }

  private reload(): void { this.pageIndex.set(0); this.load(); }
}
