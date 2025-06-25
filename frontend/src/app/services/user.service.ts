import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { User, UserDisplayName } from '../models/user.model';
import { AuthService } from './auth';
import { environment } from '../environments/environment';

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

  //  metodo per creare gli headers con il token JWT
  private getHeaders(): HttpHeaders {
    const token = this.authService.getToken();
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    });
  }

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

  private createDisplayName(user: User): string {
    if (user.nome && user.cognome) {
      return `${user.nome} ${user.cognome} (${user.username})`;
    } else if (user.nome) {
      return `${user.nome} (${user.username})`;
    }
    return user.username;
  }
}
