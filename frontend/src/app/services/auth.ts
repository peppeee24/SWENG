import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';

// Rappresenta un utente nell'applicazione con proprietà opzionali e obbligatorie
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

// Dati necessari per la registrazione di un nuovo utente
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

// Risposta ricevuta dal backend alla richiesta di registrazione
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

// Risposta per verificare disponibilità di username o email
export interface AvailabilityCheck {
  available: boolean;
  message: string;
  field?: string;
  value?: string;
}

// Dati necessari per effettuare il login (username + password)
export interface LoginRequest {
  username: string;
  password: string;
}

// Risposta ricevuta dal backend alla richiesta di login
export interface LoginResponse {
  success: boolean;
  message: string;
  token?: string;
  user?: User;
  loginTime?: string;
}

// Risposta generica contenente token e dati utente autenticato
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

  /**
   * Invia una richiesta di registrazione utente al backend.
   * @param request Dati necessari per la registrazione (username, password, e altri opzionali)
   * @returns Observable che emette la risposta di registrazione (successo, messaggi, eventuali errori)
   */
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

  /**
   * Esegue il login dell'utente inviando username e password.
   * @param request Contiene username e password dell'utente
   * @returns Observable che emette la risposta di login (token, utente, successo, messaggi)
   */
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

  /**
   * Verifica la disponibilità di uno username.
   * @param username Username da controllare
   * @returns Observable che emette se lo username è disponibile o meno, con messaggio esplicativo
   */
  checkUsernameAvailability(username: string): Observable<AvailabilityCheck> {
    return this.http.get<AvailabilityCheck>(`${this.API_URL}/check-username?username=${encodeURIComponent(username)}`)
      .pipe(
        catchError(this.handleError.bind(this))
      );
  }

  /**
   * Verifica la disponibilità di una email.
   * @param email Email da controllare
   * @returns Observable che emette se la email è disponibile o meno, con messaggio esplicativo
   */
  checkEmailAvailability(email: string): Observable<AvailabilityCheck> {
    return this.http.get<AvailabilityCheck>(`${this.API_URL}/check-email?email=${encodeURIComponent(email)}`)
      .pipe(
        catchError(this.handleError.bind(this))
      );
  }

  /**
   * Effettua un controllo di salute (health check) sul backend di autenticazione.
   * @returns Observable che emette la risposta del controllo di salute del server
   */
  healthCheck(): Observable<any> {
    return this.http.get(`${this.API_URL}/health`)
      .pipe(
        catchError(this.handleError.bind(this))
      );
  }

  /**
   * Effettua il logout dell'utente cancellando la sessione locale.
   * @returns void
   */
  logout(): void {
    this.clearSession();
    console.log('Logout effettuato');
  }

  /**
   * Verifica se l'utente è attualmente autenticato.
   * @returns true se l'utente è autenticato e ha un token valido, false altrimenti
   */
  isLoggedIn(): boolean {
    return this.isAuthenticated() && this.currentToken() !== null;
  }

  /**
   * Restituisce i dati dell'utente attualmente autenticato.
   * @returns Oggetto User o null se non autenticato
   */
  getCurrentUser(): User | null {
    return this.currentUser();
  }

  /**
   * Restituisce il token di autenticazione corrente.
   * @returns Token stringa o null se non autenticato
   */
  getToken(): string | null {
    return this.currentToken();
  }

  /**
   * Imposta la sessione autenticata salvando token e dati utente su localStorage e segnali.
   * @param token Token JWT o altro token di autenticazione
   * @param user Dati utente autenticato
   * @returns void
   */
  private setAuthenticatedSession(token: string, user: User): void {
    localStorage.setItem('authToken', token);
    localStorage.setItem('currentUser', JSON.stringify(user));
    this.currentToken.set(token);
    this.currentUser.set(user);
    this.isAuthenticated.set(true);
    console.log('Sessione autenticata salvata:', { user: user.username, hasToken: !!token });
  }

  /**
   * Imposta la sessione utente senza token (es. dopo registrazione).
   * @param user Dati utente da salvare in sessione locale
   * @returns void
   */
  private setUserSession(user: User): void {
    localStorage.setItem('currentUser', JSON.stringify(user));
    this.currentUser.set(user);
    this.isAuthenticated.set(true);
    console.log('Sessione utente salvata (senza token):', user);
  }

  /**
   * Pulisce la sessione locale rimuovendo dati utente e token.
   * @returns void
   */
  private clearSession(): void {
    localStorage.removeItem('currentUser');
    localStorage.removeItem('authToken');
    this.currentUser.set(null);
    this.currentToken.set(null);
    this.isAuthenticated.set(false);
  }

  /**
   * Gestisce gli errori HTTP intercettati durante le chiamate al backend,
   * costruendo un messaggio di errore leggibile per l'utente.
   * @param error Errore HTTP ricevuto
   * @returns Observable che emette un errore con messaggio leggibile
   */
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