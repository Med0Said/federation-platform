import { Injectable, signal } from '@angular/core';

type Theme = 'light' | 'dark';

/**
 * Theme service — toggles the .dark class on <html> and persists preference.
 * Works with Tailwind's `darkMode: 'class'` and Angular Material's dark theme.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {

  private readonly STORAGE_KEY = 'fed_theme';
  readonly isDark = signal<boolean>(false);

  constructor() {
    this.loadSavedTheme();
  }

  toggle(): void {
    this.setTheme(this.isDark() ? 'light' : 'dark');
  }

  setTheme(theme: Theme): void {
    const html = document.documentElement;
    if (theme === 'dark') {
      html.classList.add('dark');
      this.isDark.set(true);
    } else {
      html.classList.remove('dark');
      this.isDark.set(false);
    }
    localStorage.setItem(this.STORAGE_KEY, theme);
  }

  private loadSavedTheme(): void {
    const saved = localStorage.getItem(this.STORAGE_KEY) as Theme | null;
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    this.setTheme(saved ?? (prefersDark ? 'dark' : 'light'));
  }
}
