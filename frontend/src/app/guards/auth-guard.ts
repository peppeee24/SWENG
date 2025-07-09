import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth';

/**
 * Functional Guard di Angular (Angular 15+ con funzionalità aggiornata in Angular 20)
 * 
 * Questa guardia viene utilizzata per proteggere le rotte che richiedono un utente autenticato.
 * 
 * Se l'utente è loggato → permette la navigazione.
 * Se l'utente NON è loggato → reindirizza alla pagina di login ('/auth') e blocca l'accesso.
 */
export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isLoggedIn()) {
    return true;
  } else {
    router.navigate(['/auth']);
    return false;
  }
};