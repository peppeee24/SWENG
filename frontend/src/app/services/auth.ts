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
  field?: string;
  value?: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  success: boolean;
  message: string;
  token?: string;
  user?: User;
  loginTime?: string;
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
  currentToken = signal<string | null>(null);

  constructor() {
    const userData = localStorage.getItem('currentUser');
    const token = localStorage.getItem('authToken');
    
    if (userData && token) {
      try {
        const user = JSON.parse(userData);
        this.currentUser.set(user);
        this.currentToken.set(token);
        this.isAuthenticated.set(true);
        console.log('Sessione utente recuperata:', user);
      } catch (error) {
        console.error('Errore parsing userData:', error);
        this.clearSession();
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
            
            this.setUserSession(user);
          }
        }),
        catchError(this.handleError.bind(this))
      );
  }

  login(request: LoginRequest): Observable<LoginResponse> {
    console.log('Tentativo login per:', request.username);
    
    return this.http.post<LoginResponse>(`${this.API_URL}/login`, request)
      .pipe(
        tap(response => {
          console.log('Risposta login:', response);
          if (response.success && response.token && response.user) {
            this.setAuthenticatedSession(response.token, response.user);
            console.log('Login completato con successo!');
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

  healthCheck(): Observable<any> {
    return this.http.get(`${this.API_URL}/health`)
      .pipe(
        catchError(this.handleError.bind(this))
      );
  }

  logout(): void {
    this.clearSession();
    console.log('Logout effettuato');
  }

  isLoggedIn(): boolean {
    return this.isAuthenticated() && this.currentToken() !== null;
  }

  getCurrentUser(): User | null {
    return this.currentUser();
  }

  getToken(): string | null {
    return this.currentToken();
  }

  private setAuthenticatedSession(token: string, user: User): void {
    localStorage.setItem('authToken', token);
    localStorage.setItem('currentUser', JSON.stringify(user));
    this.currentToken.set(token);
    this.currentUser.set(user);
    this.isAuthenticated.set(true);
    console.log('Sessione autenticata salvata:', { user: user.username, hasToken: !!token });
  }

  private setUserSession(user: User): void {
    localStorage.setItem('currentUser', JSON.stringify(user));
    this.currentUser.set(user);
    this.isAuthenticated.set(true);
    console.log('Sessione utente salvata (senza token):', user);
  }

  private clearSession(): void {
    localStorage.removeItem('currentUser');
    localStorage.removeItem('authToken');
    this.currentUser.set(null);
    this.currentToken.set(null);
    this.isAuthenticated.set(false);
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    let errorMessage = 'Si è verificato un errore sconosciuto';
    
    console.error('Errore HTTP:', error);
    
    if (error.error instanceof ErrorEvent) {
      errorMessage = `Errore: ${error.error.message}`;
    } else {
      if (error.status === 0) {
        errorMessage = 'Impossibile connettersi al server. Verifica che il backend SWENG sia avviato su http://localhost:8080';
      } else if (error.status === 401) {
        errorMessage = 'Username o password non corretti';
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