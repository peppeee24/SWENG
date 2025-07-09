import { Component, OnInit, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth';

/**
 * Componente DashboardComponent
 * 
 * Mostra la dashboard utente dopo il login. Controlla
 * lo stato di autenticazione e mostra il nome utente.
 * Fornisce funzionalità di logout e navigazione verso le note.
 */

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css']
})
export class DashboardComponent implements OnInit {
  private authService = inject(AuthService);
  private router = inject(Router);

  currentUser = computed(() => this.authService.currentUser());
  isAuthenticated = computed(() => this.authService.isAuthenticated());
  hasToken = computed(() => !!this.authService.getToken());

  displayName = computed(() => {
    const user = this.currentUser();
    if (!user) return 'Utente';
    
    if (user.nome && user.cognome) {
      return `${user.nome} ${user.cognome}`;
    } else if (user.nome) {
      return user.nome;
    } else {
      return user.username;
    }
  });

  /**
   * Controlla autenticazione all’avvio
   * Se non autenticato, reindirizza alla pagina di login
   */
  ngOnInit(): void {
    console.log('Dashboard: Inizializzazione...');
    console.log('isAuthenticated:', this.isAuthenticated());
    console.log('hasToken:', this.hasToken());
    console.log('currentUser:', this.currentUser());

    if (!this.isAuthenticated()) {
      console.log('Dashboard: Utente non autenticato, redirect al login');
      this.router.navigate(['/auth']);
    } else {
      console.log('Dashboard: Utente autenticato, caricamento dashboard');
    }
  }

  /**
   * Logout utente
   * Pulisce lo stato e naviga alla pagina di login
   */
  onLogout(): void {
    console.log('Dashboard: Logout richiesto');
    this.authService.logout();
    this.router.navigate(['/auth']);
  }

  /**
   * Naviga alla pagina delle note
   */
  goToNotes(): void {
    console.log('Dashboard: Navigazione verso le note');
    this.router.navigate(['/notes']);
  }
}