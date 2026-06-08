import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/**
 * Root application shell.
 * Renders the router outlet — all layout and content is
 * managed by layout components activated via the route tree.
 */
@Component({
  selector:    'app-root',
  standalone:  true,
  imports:     [RouterOutlet],
  template:    `<router-outlet />`,
  styles:      [`:host { display: block; height: 100%; }`],
})
export class AppComponent {}
