import { Injectable, inject, signal } from '@angular/core';
import {HttpClient, HttpHeaders, HttpErrorResponse, HttpParams} from '@angular/common/http';
import { Observable, throwError, of } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { AuthService } from './auth';
import {
  Note,
  CreateNoteRequest,
  UpdateNoteRequest,
  NoteResponse,
  NotesListResponse,
  UserStats,
  PermissionsRequest
} from '../models/note.model';

@Injectable({
  providedIn: 'root'
})
export class NotesService {
  private readonly API_URL = 'http://localhost:8080/api/notes';
  private http = inject(HttpClient);
  private authService = inject(AuthService);

  notes = signal<Note[]>([]);
  isLoading = signal(false);
  error = signal<string | null>(null);
  selectedNote = signal<Note | null>(null);

  /**
   * Restituisce gli headers HTTP con il token di autorizzazione.
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
   * Crea una nuova nota inviando i dati al backend.
   * @param request - dati della nuova nota (titolo, contenuto, etc.)
   * @returns Observable di NoteResponse con esito e dati della nota creata
   */
  createNote(request: CreateNoteRequest): Observable<NoteResponse> {
    this.isLoading.set(true);
    this.error.set(null);

    return this.http.post<NoteResponse>(this.API_URL, request, {
      headers: this.getHeaders()
    }).pipe(
      tap(response => {
        this.isLoading.set(false);
        if (response.success && response.note) {
          const currentNotes = this.notes();
          this.notes.set([response.note, ...currentNotes]);
          console.log('Nota creata con successo:', response.note.id);
        }
      }),
      catchError(this.handleError.bind(this))
    );
  }

  /**
   * Recupera tutte le note, eventualmente filtrate (tutte, proprie, condivise).
   * @param filter - filtro da applicare: 'all', 'own', 'shared'
   * @returns Observable di NotesListResponse contenente lista note e conteggio
   */
  getAllNotes(filter: 'all' | 'own' | 'shared' = 'all'): Observable<NotesListResponse> {
    this.isLoading.set(true);
    this.error.set(null);

    const url = `${this.API_URL}?filter=${filter}`;

    return this.http.get<NotesListResponse>(url, {
      headers: this.getHeaders()
    }).pipe(
      tap(response => {
        this.isLoading.set(false);
        if (response.success) {
          this.notes.set(response.notes);
          console.log(`Recuperate ${response.count} note con filtro: ${filter}`);
        }
      }),
      catchError(this.handleError.bind(this))
    );
  }

  /**
   * Rimuove la condivisione di una nota specifica.
   * @param id - ID della nota da rimuovere dalla condivisione
   * @returns Observable di risposta generica con esito dell'operazione
   */
  removeFromSharing(id: number): Observable<any> {
    this.isLoading.set(true);
    this.error.set(null);

    return this.http.delete<any>(`${this.API_URL}/${id}/sharing`, {
      headers: this.getHeaders()
    }).pipe(
      tap(response => {
        this.isLoading.set(false);
        if (response.success) {
          const currentNotes = this.notes();
          this.notes.set(currentNotes.filter(note => note.id !== id));

          if (this.selectedNote()?.id === id) {
            this.selectedNote.set(null);
          }

          console.log('Rimosso dalla condivisione della nota:', id);
        }
      }),
      catchError(this.handleError.bind(this))
    );
  }

  /**
   * Aggiorna i permessi di una nota (lettura, scrittura, tipo permesso).
   * @param id - ID della nota di cui modificare i permessi
   * @param permessi - oggetto con i permessi da aggiornare
   * @returns Observable di NoteResponse con esito e dati aggiornati
   */

