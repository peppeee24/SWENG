import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withFetch } from '@angular/common/http';
import { routes } from './app.routes';

/**
 * Configurazione globale dell'applicazione Angular
 * 
 * Qui vengono registrati i principali servizi (providers) e impostazioni che saranno
 * disponibili a tutta l'app standalone.
 */

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }), // Configura il meccanismo di rilevamento dei cambiamenti di Angular
    provideRouter(routes), // Registra il servizio di routing, con le route definite nel file app.routes.ts
    provideHttpClient(withFetch()) // Registra il servizio HttpClient usando Fetch API al posto di XMLHttpRequest
  ]
};