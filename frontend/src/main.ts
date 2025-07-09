// src/main.ts

/**
 * Punto di ingresso principale per l'applicazione Angular.
 * <p>
 * Esegue il bootstrap del componente radice {@link AppComponent}
 * utilizzando la configurazione specificata in {@link appConfig}.
 */

import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app';
import { appConfig } from './app/app.config';

/**
 * Avvia l'applicazione Angular applicando la configurazione definita.
 * <p>
 * Se si verifica un errore durante il bootstrap, viene loggato nella console.
 */

bootstrapApplication(AppComponent, appConfig)
  .catch((err) => console.error(err));