  updateNotePermissions(id: number, permessi: PermissionsRequest): Observable<NoteResponse> {
    this.isLoading.set(true);
    this.error.set(null);

    console.log('Aggiornamento permessi per nota:', id);
    console.log('Nuovi permessi:', permessi);

    return this.http.put<NoteResponse>(`${this.API_URL}/${id}/permissions`, permessi, {
      headers: this.getHeaders()
    }).pipe(
      tap(response => {
        this.isLoading.set(false);
        console.log('Risposta aggiornamento permessi:', response);

        if (response.success && (response.note || response.data)) {
          const updatedNote = response.note || response.data;

          console.log('Nota aggiornata ricevuta:', updatedNote);
          console.log('Nuovi permessi dal server:', {
            tipo: updatedNote?.tipoPermesso,
            lettura: updatedNote?.permessiLettura,
            scrittura: updatedNote?.permessiScrittura
          });

          const currentNotes = this.notes();
          const updatedNotes = currentNotes.map(note =>
            note.id === id ? { ...note, ...updatedNote } : note
          );
          this.notes.set(updatedNotes);

          const selectedNote = this.selectedNote();
          if (selectedNote && selectedNote.id === id) {
            this.selectedNote.set({ ...selectedNote, ...updatedNote });
          }

          console.log('Cache locale aggiornata per nota:', id);
        } else {
          console.error('Risposta non valida dal server:', response);
        }
      }),
      catchError(error => {
        console.error('Errore aggiornamento permessi:', error);
        return this.handleError(error);
      })
    );
  }

  /**
   * Aggiorna manualmente la singola nota con dati aggiornati dal backend.
   * @param noteId - ID della nota da aggiornare in cache locale
   * @returns void
   */
  refreshNote(noteId: number): void {
    console.log('Refresh manuale della nota:', noteId);

    this.http.get<NoteResponse>(`${this.API_URL}/${noteId}`, {
      headers: this.getHeaders()
    }).subscribe({
      next: (response) => {
        if (response.success && (response.note || response.data)) {
          const refreshedNote = response.note || response.data;

          const currentNotes = this.notes();
          const updatedNotes = currentNotes.map(note =>
            note.id === noteId ? { ...note, ...refreshedNote } : note
          );
          this.notes.set(updatedNotes);

          console.log('Nota refreshed:', refreshedNote);
        }
      },
      error: (error) => {
        console.error('Errore refresh nota:', error);
      }
    });
  }

  // SISTEMA DI LOCK
  /**
   * Richiede il blocco (lock) di una nota per impedirne modifiche concorrenti.
   * @param noteId - ID della nota da bloccare
   * @returns Observable di risposta generica
   */
  lockNote(noteId: number): Observable<any> {
    console.log('Chiamata API lockNote per nota:', noteId);
    return this.http.post(`${this.API_URL}/${noteId}/lock`, {}, {
      headers: this.getHeaders()
    }).pipe(
      tap(response => {
        console.log('Lock acquisito per nota:', noteId, response);
      }),
      catchError(this.handleError.bind(this))
    );
  }

  /**
   * Verifica lo stato del blocco su una nota.
   * @param noteId - ID della nota di cui verificare il lock
   * @returns Observable di risposta generica contenente lo stato del lock
   */
  getLockStatus(noteId: number): Observable<any> {
    console.log('Chiamata API getLockStatus per nota:', noteId);

    return this.http.get(`${this.API_URL}/${noteId}/lock-status`, {
      headers: this.getHeaders()
    }).pipe(
      tap(response => {
        console.log('Stato lock nota:', noteId, response);
      }),
      catchError(this.handleError.bind(this))
    );
  }

   /**
   * Rimuove il blocco su una nota.
   * @param noteId - ID della nota da sbloccare
   * @returns Observable di risposta generica
   */
  unlockNote(noteId: number): Observable<any> {
    console.log('Chiamata API unlockNote per nota:', noteId);

    return this.http.delete(`${this.API_URL}/${noteId}/lock`, {
      headers: this.getHeaders()
    }).pipe(
      tap(response => {
        console.log('Lock rilasciato per nota:', noteId, response);
      }),
      catchError(this.handleError.bind(this))
    );
  }

