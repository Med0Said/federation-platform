import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';

import { ApiService } from '@core/services/api.service';
import { NotificationService } from '@core/services/notification.service';
import { Competition, humanize, NEWS_CATEGORIES, NewsArticle, Tag } from '@core/models';
import { PageHeaderComponent } from '@shared/components/page-header/page-header.component';
import { LoadingSpinnerComponent } from '@shared/components/loading-spinner/loading-spinner.component';

@Component({
  selector: 'app-news-form',
  standalone: true,
  imports: [
    CommonModule, RouterModule, ReactiveFormsModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, MatIconModule, MatSlideToggleModule,
    PageHeaderComponent, LoadingSpinnerComponent,
  ],
  template: `
    <app-page-header
      [title]="isEdit ? 'Edit Article' : 'Write Article'"
      [breadcrumbs]="[
        { label: 'Admin', path: '/admin' },
        { label: 'News', path: '/admin/news' },
        { label: isEdit ? 'Edit' : 'New' }
      ]">
    </app-page-header>

    @if (initLoading()) {
      <app-loading-spinner message="Loading article…" />
    } @else {
      <form [formGroup]="form" class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div class="card-padded lg:col-span-2 space-y-2">
          <mat-form-field appearance="outline" class="w-full">
            <mat-label>Title</mat-label>
            <input matInput formControlName="title" maxlength="300" />
            @if (form.controls.title.hasError('required')) { <mat-error>Required</mat-error> }
          </mat-form-field>
          <mat-form-field appearance="outline" class="w-full">
            <mat-label>Excerpt</mat-label>
            <textarea matInput rows="2" formControlName="excerpt" placeholder="Short summary shown in lists"></textarea>
          </mat-form-field>
          <mat-form-field appearance="outline" class="w-full">
            <mat-label>Content</mat-label>
            <textarea matInput rows="16" formControlName="content"
                      placeholder="Write the article. Blank lines separate paragraphs."></textarea>
            @if (form.controls.content.hasError('required')) { <mat-error>Required</mat-error> }
          </mat-form-field>
        </div>

        <div class="space-y-4">
          <div class="card-padded space-y-2">
            <mat-form-field appearance="outline" class="w-full">
              <mat-label>Status</mat-label>
              <mat-select formControlName="status">
                @for (s of statuses; track s) { <mat-option [value]="s">{{ humanize(s) }}</mat-option> }
              </mat-select>
            </mat-form-field>
            <mat-form-field appearance="outline" class="w-full">
              <mat-label>Category</mat-label>
              <mat-select formControlName="category">
                @for (c of categories; track c) { <mat-option [value]="c">{{ humanize(c) }}</mat-option> }
              </mat-select>
            </mat-form-field>
            <mat-form-field appearance="outline" class="w-full">
              <mat-label>Tags</mat-label>
              <mat-select formControlName="tagIds" multiple>
                @for (t of tags(); track t.id) { <mat-option [value]="t.id">{{ t.name }}</mat-option> }
              </mat-select>
            </mat-form-field>
            <mat-form-field appearance="outline" class="w-full">
              <mat-label>Related competition</mat-label>
              <mat-select formControlName="relatedCompetitionId">
                <mat-option [value]="null">— None —</mat-option>
                @for (c of competitions(); track c.id) { <mat-option [value]="c.id">{{ c.name }}</mat-option> }
              </mat-select>
            </mat-form-field>
            <mat-form-field appearance="outline" class="w-full">
              <mat-label>Language</mat-label>
              <mat-select formControlName="language">
                <mat-option value="fr">Français</mat-option>
                <mat-option value="ar">العربية</mat-option>
                <mat-option value="en">English</mat-option>
              </mat-select>
            </mat-form-field>
            <div class="flex flex-col gap-2 pb-2">
              <mat-slide-toggle formControlName="featured">Featured on the home page</mat-slide-toggle>
              <mat-slide-toggle formControlName="pinned">Pinned</mat-slide-toggle>
            </div>
          </div>

          <div class="card-padded space-y-2">
            <mat-form-field appearance="outline" class="w-full">
              <mat-label>Cover image URL</mat-label>
              <input matInput formControlName="coverUrl" placeholder="https://" />
            </mat-form-field>
            @if (form.value.coverUrl) {
              <img [src]="form.value.coverUrl" alt="" class="w-full rounded-lg max-h-40 object-cover" />
            }
            <mat-form-field appearance="outline" class="w-full">
              <mat-label>Cover alt text</mat-label>
              <input matInput formControlName="coverAltText" />
            </mat-form-field>
          </div>

          <div class="flex gap-3 justify-end">
            <button mat-stroked-button type="button" routerLink="/admin/news">Cancel</button>
            <button mat-flat-button color="primary" type="button" (click)="save()" [disabled]="saving()">
              <mat-icon>save</mat-icon> {{ saving() ? 'Saving…' : 'Save' }}
            </button>
          </div>
        </div>
      </form>
    }
  `,
})
export class NewsFormComponent implements OnInit {
  private fb     = inject(FormBuilder);
  private api    = inject(ApiService);
  private notify = inject(NotificationService);
  private router = inject(Router);
  private route  = inject(ActivatedRoute);
  readonly humanize   = humanize;
  readonly categories = NEWS_CATEGORIES;
  readonly statuses   = ['DRAFT', 'REVIEW', 'PUBLISHED', 'ARCHIVED'];

  saving       = signal(false);
  initLoading  = signal(false);
  tags         = signal<Tag[]>([]);
  competitions = signal<Competition[]>([]);
  isEdit       = false;
  newsId?: string;

  form = this.fb.group({
    title:                ['', [Validators.required, Validators.maxLength(300)]],
    excerpt:              [''],
    content:              ['', Validators.required],
    status:               ['DRAFT'],
    category:             ['GENERAL'],
    language:             ['fr'],
    tagIds:               [[] as string[]],
    relatedCompetitionId: [null as string | null],
    coverUrl:             [''],
    coverAltText:         [''],
    featured:             [false],
    pinned:               [false],
  });

  ngOnInit(): void {
    this.newsId = this.route.snapshot.paramMap.get('id') ?? undefined;
    this.isEdit = !!this.newsId;
    this.api.get<Tag[]>('/news/tags').subscribe(t => this.tags.set(t));
    this.api.getPaged<Competition>('/competitions', { size: 100, sort: 'startDate,desc' })
      .subscribe(p => this.competitions.set(p.content));

    if (this.isEdit) {
      this.initLoading.set(true);
      this.api.get<NewsArticle>(`/news/${this.newsId}`).subscribe({
        next: n => {
          this.form.patchValue({ ...n, tagIds: (n.tags ?? []).map(t => t.id) } as any);
          this.initLoading.set(false);
        },
        error: () => this.initLoading.set(false),
      });
    }
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    const body = this.form.getRawValue();
    const req$ = this.isEdit
      ? this.api.put<NewsArticle>(`/news/${this.newsId}`, body)
      : this.api.post<NewsArticle>('/news', body);
    req$.subscribe({
      next: () => {
        this.notify.success(body.status === 'PUBLISHED' ? 'Article published.' : 'Article saved.');
        this.router.navigate(['/admin/news']);
      },
      error: () => this.saving.set(false),
    });
  }
}
