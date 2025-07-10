import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { User, UserDisplayName } from '../models/user.model';
import { AuthService } from './auth';
import { environment } from '../environments/environment';

/**
 * Servizio Angular per la gestione degli utenti.
 * Permette di recuperare la lista utenti dal backend,
 * mappando i dati per una visualizzazione più leggibile,
 * e gestisce gli stati di caricamento e errori con segnali reattivi.
 */

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private http = inject(HttpClient);
  private authService = inject(AuthService);
  private apiUrl = `${environment.apiUrl}/users`;

  users = signal<UserDisplayName[]>([]);
  isLoading = signal(false);
  error = signal<string | null>(null);

  /**
   * Crea gli headers HTTP con il token JWT per autorizzazione.
   * @returns HttpHeaders con Content-Type e Authorization Bearer token
   */
  private getHeaders(): HttpHeaders {
    const token = this.authService.getToken();
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    });
  }

  /**
   * Recupera la lista di tutti gli utenti dal backend.
   * Aggiorna il segnale `users` con la lista di utenti mappati a UserDisplayName.
   * Gestisce gli stati di caricamento e di errore.
   * @returns Observable di array User dal backend
   */
  getAllUsers(): Observable<User[]> {
    this.isLoading.set(true);
    this.error.set(null);


    return this.http.get<User[]>(`${this.apiUrl}/list`, {
      headers: this.getHeaders()
    }).pipe(
      tap({
        next: (users) => {
          const userDisplayNames = users.map(user => ({
            id: user.id,
            username: user.username,
            displayName: this.createDisplayName(user)
          }));
          this.users.set(userDisplayNames);
          this.isLoading.set(false);
        },
        error: (error) => {
          this.error.set('Errore nel caricamento degli utenti');
          this.isLoading.set(false);
          console.error('Errore caricamento utenti:', error);
        }
      })
    );
  }

  /**
   * Crea una stringa displayName a partire dai dati dell'utente.
   * - Se sono presenti nome e cognome, concatena "nome cognome (username)"
   * - Se presente solo nome, concatena "nome (username)"
   * - Altrimenti restituisce solo lo username
   * @param user - oggetto User da cui ricavare il displayName
   * @returns stringa da mostrare come nome utente leggibile
   */
  private createDisplayName(user: User): string {
    if (user.nome && user.cognome) {
      return `${user.nome} ${user.cognome} (${user.username})`;
    } else if (user.nome) {
      return `${user.nome} (${user.username})`;
    }
    return user.username;
  }
}