  /**
   * Rinnova il blocco su una nota per estendere il tempo di lock.
   * @param noteId - ID della nota di cui rinnovare il lock
   * @returns Observable di risposta generica
   */

  refreshLock(noteId: number): Observable<any> {
    console.log('Chiamata API refreshLock per nota:', noteId);

    return this.http.put(`${this.API_URL}/${noteId}/lock/refresh`, {}, {
      headers: this.getHeaders()
    }).pipe(
      tap(response => {
        console.log('Lock rinnovato per nota:', noteId, response);
      }),
      catchError(this.handleError.bind(this))
    );
  }

  /**
   * Recupera i dettagli di una singola nota tramite ID.
   * @param id - ID della nota da recuperare
   * @returns Observable di NoteResponse con dati della nota selezionata
   */
  getNoteById(id: number): Observable<NoteResponse> {
    this.isLoading.set(true);
    this.error.set(null);

    return this.http.get<NoteResponse>(`${this.API_URL}/${id}`, {
      headers: this.getHeaders()
    }).pipe(
      tap(response => {
        this.isLoading.set(false);
        if (response.success && response.note) {
          this.selectedNote.set(response.note);
        }
      }),
      catchError(this.handleError.bind(this))
    );
  }

  /**
   * Aggiorna una nota esistente con nuovi dati.
   * @param id - ID della nota da aggiornare
   * @param request - dati aggiornati della nota
   * @returns Observable di NoteResponse con dati aggiornati
   */
  updateNote(id: number, request: UpdateNoteRequest): Observable<NoteResponse> {
    this.isLoading.set(true);
    this.error.set(null);

    return this.http.put<NoteResponse>(`${this.API_URL}/${id}`, request, {
      headers: this.getHeaders()
    }).pipe(
      tap(response => {
        this.isLoading.set(false);
        if (response.success && response.note) {
          const currentNotes = this.notes();
          const updatedNotes = currentNotes.map(note =>
            note.id === id ? response.note! : note
          );
          this.notes.set(updatedNotes);
          this.selectedNote.set(response.note);
          console.log('Nota aggiornata:', id);
        }
      }),
      catchError(this.handleError.bind(this))
    );
  }

  /**
   * Elimina una nota tramite ID.
   * @param id - ID della nota da eliminare
   * @returns Observable di NoteResponse con esito eliminazione
   */
  deleteNote(id: number): Observable<NoteResponse> {
    this.isLoading.set(true);
    this.error.set(null);

    return this.http.delete<NoteResponse>(`${this.API_URL}/${id}`, {
      headers: this.getHeaders()
    }).pipe(
      tap(response => {
        this.isLoading.set(false);
        if (response.success) {
          const currentNotes = this.notes();
          this.notes.set(currentNotes.filter(note => note.id !== id));
          if (this.selectedNote()?.id === id) {
            this.selectedNote.set(null);
          }
          console.log('Nota eliminata:', id);
        }
      }),
      catchError(this.handleError.bind(this))
    );
  }

  /**
   * Duplica una nota specifica tramite ID.
   * @param id - ID della nota da duplicare
   * @returns Observable di NoteResponse con la nuova nota creata
   */
  duplicateNote(id: number): Observable<NoteResponse> {
    console.log('NotesService: Duplicazione nota ID:', id);
    console.log('URL chiamata:', `${this.API_URL}/${id}/duplicate`);
    console.log('Headers:', this.getHeaders());

    this.isLoading.set(true);
    this.error.set(null);

    return this.http.post<NoteResponse>(`${this.API_URL}/${id}/duplicate`, {}, {
      headers: this.getHeaders()
    }).pipe(
      tap(response => {
        console.log('Risposta backend duplicazione:', response);
        this.isLoading.set(false);
        if (response.success && response.note) {
          const currentNotes = this.notes();
          this.notes.set([response.note, ...currentNotes]);
          console.log('Nota duplicata aggiunta alla lista:', response.note.id);
        }
      }),
      catchError(error => {
        console.error('Errore dettagliato duplicazione:', {
          status: error.status,
          statusText: error.statusText,
          error: error.error,
          url: error.url,
          noteId: id
        });
        return this.handleError(error);
      })
    );
  }

