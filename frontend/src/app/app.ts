import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/**
 * Componente radice dell'applicazione Angular.
 * <p>
 * Utilizza il decoratore {@link Component} per definire:
 * - il selettore del componente (`app-root`)
 * - l'abilitazione della modalità standalone
 * - i moduli importati (in questo caso {@link RouterOutlet})
 * - il template inline
 * - gli stili inline
 */

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: `
    <router-outlet></router-outlet>
  `,
  styles: [`
    :host {
      display: block;
      height: 100vh;
    }
  `]
})
export class AppComponent {
  title = 'nota-bene-frontend';
}