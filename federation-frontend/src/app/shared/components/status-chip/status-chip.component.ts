import { Component, Input } from '@angular/core';
import { CommonModule }     from '@angular/common';
import { UserRole, UserStatus } from '@core/models';

// ═══════════════════════════════════════════════════════════════════════════
// StatusChipComponent
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Displays a coloured chip for entity status values.
 *
 * Usage:
 *   <app-status-chip [status]="athlete.status" />
 *   <app-status-chip status="ACTIVE" />
 */
@Component({
  selector:   'app-status-chip',
  standalone: true,
  imports:    [CommonModule],
  template: `
    <span [ngClass]="chipClass"
          class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium">
      <span class="w-1.5 h-1.5 rounded-full" [ngClass]="dotClass"></span>
      {{ statusLabel }}
    </span>
  `,
})
export class StatusChipComponent {
  @Input({ required: true }) status: string = '';

  get chipClass(): string {
    const map: Record<string, string> = {
      ACTIVE:               'bg-green-100 text-green-800',
      INACTIVE:             'bg-gray-100 text-gray-600',
      SUSPENDED:            'bg-red-100 text-red-700',
      PENDING_VERIFICATION: 'bg-amber-100 text-amber-700',
      PUBLISHED:            'bg-green-100 text-green-800',
      DRAFT:                'bg-gray-100 text-gray-600',
      REVIEW:               'bg-blue-100 text-blue-700',
      ARCHIVED:             'bg-gray-100 text-gray-500',
      CONFIRMED:            'bg-green-100 text-green-800',
      PENDING:              'bg-amber-100 text-amber-700',
      WAITLISTED:           'bg-blue-100 text-blue-700',
      CANCELLED:            'bg-red-100 text-red-700',
      COMPLETED:            'bg-purple-100 text-purple-700',
      ONGOING:              'bg-emerald-100 text-emerald-700',
      OFFICIAL:             'bg-green-100 text-green-800',
      UNOFFICIAL:           'bg-gray-100 text-gray-600',
      DISQUALIFIED:         'bg-red-100 text-red-700',
      DNS:                  'bg-gray-100 text-gray-500',
      DNF:                  'bg-orange-100 text-orange-700',
    };
    return map[this.status] ?? 'bg-gray-100 text-gray-600';
  }

  get dotClass(): string {
    const active = ['ACTIVE', 'PUBLISHED', 'CONFIRMED', 'ONGOING', 'OFFICIAL'];
    const warn   = ['PENDING_VERIFICATION', 'PENDING', 'REVIEW', 'WAITLISTED'];
    const error  = ['SUSPENDED', 'CANCELLED', 'DISQUALIFIED', 'DNS', 'DNF'];
    if (active.includes(this.status)) return 'bg-green-500';
    if (warn.includes(this.status))   return 'bg-amber-500';
    if (error.includes(this.status))  return 'bg-red-500';
    return 'bg-gray-400';
  }

  get statusLabel(): string {
    return this.status.replace(/_/g, ' ');
  }
}


// ═══════════════════════════════════════════════════════════════════════════
// RoleBadgeComponent
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Displays a coloured badge for a user role.
 *
 * Usage:
 *   <app-role-badge [role]="user.role" />
 */
@Component({
  selector:   'app-role-badge',
  standalone: true,
  imports:    [CommonModule],
  template: `
    <span [ngClass]="badgeClass"
          class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium">
      {{ roleLabel }}
    </span>
  `,
})
export class RoleBadgeComponent {
  @Input({ required: true }) role: UserRole | string = '';

  get badgeClass(): string {
    const map: Record<string, string> = {
      [UserRole.ADMIN]:            'bg-red-100 text-red-800',
      [UserRole.FEDERATION_STAFF]: 'bg-amber-100 text-amber-800',
      [UserRole.CLUB_MANAGER]:     'bg-blue-100 text-blue-800',
      [UserRole.ATHLETE]:          'bg-green-100 text-green-800',
      [UserRole.PUBLIC]:           'bg-gray-100 text-gray-600',
    };
    return map[this.role] ?? 'bg-gray-100 text-gray-600';
  }

  get roleLabel(): string {
    return String(this.role).replace('ROLE_', '').replace(/_/g, ' ');
  }
}