   /**
   * Cerca note tramite parola chiave.
   * @param keyword - stringa di ricerca
   * @returns Observable di NotesListResponse con lista note corrispondenti
   */
  searchNotes(keyword: string): Observable<NotesListResponse> {
    this.isLoading.set(true);
    this.error.set(null);

    return this.http.get<NotesListResponse>(`${this.API_URL}/search?q=${encodeURIComponent(keyword)}`, {
      headers: this.getHeaders()
    }).pipe(
      tap(response => {
        this.isLoading.set(false);
        if (response.success) {
          this.notes.set(response.notes);
          console.log(`Trovate ${response.count} note per: ${keyword}`);
        }
      }),
      catchError(this.handleError.bind(this))
    );
  }

  /**
   * Recupera note filtrate per tag specifico.
   * @param tag - tag da usare come filtro
   * @returns Observable di NotesListResponse con note filtrate
   */
  getNotesByTag(tag: string): Observable<NotesListResponse> {
    this.isLoading.set(true);
    this.error.set(null);

    return this.http.get<NotesListResponse>(`${this.API_URL}/filter/tag/${encodeURIComponent(tag)}`, {
      headers: this.getHeaders()
    }).pipe(
      tap(response => {
        this.isLoading.set(false);
        if (response.success) {
          this.notes.set(response.notes);
          console.log(`Recuperate ${response.count} note per tag: ${tag}`);
        }
      }),
      catchError(this.handleError.bind(this))
    );
  }

  /**
   * Recupera note filtrate per cartella specifica.
   * @param cartella - nome della cartella da filtrare
   * @returns Observable di NotesListResponse con note filtrate
   */
  getNotesByCartella(cartella: string): Observable<NotesListResponse> {
    this.isLoading.set(true);
    this.error.set(null);

    return this.http.get<NotesListResponse>(`${this.API_URL}/filter/cartella/${encodeURIComponent(cartella)}`, {
      headers: this.getHeaders()
    }).pipe(
      tap(response => {
        this.isLoading.set(false);
        if (response.success) {
          this.notes.set(response.notes);
          console.log(`Recuperate ${response.count} note per cartella: ${cartella}`);
        }
      }),
      catchError(this.handleError.bind(this))
    );
  }

  /**
   * Recupera statistiche relative all'utente e le sue note.
   * @returns Observable contenente esito e statistiche utente
   */
  getUserStats(): Observable<{success: boolean, stats: UserStats}> {
    this.isLoading.set(true);
    this.error.set(null);

    return this.http.get<{success: boolean, stats: UserStats}>(`${this.API_URL}/stats`, {
      headers: this.getHeaders()
    }).pipe(
      tap(response => {
        this.isLoading.set(false);
        if (response.success) {
          console.log('Statistiche caricate:', response.stats);
        }
      }),
      catchError(this.handleError.bind(this))
    );
  }

   /**
   * Recupera la cronologia delle versioni di una nota.
   * @param noteId - ID della nota di cui ottenere la cronologia versioni
   * @returns Observable con array di versioni o array vuoto in caso di errori 404
   */
  getNoteVersionHistory(noteId: number): Observable<any> {
    console.log('Service: richiesta cronologia versioni per nota:', noteId);

    if (!noteId || noteId <= 0) {
      console.error('Service: noteId non valido:', noteId);
      return of([]);
    }

    return this.http.get(`${this.API_URL}/${noteId}/versions`, {
      headers: this.getHeaders()
    }).pipe(
      tap(response => {
        console.log('Service: risposta cronologia ricevuta:', response);
        console.log('Service: tipo risposta:', typeof response);
        console.log('Service: è array:', Array.isArray(response));
      }),
      catchError(error => {
        console.error('Service: errore cronologia versioni:', error);
        console.error('Service: status code:', error.status);

        // Se l'endpoint non esiste o la nota non ha versioni, restituisci array vuoto
        if (error.status === 404) {
          console.log('Service: nessuna versione trovata, restituisco array vuoto');
          return of([]);
        }

        // Per altri errori, propagali
        return this.handleError(error);
      })
    );
  }

