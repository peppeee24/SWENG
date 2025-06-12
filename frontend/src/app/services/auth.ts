import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';

export interface User {
  id?: number;
  username: string;
  email?: string;
  nome?: string;
  cognome?: string;
  sesso?: string;
  numeroTelefono?: string;
  citta?: string;
  dataNascita?: string;
  createdAt?: string;
  updatedAt?: string;
  password?: string;
}


export interface RegisterRequest {
  username: string;
  password: string;
  nome?: string;
  cognome?: string;
  email?: string;
  sesso?: 'M' | 'F' | 'ALTRO'; 
  numeroTelefono?: string;
  citta?: string;
  dataNascita?: string;
}

export interface RegistrationResponse {
  success: boolean;
  message: string;
  userId?: number;
  username?: string;
  nome?: string;
  cognome?: string;
  email?: string;
  citta?: string;
  dataNascita?: string;
  createdAt?: string;
  errors?: { [key: string]: string };
}

export interface AvailabilityCheck {
  available: boolean;
  message: string;
  field: string;
  value?: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  token?: string; 
  user: User;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = 'http://localhost:8080/api/auth';
  private http = inject(HttpClient);
  
  currentUser = signal<User | null>(null);
  isAuthenticated = signal<boolean>(false);

  constructor() {
    const userData = localStorage.getItem('currentUser');
    if (userData) {
      try {
        const user = JSON.parse(userData);
        this.currentUser.set(user);
        this.isAuthenticated.set(true);
      } catch (error) {
        console.error('Errore parsing userData:', error);
        localStorage.removeItem('currentUser');
      }
    }
  }

  register(request: RegisterRequest): Observable<RegistrationResponse> {
    console.log('Invio richiesta registrazione:', request);
    
    return this.http.post<RegistrationResponse>(`${this.API_URL}/register`, request)
      .pipe(
        tap(response => {
          console.log('Risposta registrazione:', response);
          if (response.success) {
            const user: User = {
              id: response.userId,
              username: response.username || request.username,
              nome: response.nome,
              cognome: response.cognome,
              email: response.email,
              citta: response.citta,
              dataNascita: response.dataNascita,
              sesso: request.sesso,
              numeroTelefono: request.numeroTelefono
            };
            
            // Salva user info per sessioni future
            this.setUserSession(user);
          }
        }),
        catchError(this.handleError.bind(this))
      );
  }

  checkUsernameAvailability(username: string): Observable<AvailabilityCheck> {
    return this.http.get<AvailabilityCheck>(`${this.API_URL}/check-username?username=${encodeURIComponent(username)}`)
      .pipe(
        catchError(this.handleError.bind(this))
      );
  }

  checkEmailAvailability(email: string): Observable<AvailabilityCheck> {
    return this.http.get<AvailabilityCheck>(`${this.API_URL}/check-email?email=${encodeURIComponent(email)}`)
      .pipe(
        catchError(this.handleError.bind(this))
      );
  }

  login(request: LoginRequest): Observable<AuthResponse>{
    return throwError(() => new Error('Login non ancora implementato.'));
  }

  healthCheck(): Observable<any> {
    return this.http.get(`${this.API_URL}/health`)
      .pipe(
        catchError(this.handleError.bind(this))
      );
  }

  logout(): void {
    localStorage.removeItem('currentUser');
    this.currentUser.set(null);
    this.isAuthenticated.set(false);
    console.log('👋 Logout effettuato');
  }

  isLoggedIn(): boolean {
    return this.isAuthenticated();
  }


  getCurrentUser(): User | null {
    return this.currentUser();
  }


  private setUserSession(user: User): void {
    localStorage.setItem('currentUser', JSON.stringify(user));
    this.currentUser.set(user);
    this.isAuthenticated.set(true);
    console.log('Sessione utente salvata:', user);
  }


  private handleError(error: HttpErrorResponse): Observable<never> {
    let errorMessage = 'Si è verificato un errore sconosciuto';
    
    console.error('Errore HTTP:', error);
    
    if (error.error instanceof ErrorEvent) {
      errorMessage = `Errore: ${error.error.message}`;
    } else {
      if (error.status === 0) {
        errorMessage = 'Impossibile connettersi al server. Verifica che il backend SWENG sia avviato su http://localhost:8080';
      } else if (error.status === 409) {
        if (error.error && error.error.message) {
          errorMessage = error.error.message;
        } else {
          errorMessage = 'Username o email già in uso';
        }
      } else if (error.status === 400) {
        if (error.error && error.error.message) {
          errorMessage = error.error.message;
        } else {
          errorMessage = 'Dati non validi. Controlla i campi inseriti.';
        }
      } else if (error.status === 500) {
        errorMessage = 'Errore interno del server. Riprova più tardi.';
      } else if (error.error && error.error.message) {
        errorMessage = error.error.message;
      } else {
        errorMessage = `Errore del server: ${error.status} - ${error.message}`;
      }
    }
    
    return throwError(() => new Error(errorMessage));
  }
}