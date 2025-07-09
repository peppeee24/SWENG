import { Routes } from '@angular/router';
import { AuthComponent } from './components/auth/auth';
import { DashboardComponent } from './components/dashboard/dashboard';
import { NotesComponent } from './components/notes/notes';
import { CartelleComponent } from './components/cartelle/cartelle';
import { CartellaNoteComponent } from './components/cartella-notes/cartella-notes'; 
import { authGuard } from './guards/auth-guard';

/**
 * Definizione delle route dell'applicazione.
 * 
 * Ogni oggetto nella lista rappresenta una configurazione di routing:
 * - `path`: la URL relativa da intercettare
 * - `component`: il componente da renderizzare in quella route
 * - `canActivate`: eventuali guardie di accesso per abilitare/disabilitare la navigazione
 */

export const routes: Routes = [
  { 
    path: '', 
    redirectTo: '/notes', 
    pathMatch: 'full' 
  },
  { 
    path: 'auth', 
    component: AuthComponent 
  },
  { 
    path: 'dashboard', 
    component: DashboardComponent, 
    canActivate: [authGuard] 
  },
  { 
    path: 'notes', 
    component: NotesComponent, 
    canActivate: [authGuard] 
  },
  { 
    path: 'cartelle', 
    component: CartelleComponent, 
    canActivate: [authGuard] 
  },
  { 
    path: 'cartelle/:nome/notes', 
    component: CartellaNoteComponent, 
    canActivate: [authGuard] 
  },
  { 
    path: '**', 
    redirectTo: '/notes' 
  }
];