  /**
   * Ripristina una versione precedente di una nota.
   * @param noteId - ID della nota
   * @param versionNumber - numero della versione da ripristinare
   * @returns Observable con risultato del ripristino
   */
  restoreNoteVersion(noteId: number, versionNumber: number): Observable<any> {
    console.log('Service: ripristino versione', versionNumber, 'per nota:', noteId);

    if (!noteId || noteId <= 0 || !versionNumber || versionNumber <= 0) {
      console.error('Service: parametri non validi:', { noteId, versionNumber });
      return throwError(() => new Error('Parametri non validi per il ripristino'));
    }

    return this.http.post(`${this.API_URL}/${noteId}/restore`,
      { versionNumber },
      { headers: this.getHeaders() }
    ).pipe(
      tap((response: any) => {
        console.log('Service: versione ripristinata:', response);

        // Aggiorna la lista delle note dopo il ripristino
        if (response && (response.data || response.note)) {
          const restoredNote = response.data || response.note;
          const currentNotes = this.notes();
          const updatedNotes = currentNotes.map(note =>
            note.id === noteId ? { ...note, ...restoredNote } : note
          );
          this.notes.set(updatedNotes);
          console.log('Service: note aggiornate dopo ripristino');
        }
      }),
      catchError(error => {
        console.error('Service: errore ripristino versione:', error);
        return this.handleError(error);
      })
    );
  }

  //  METODI PER FILTRI AUTORE E DATA

  /**
   * Recupera le note filtrate per autore.
   * @param autore - nome autore da filtrare
   * @returns Observable di NotesListResponse con note filtrate
   */
  getNotesByAutore(autore: string): Observable<NotesListResponse> {
    this.isLoading.set(true);
    this.error.set(null);

    const params = new HttpParams().set('autore', autore);

    return this.http.get<NotesListResponse>(`${this.API_URL}`, {
      params,
      headers: this.getHeaders()
    }).pipe(
      tap(response => {
        this.isLoading.set(false);
        if (response.success && response.notes) {
          this.notes.set(response.notes);
          console.log(`Note filtrate per autore "${autore}":`, response.notes.length);
        }
      }),
      catchError(this.handleError.bind(this))
    );
  }

  /**
   * Recupera le note filtrate per intervallo di date e filtro.
   * @param dataInizio - data di inizio (formato stringa)
   * @param dataFine - data di fine (formato stringa)
   * @param filter - filtro applicato (default 'all')
   * @returns Observable di NotesListResponse con note filtrate
   */
  getNotesByDateRange(dataInizio?: string, dataFine?: string, filter: string = 'all'): Observable<NotesListResponse> {
  console.log('SERVICE: getNotesByDateRange chiamato con:', { dataInizio, dataFine, filter });

  this.isLoading.set(true);
  this.error.set(null);

  let params = new HttpParams();

  if (dataInizio) {
    params = params.set('dataInizio', dataInizio);
  }
  if (dataFine) {
    params = params.set('dataFine', dataFine);
  }
  params = params.set('filter', filter);

  console.log('SERVICE: Parametri finali:', params.toString());
  console.log('SERVICE: URL completa:', `${this.API_URL}?${params.toString()}`);

  return this.http.get<NotesListResponse>(`${this.API_URL}`, {
    params,
    headers: this.getHeaders()
  }).pipe(
    tap(response => {
      this.isLoading.set(false);
      if (response.success && response.notes) {
        this.notes.set(response.notes);
        console.log(`SERVICE: Note filtrate per date (${dataInizio} - ${dataFine}) con filtro ${filter}:`, response.notes.length);
        console.log('SERVICE: Note ricevute:', response.notes.map(n => ({ id: n.id, titolo: n.titolo, dataCreazione: n.dataCreazione })));
      }
    }),
    catchError(this.handleError.bind(this))
  );
}

