import { Routes } from '@angular/router';
import { AuthComponent } from './components/auth/auth';
import { DashboardComponent } from './components/dashboard/dashboard';
import { NotesComponent } from './components/notes/notes';
import { authGuard } from './guards/auth-guard';

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
    path: '**', 
    redirectTo: '/notes' 
  }
];