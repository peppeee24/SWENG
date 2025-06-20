import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { AuthService } from './auth';
import {
  Cartella,
  CreateCartellaRequest,
  UpdateCartellaRequest,
  CartellaResponse,
  CartelleListResponse,
  CartelleStats
} from '../models/cartella.model';

@Injectable({
  providedIn: 'root'
})
export class CartelleService {
  private readonly API_URL = 'http://localhost:8080/api/cartelle';
  private http = inject(HttpClient);
  private authService = inject(AuthService);

  cartelle = signal<Cartella[]>([]);
  isLoading = signal(false);
  error = signal<string | null>(null);
  selectedCartella = signal<Cartella | null>(null);

  private getHeaders(): HttpHeaders {
    const token = this.authService.getToken();
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    });
  }

  createCartella(request: CreateCartellaRequest): Observable<CartellaResponse> {
    this.isLoading.set(true);
    this.error.set(null);

    return this.http.post<CartellaResponse>(this.API_URL, request, {
      headers: this.getHeaders()
    }).pipe(
      tap(response => {
        this.isLoading.set(false);
        if (response.success && response.cartella) {
          const currentCartelle = this.cartelle();
          this.cartelle.set([response.cartella, ...currentCartelle]);
          console.log('Cartella creata con successo:', response.cartella.id);
        }
      }),
      catchError(this.handleError.bind(this))
    );
  }

  getAllCartelle(): Observable<CartelleListResponse> {
    this.isLoading.set(true);
    this.error.set(null);

    return this.http.get<CartelleListResponse>(this.API_URL, {
      headers: this.getHeaders()
    }).pipe(
      tap(response => {
        this.isLoading.set(false);
        if (response.success) {
          this.cartelle.set(response.cartelle);
          console.log(`Recuperate ${response.count} cartelle`);
        }
      }),
      catchError(this.handleError.bind(this))
    );
  }

  getCartellaById(id: number): Observable<CartellaResponse> {
    this.isLoading.set(true);
    this.error.set(null);

    return this.http.get<CartellaResponse>(`${this.API_URL}/${id}`, {
      headers: this.getHeaders()
    }).pipe(
      tap(response => {
        this.isLoading.set(false);
        if (response.success && response.cartella) {
          this.selectedCartella.set(response.cartella);
        }
      }),
      catchError(this.handleError.bind(this))
    );
  }

  updateCartella(id: number, request: UpdateCartellaRequest): Observable<CartellaResponse> {
    this.isLoading.set(true);
    this.error.set(null);

    return this.http.put<CartellaResponse>(`${this.API_URL}/${id}`, request, {
      headers: this.getHeaders()
    }).pipe(
      tap(response => {
        this.isLoading.set(false);
        if (response.success && response.cartella) {
          const currentCartelle = this.cartelle();
          const updatedCartelle = currentCartelle.map(cartella => 
            cartella.id === id ? response.cartella! : cartella
          );
          this.cartelle.set(updatedCartelle);
          this.selectedCartella.set(response.cartella);
          console.log('Cartella aggiornata:', id);
        }
      }),
      catchError(this.handleError.bind(this))
    );
  }

  deleteCartella(id: number): Observable<CartellaResponse> {
    this.isLoading.set(true);
    this.error.set(null);

    return this.http.delete<CartellaResponse>(`${this.API_URL}/${id}`, {
      headers: this.getHeaders()
    }).pipe(
      tap(response => {
        this.isLoading.set(false);
        if (response.success) {
          const currentCartelle = this.cartelle();
          this.cartelle.set(currentCartelle.filter(cartella => cartella.id !== id));
          if (this.selectedCartella()?.id === id) {
            this.selectedCartella.set(null);
          }
          console.log('Cartella eliminata:', id);
        }
      }),
      catchError(this.handleError.bind(this))
    );
  }

  getCartelleStats(): Observable<{success: boolean, stats: CartelleStats}> {
    this.isLoading.set(true);
    this.error.set(null);

    return this.http.get<{success: boolean, stats: CartelleStats}>(`${this.API_URL}/stats`, {
      headers: this.getHeaders()
    }).pipe(
      tap(response => {
        this.isLoading.set(false);
        if (response.success) {
          console.log('Statistiche cartelle caricate:', response.stats);
        }
      }),
      catchError(this.handleError.bind(this))
    );
  }

  clearCartelle(): void {
    this.cartelle.set([]);
    this.selectedCartella.set(null);
    this.error.set(null);
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    this.isLoading.set(false);
    let errorMessage = 'Si è verificato un errore sconosciuto';

    console.error('Errore API Cartelle:', error);

    if (error.error instanceof ErrorEvent) {
      errorMessage = `Errore: ${error.error.message}`;
    } else {
      if (error.status === 0) {
        errorMessage = 'Impossibile connettersi al server. Verifica che il backend sia avviato su http://localhost:8080';
      } else if (error.status === 401) {
        errorMessage = 'Non autorizzato. Effettua nuovamente il login.';
        this.authService.logout();
      } else if (error.status === 403) {
        errorMessage = 'Non hai i permessi per eseguire questa operazione';
      } else if (error.status === 404) {
        errorMessage = 'Cartella non trovata';
      } else if (error.status === 409) {
        if (error.error && error.error.message) {
          errorMessage = error.error.message;
        } else {
          errorMessage = 'Conflitto: la cartella esiste già o contiene note';
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

    this.error.set(errorMessage);
    return throwError(() => new Error(errorMessage));
  }
}