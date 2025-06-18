import { Component, OnInit, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth';

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

  onLogout(): void {
    console.log('Dashboard: Logout richiesto');
    this.authService.logout();
    this.router.navigate(['/auth']);
  }

  goToNotes(): void {
    console.log('Dashboard: Navigazione verso le note');
    this.router.navigate(['/notes']);
  }
}