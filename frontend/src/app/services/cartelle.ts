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


  /**
   * Costruisce gli headers HTTP con il token di autorizzazione.
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
   * Crea una nuova cartella inviando i dati al backend.
   * @param request - dati della cartella da creare (nome, descrizione, colore)
   * @returns Observable di CartellaResponse con esito e dati cartella creata
   */
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

  /**
   * Recupera tutte le cartelle dell'utente dal backend.
   * @returns Observable di CartelleListResponse contenente lista cartelle e conteggio
   */
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

  /**
   * Recupera i dettagli di una singola cartella tramite il suo ID.
   * @param id - identificativo univoco della cartella
   * @returns Observable di CartellaResponse con i dati della cartella selezionata
   */
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

  /**
   * Aggiorna i dati di una cartella esistente.
   * @param id - ID della cartella da aggiornare
   * @param request - dati aggiornati della cartella (nome, descrizione, colore)
   * @returns Observable di CartellaResponse con esito e dati aggiornati
   */
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

  /**
   * Elimina una cartella tramite il suo ID.
   * @param id - ID della cartella da eliminare
   * @returns Observable di CartellaResponse con esito dell'operazione
   */
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

  /**
   * Recupera le statistiche sulle cartelle (es. numero e nomi).
   * @returns Observable contenente esito e statistiche delle cartelle
   */
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

  /**
   * Pulisce lo stato locale rimuovendo tutte le cartelle e la cartella selezionata.
   * @returns void
   */
  clearCartelle(): void {
    this.cartelle.set([]);
    this.selectedCartella.set(null);
    this.error.set(null);
  }

  /**
   * Gestisce gli errori HTTP provenienti dalle chiamate API.
   * Imposta il messaggio di errore e restituisce un errore Observable.
   * @param error - errore HttpErrorResponse ricevuto
   * @returns Observable che emette un errore con messaggio descrittivo
   */
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