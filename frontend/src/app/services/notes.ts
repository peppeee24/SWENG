import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { AuthService } from './auth';
import {
  Note,
  CreateNoteRequest,
  UpdateNoteRequest,
  NoteResponse,
  NotesListResponse,
  UserStats, PermissionsRequest
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

  private getHeaders(): HttpHeaders {
    const token = this.authService.getToken();
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    });
  }

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

  removeFromSharing(id: number): Observable<any> {
    this.isLoading.set(true);
    this.error.set(null);

    return this.http.delete<any>(`${this.API_URL}/${id}/sharing`, {
      headers: this.getHeaders()
    }).pipe(
      tap(response => {
        this.isLoading.set(false);
        if (response.success) {
          // Rimuove la nota dall'elenco delle note accessibili
          const currentNotes = this.notes();
          this.notes.set(currentNotes.filter(note => note.id !== id));

          // Se è la nota attualmente selezionata, deselezionala
          if (this.selectedNote()?.id === id) {
            this.selectedNote.set(null);
          }

          console.log('Rimosso dalla condivisione della nota:', id);
        }
      }),
      catchError(this.handleError.bind(this))
    );
  }


  updateNotePermissions(id: number, permessi: PermissionsRequest): Observable<NoteResponse> {
    this.isLoading.set(true);
    this.error.set(null);

    return this.http.put<NoteResponse>(`${this.API_URL}/${id}/permissions`, permessi, {
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
          console.log('Permessi nota aggiornati:', id);
        }
      }),
      catchError(this.handleError.bind(this))
    );
  }

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

  clearNotes(): void {
    this.notes.set([]);
    this.selectedNote.set(null);
    this.error.set(null);
  }

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