  /**
   * Recupera la lista degli autori disponibili per il filtro.
   * @returns Observable di array stringhe con autori disponibili
   */

  getAvailableAutori(): Observable<string[]> {
    console.log('////////// Service: getAvailableAutori chiamato');
    console.log('////////// Service: URL:', `${this.API_URL}/autori`);

    return this.http.get<string[]>(`${this.API_URL}/autori`, {
      headers: this.getHeaders()
    }).pipe(
      tap(response => {
        console.log('///////// Service: Autori ricevuti:', response);
        console.log('///////// Service: Lunghezza array:', response.length);
      }),
      catchError(error => {
        console.error('//////// Service: Errore recupero autori:', error);
        console.error('//////// Service: Status:', error.status);
        console.error('//////// Service: Message:', error.message);

        // Restituisci array vuoto in caso di errore
        return of([]);
      })
    );
  }

  /**
   * Metodo di ricerca avanzata con supporto a tutti i filtri possibili.
   * @param filters - oggetto contenente i filtri (search, tag, cartella, autore, dataInizio, dataFine)
   * @returns Observable di NotesListResponse con risultati filtrati
   */
  searchNotesAdvanced(filters: {
    search?: string;
    tag?: string;
    cartella?: string;
    autore?: string;
    dataInizio?: string;
    dataFine?: string;
  }): Observable<NotesListResponse> {
    this.isLoading.set(true);
    this.error.set(null);

    let params = new HttpParams();

    if (filters.search) params = params.set('search', filters.search);
    if (filters.tag) params = params.set('tag', filters.tag);
    if (filters.cartella) params = params.set('cartella', filters.cartella);
    if (filters.autore) params = params.set('autore', filters.autore);
    if (filters.dataInizio) params = params.set('dataInizio', filters.dataInizio);
    if (filters.dataFine) params = params.set('dataFine', filters.dataFine);

    return this.http.get<NotesListResponse>(`${this.API_URL}`, {
      params,
      headers: this.getHeaders()
    }).pipe(
      tap(response => {
        this.isLoading.set(false);
        if (response.success && response.notes) {
          this.notes.set(response.notes);
          console.log('Ricerca avanzata completata:', response.notes.length, 'note trovate');
        }
      }),
      catchError(this.handleError.bind(this))
    );
  }

  // METODI UTILITY

  /**
   * Pulisce eventuale cache (se implementata).
   */
  clearCache(): void {
    console.log('Cache cleared');
  }

  /**
   * Pulisce eventuale cache (se implementata).
   */
  clearNotes(): void {
    this.notes.set([]);
    this.selectedNote.set(null);
    this.error.set(null);
  }

  /**
   * Gestione centralizzata degli errori HTTP.
   * @param error - HttpErrorResponse ricevuto dall'API
   * @returns Observable che emette errore con messaggio significativo
   */
  private handleError(error: HttpErrorResponse): Observable<never> {
    this.isLoading.set(false);
    let errorMessage = 'Si è verificato un errore sconosciuto';

    console.error('Errore API Note:', error);

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
        errorMessage = 'Nota non trovata';
      } else if (error.status === 409) {
        // Conflitto - probabilmente nota bloccata
        if (error.error && error.error.message) {
          errorMessage = error.error.message;
        } else {
          errorMessage = 'La nota è già in modifica da un altro utente';
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
