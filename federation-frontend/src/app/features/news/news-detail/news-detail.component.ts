import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { ApiService } from '@core/services/api.service';
import { AuthService } from '@core/services/auth.service';
import { humanize, NewsArticle, UserRole } from '@core/models';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';
import { EmptyStateComponent } from '@shared/components/empty-state/empty-state.component';

@Component({
  selector: 'app-news-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, MatButtonModule, MatIconModule, LoadingSpinnerComponent, EmptyStateComponent],
  template: `
    @if (loading()) {
      <app-loading-spinner message="Loading article…" />
    } @else { @if (article(); as n) {
      <article class="max-w-3xl mx-auto">
        <a routerLink="/news" class="inline-flex items-center gap-1 text-sm text-surface-500 hover:text-primary-600 mb-4">
          <mat-icon class="!text-base !w-4 !h-4">arrow_back</mat-icon> All news
        </a>
        <p class="text-xs font-semibold uppercase tracking-wide text-primary-600">{{ humanize(n.category) }}</p>
        <h1 class="text-3xl font-bold text-surface-900 mt-1 leading-tight">{{ n.title }}</h1>
        <p class="text-sm text-surface-500 mt-2">
          {{ n.publishedAt | date:'d MMMM y' }}
          @if (n.authorName) { · {{ n.authorName }} }
          · {{ n.viewCount }} view{{ n.viewCount === 1 ? '' : 's' }}
        </p>

        @if (n.coverUrl) {
          <img [src]="n.coverUrl" [alt]="n.coverAltText || n.title" class="w-full rounded-xl mt-6 max-h-96 object-cover" />
        }
        @if (n.excerpt) {
          <p class="text-lg text-surface-700 mt-6 font-medium">{{ n.excerpt }}</p>
        }
        <!-- Content is rendered as text (paragraphs preserved) — never as raw HTML -->
        <div class="mt-6 text-surface-800 leading-relaxed whitespace-pre-line">{{ n.content }}</div>

        <div class="flex flex-wrap items-center gap-2 mt-8 pt-6 border-t border-surface-100">
          @for (t of n.tags ?? []; track t.id) {
            <span class="text-xs px-2.5 py-1 rounded-full text-white" [style.background-color]="t.color || '#64748b'">
              {{ t.name }}
            </span>
          }
          <span class="flex-1"></span>
          @if (n.relatedCompetitionId) {
            <a mat-stroked-button [routerLink]="['/competitions', n.relatedCompetitionId]">
              <mat-icon>emoji_events</mat-icon> {{ n.relatedCompetitionName || 'Competition' }}
            </a>
          }
          @if (canEdit) {
            <a mat-stroked-button [routerLink]="['/admin/news', n.id, 'edit']"><mat-icon>edit</mat-icon> Edit</a>
          }
        </div>
      </article>
    } @else {
      <app-empty-state icon="article" title="Article not found" subtitle="It may have been unpublished." />
    } }
  `,
})
export class NewsDetailComponent implements OnInit {
  private api   = inject(ApiService);
  private auth  = inject(AuthService);
  private route = inject(ActivatedRoute);
  readonly humanize = humanize;
  readonly canEdit  = this.auth.hasAnyRole([UserRole.ADMIN, UserRole.FEDERATION_STAFF]);

  article = signal<NewsArticle | null>(null);
  loading = signal(true);

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      this.loading.set(true);
      this.api.get<NewsArticle>(`/news/slug/${params.get('slug')}`).subscribe({
        next: n => { this.article.set(n); this.loading.set(false); },
        error: () => { this.article.set(null); this.loading.set(false); },
      });
    });
  }
}
