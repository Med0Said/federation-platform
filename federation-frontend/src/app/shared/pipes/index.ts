import { Pipe, PipeTransform } from '@angular/core';
import { UserRole }            from '@core/models';

// ═══════════════════════════════════════════════════════════════════════════
// RelativeTimePipe — "3 hours ago", "2 days ago"
// ═══════════════════════════════════════════════════════════════════════════

@Pipe({ name: 'relativeTime', standalone: true })
export class RelativeTimePipe implements PipeTransform {

  transform(value: string | Date | null | undefined): string {
    if (!value) return '';
    const date = value instanceof Date ? value : new Date(value);
    const seconds = Math.floor((Date.now() - date.getTime()) / 1000);

    if (seconds < 60)   return 'just now';
    if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
    if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`;
    if (seconds < 2592000) return `${Math.floor(seconds / 86400)}d ago`;
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  }
}


// ═══════════════════════════════════════════════════════════════════════════
// TruncatePipe — "Some long text that gets…"
// ═══════════════════════════════════════════════════════════════════════════

@Pipe({ name: 'truncate', standalone: true })
export class TruncatePipe implements PipeTransform {

  transform(value: string | null | undefined, limit = 80, trail = '…'): string {
    if (!value) return '';
    return value.length > limit ? value.substring(0, limit).trimEnd() + trail : value;
  }
}


// ═══════════════════════════════════════════════════════════════════════════
// RoleLabelPipe — "ROLE_CLUB_MANAGER" → "Club Manager"
// ═══════════════════════════════════════════════════════════════════════════

@Pipe({ name: 'roleLabel', standalone: true })
export class RoleLabelPipe implements PipeTransform {

  private static readonly labels: Record<string, string> = {
    [UserRole.ADMIN]:            'Administrator',
    [UserRole.FEDERATION_STAFF]: 'Federation Staff',
    [UserRole.CLUB_MANAGER]:     'Club Manager',
    [UserRole.ATHLETE]:          'Athlete',
    [UserRole.PUBLIC]:           'Public User',
  };

  transform(role: UserRole | string | null | undefined): string {
    if (!role) return '';
    return RoleLabelPipe.labels[role]
        ?? String(role).replace('ROLE_', '').replace(/_/g, ' ').toLowerCase()
              .replace(/\b\w/g, l => l.toUpperCase());
  }
}


// ═══════════════════════════════════════════════════════════════════════════
// InitialsPipe — "Mohamed Ferjani" → "MF"
// ═══════════════════════════════════════════════════════════════════════════

@Pipe({ name: 'initials', standalone: true })
export class InitialsPipe implements PipeTransform {

  transform(fullName: string | null | undefined): string {
    if (!fullName?.trim()) return '?';
    return fullName.trim()
      .split(/\s+/)
      .slice(0, 2)
      .map(n => n[0].toUpperCase())
      .join('');
  }
}


// ═══════════════════════════════════════════════════════════════════════════
// PerformancePipe — formats "10.32" with unit → "10.32s" or "8.45m"
// ═══════════════════════════════════════════════════════════════════════════

@Pipe({ name: 'performance', standalone: true })
export class PerformancePipe implements PipeTransform {

  private static readonly unitSymbols: Record<string, string> = {
    seconds: 's',
    meters:  'm',
    points:  'pts',
  };

  transform(value: number | string | null | undefined, unit?: string): string {
    if (value === null || value === undefined) return '—';
    const symbol = unit ? (PerformancePipe.unitSymbols[unit] ?? unit) : '';
    return `${value}${symbol}`;
  }
}
