import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule }   from '@angular/material/icon';

@Component({
  selector:   'app-home',
  standalone: true,
  imports:    [CommonModule, RouterModule, MatButtonModule, MatIconModule],
  template: `
    <!-- Hero -->
    <section class="relative bg-gradient-to-br from-primary-700 via-primary-600 to-primary-800
                    text-white overflow-hidden">
      <div class="absolute inset-0 opacity-10"
           style="background-image: url('data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 width=%2260%22 height=%2260%22><circle cx=%2230%22 cy=%2230%22 r=%221.5%22 fill=%22white%22/></svg>')">
      </div>
      <div class="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-24 lg:py-32">
        <div class="max-w-3xl">
          <div class="inline-flex items-center gap-2 bg-white/20 rounded-full px-4 py-1.5 text-sm
                      font-medium mb-6 backdrop-blur-sm">
            <mat-icon class="!text-base">emoji_events</mat-icon>
            Official Sports Federation Platform
          </div>
          <h1 class="text-4xl sm:text-5xl lg:text-6xl font-extrabold leading-tight mb-6">
            Manage Athletics<br/>
            <span class="text-accent-300">At National Scale</span>
          </h1>
          <p class="text-lg text-primary-100 max-w-xl mb-8 leading-relaxed">
            Athletes, clubs, competitions and results — all managed from a single,
            modern platform built for the national sports federation.
          </p>
          <div class="flex flex-wrap gap-3">
            <a mat-flat-button routerLink="/competitions"
               class="!bg-white !text-primary-700 !font-semibold !px-6 !py-3 !rounded-xl">
              View Competitions
            </a>
            <a mat-stroked-button routerLink="/auth/register"
               class="!border-white !text-white !px-6 !py-3 !rounded-xl">
              Join the Federation
            </a>
          </div>
        </div>
      </div>
    </section>

    <!-- Stats strip -->
    <section class="bg-white border-b border-surface-200">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div class="grid grid-cols-2 md:grid-cols-4 gap-6 text-center">
          @for (stat of stats; track stat.label) {
            <div>
              <p class="text-3xl font-extrabold text-primary-600">{{ stat.value }}</p>
              <p class="text-sm text-surface-500 mt-1">{{ stat.label }}</p>
            </div>
          }
        </div>
      </div>
    </section>

    <!-- Feature cards -->
    <section class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
      <div class="text-center mb-12">
        <h2 class="text-3xl font-bold text-surface-900">Everything you need</h2>
        <p class="mt-3 text-surface-500 max-w-xl mx-auto">
          One platform covering the full lifecycle of sports management.
        </p>
      </div>
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        @for (card of featureCards; track card.title) {
          <a [routerLink]="card.path"
             class="card-padded group hover:shadow-card-md hover:-translate-y-0.5
                    transition-all cursor-pointer">
            <div [class]="'w-12 h-12 rounded-xl flex items-center justify-center mb-4 ' + card.bg">
              <mat-icon [class]="'!text-2xl ' + card.color">{{ card.icon }}</mat-icon>
            </div>
            <h3 class="font-semibold text-surface-900 mb-1 group-hover:text-primary-600
                       transition-colors">
              {{ card.title }}
            </h3>
            <p class="text-sm text-surface-500 leading-relaxed">{{ card.desc }}</p>
          </a>
        }
      </div>
    </section>
  `,
})
export class HomeComponent {
  readonly stats = [
    { value: '1,200+', label: 'Registered Athletes' },
    { value: '45',     label: 'Active Clubs' },
    { value: '120+',   label: 'Competitions / Year' },
    { value: '8,000+', label: 'Recorded Results' },
  ];

  readonly featureCards = [
    {
      title: 'Competitions',
      desc:  'Browse upcoming and past competitions, schedules and venues.',
      icon:  'emoji_events', path: '/competitions',
      bg:    'bg-amber-100',  color: 'text-amber-600',
    },
    {
      title: 'Athletes',
      desc:  'Search athlete profiles, categories and personal bests.',
      icon:  'directions_run', path: '/athletes',
      bg:    'bg-green-100',   color: 'text-green-600',
    },
    {
      title: 'Clubs',
      desc:  'Discover clubs across regions and their rosters.',
      icon:  'groups', path: '/clubs',
      bg:    'bg-blue-100',  color: 'text-blue-600',
    },
    {
      title: 'Results',
      desc:  'Official results, rankings, and national records.',
      icon:  'leaderboard', path: '/results',
      bg:    'bg-purple-100', color: 'text-purple-600',
    },
    {
      title: 'News',
      desc:  'Latest federation news, press releases, and announcements.',
      icon:  'article', path: '/news',
      bg:    'bg-rose-100',   color: 'text-rose-600',
    },
    {
      title: 'My Account',
      desc:  'Register for competitions, track your results and manage your profile.',
      icon:  'manage_accounts', path: '/auth/login',
      bg:    'bg-teal-100',     color: 'text-teal-600',
    },
  ];
}
