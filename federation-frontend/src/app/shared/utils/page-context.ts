import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { Breadcrumb } from '@shared/components/page-header/page-header.component';

/**
 * Feature pages are shared between the public site (/clubs, /competitions…)
 * and the back-office (/admin/clubs…). This helper tells a page where it is
 * rendered so it can pick the right links, breadcrumbs and actions.
 *
 * Must be called in an injection context (field initialiser or constructor).
 */
export function pageContext(section: string, sectionLabel: string) {
  const backOffice = inject(Router).url.startsWith('/admin');
  const base = backOffice ? `/admin/${section}` : `/${section}`;

  return {
    backOffice,
    /** Root list URL of the section in the current context. */
    base,
    crumbs(...tail: Breadcrumb[]): Breadcrumb[] {
      const root: Breadcrumb[] = backOffice
        ? [{ label: 'Admin', path: '/admin' }, { label: sectionLabel, path: base }]
        : [{ label: 'Home', path: '/home' }, { label: sectionLabel, path: base }];
      return tail.length ? [...root, ...tail] : [root[0], { label: sectionLabel }];
    },
  };
}
