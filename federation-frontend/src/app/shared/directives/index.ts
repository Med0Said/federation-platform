import {
  Directive, Input, TemplateRef, ViewContainerRef, OnInit, OnDestroy, effect
} from '@angular/core';
import { AuthService } from '@core/services/auth.service';
import { UserRole }    from '@core/models';

/**
 * Structural directive — conditionally renders content based on user role.
 *
 * Usage:
 *   <button *appHasRole="'ROLE_ADMIN'">Delete</button>
 *   <div *appHasRole="[UserRole.ADMIN, UserRole.FEDERATION_STAFF]">Admin tools</div>
 *
 * The directive re-evaluates reactively when the user signal changes (login/logout).
 */
@Directive({
  selector:   '[appHasRole]',
  standalone: true,
})
export class HasRoleDirective implements OnInit {

  @Input('appHasRole') set roles(value: UserRole | UserRole[] | string | string[]) {
    this.requiredRoles = Array.isArray(value) ? value as UserRole[] : [value as UserRole];
    this.updateView();
  }

  private requiredRoles: UserRole[] = [];

  constructor(
    private templateRef:    TemplateRef<any>,
    private viewContainer:  ViewContainerRef,
    private auth:           AuthService
  ) {
    // React to auth state changes (e.g. logout clears role)
    effect(() => {
      // Access the signal so this effect re-runs on change
      void this.auth.currentUser();
      this.updateView();
    });
  }

  ngOnInit(): void {
    this.updateView();
  }

  private updateView(): void {
    const hasAccess = this.auth.hasAnyRole(this.requiredRoles);
    this.viewContainer.clear();
    if (hasAccess) {
      this.viewContainer.createEmbeddedView(this.templateRef);
    }
  }
}


// ═══════════════════════════════════════════════════════════════════════════
// ClickOutsideDirective — emit event when clicking outside the host element
// ═══════════════════════════════════════════════════════════════════════════

import { Directive, Output, EventEmitter, HostListener } from '@angular/core';

@Directive({
  selector:   '[appClickOutside]',
  standalone: true,
})
export class ClickOutsideDirective {
  @Output() appClickOutside = new EventEmitter<void>();

  private clicked = false;

  @HostListener('click')
  onHostClick(): void {
    this.clicked = true;
  }

  @HostListener('document:click')
  onDocumentClick(): void {
    if (!this.clicked) {
      this.appClickOutside.emit();
    }
    this.clicked = false;
  }
}
