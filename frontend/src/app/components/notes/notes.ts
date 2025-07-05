// notes.component.ts

import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { NotesService } from '../../services/notes';
import { AuthService } from '../../services/auth';
import { CartelleService } from '../../services/cartelle';
import { Note, CreateNoteRequest, UpdateNoteRequest, UpdateNoteRequestWithPermissions, UserStats } from '../../models/note.model';
import { NoteCardComponent } from './note-card/note-card';
import { NoteFormComponent } from './note-form/note-form';

//  Definisco l'interfaccia per i permessi
interface PermissionsRequest {
  tipoPermesso: 'PRIVATA' | 'CONDIVISA_LETTURA' | 'CONDIVISA_SCRITTURA';
  utentiLettura: string[];
  utentiScrittura: string[];
}

@Component({
  selector: 'app-notes',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NoteCardComponent, NoteFormComponent],
  templateUrl: './notes.html',
  styleUrls: ['./notes.css']
})
export class NotesComponent implements OnInit {
  private notesService = inject(NotesService);
  private authService = inject(AuthService);
  private cartelleService = inject(CartelleService);
  private route = inject(ActivatedRoute);
  router = inject(Router);
  private fb = inject(FormBuilder);

  //  SIGNALS PRINCIPALI
  notes = computed(() => this.notesService.notes());
  isLoading = computed(() => this.notesService.isLoading());
  error = computed(() => this.notesService.error());
  currentUser = computed(() => this.authService.currentUser());

  // Signals per UI
  showNoteForm = signal(false);
  selectedNote = signal<Note | null>(null);
  currentFilter = signal<'all' | 'own' | 'shared'>('all');
  searchQuery = signal('');
  selectedTag = signal<string | null>(null);
  selectedCartella = signal<string | null>(null);
  userStats = signal<UserStats | null>(null);
  showStats = signal(false);

  // Signals per feedback versionamento
  versionRestoreLoading = signal(false);
  versionRestoreSuccess = signal<string | null>(null);
  versionRestoreError = signal<string | null>(null);

  // NUOVI SIGNALS per filtri autore e data
  selectedAutore = signal<string>('');
  selectedDataInizio = signal<string>('');
  selectedDataFine = signal<string>('');
  availableAutori = signal<string[]>([]);

  searchForm: FormGroup;

  //  COMPUTED PROPERTIES
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

  currentUsername = computed(() => this.currentUser()?.username || '');

  filteredNotes = computed(() => {
    return this.notes();
  });

  allTags = computed(() => {
    const stats = this.userStats();
    return stats ? stats.allTags : [];
  });

  allCartelle = computed(() => {
    const stats = this.userStats();
    return stats ? stats.allCartelle : [];
  });

  //  CONSTRUCTOR E INIT
  constructor() {
    this.searchForm = this.fb.group({
      query: ['']
    });

    this.searchForm.get('query')?.valueChanges.subscribe(value => {
      this.searchQuery.set(value || '');
    });
  }

  ngOnInit(): void {
    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/auth']);
      return;
    }

    console.log('*************** ngOnInit - availableAutori iniziale:', this.availableAutori());

    // Carica note e statistiche
    this.loadNotes();
    this.loadUserStats();
    this.loadCartelle();
    this.loadAvailableAutori(); // Carica gli autori disponibili


    setTimeout(() => {
      console.log('************* availableAutori dopo 1 secondo:', this.availableAutori());
    }, 1000);

    // Gestisci query parameters per filtri automatici
    this.handleQueryParams();
  }

  // METODI DI CARICAMENTO
  loadNotes(): void {
    this.notesService.getAllNotes(this.currentFilter()).subscribe({
      next: () => {
        console.log('Note caricate per filtro:', this.currentFilter());
      },
      error: (error) => {
        console.error('Errore caricamento note:', error);
      }
    });
  }

  loadUserStats(): void {
    this.notesService.getUserStats().subscribe({
      next: (response: any) => {
        //  Il backend restituisce { success: true, stats: UserStats }
        if (response.success && response.stats) {
          this.userStats.set(response.stats);
        } else {
          // Se la risposta non ha il formato aspettato, prova a usarla direttamente
          this.userStats.set(response);
        }
      },
      error: (error) => {
        console.error('Errore caricamento statistiche:', error);
      }
    });
  }

  private loadCartelle(): void {
    this.cartelleService.getAllCartelle().subscribe({
      next: () => {
        console.log('Cartelle caricate nel componente notes');
      },
      error: (error) => {
        console.error('Errore caricamento cartelle:', error);
      }
    });
  }

  //  Carica gli autori disponibili per il filtro
  loadAvailableAutori(): void {
    console.log('*************** loadAvailableAutori chiamato');
    console.log('***************  availableAutori prima della chiamata:', this.availableAutori());

    this.notesService.getAvailableAutori().subscribe({
      next: (autori: string[]) => {
        console.log('***************  Autori ricevuti dal service:', autori);
        console.log('***************  Tipo di autori:', typeof autori);
        console.log('***************  È array?', Array.isArray(autori));
        console.log('***************  Lunghezza array:', autori.length);

        this.availableAutori.set(autori);

        console.log('***************  availableAutori dopo set:', this.availableAutori());
        console.log('***************  availableAutori().length:', this.availableAutori().length);
      },
      error: (error) => {
        console.error('***************  Errore caricamento autori:', error);
        console.error('***************  Status:', error.status);
        console.error('***************  Message:', error.message);
        console.error('***************  URL chiamata:', error.url);
      }
    });
  }

  // ============= GESTIONE FORM NOTE =============
  showCreateForm(): void {
    this.selectedNote.set(null);
    this.showNoteForm.set(true);
  }

  showEditForm(note: Note): void {
    console.log('Modifica nota richiesta:', note);

    // Verifica se la nota ha versioni multiple e avvisa l'utente
    if (note.versionNumber && note.versionNumber > 1) {
      const proceed = confirm(
        `Questa nota ha ${note.versionNumber} versioni. ` +
        'La modifica creerà una nuova versione. Vuoi continuare?'
      );

      if (!proceed) {
        return;
      }
    }

    this.selectedNote.set(note);
    this.showNoteForm.set(true);
  }

  onNoteEdit(note: Note): void {
    this.showEditForm(note);
  }

  hideNoteForm(): void {
    this.showNoteForm.set(false);
    this.selectedNote.set(null);
  }

  showCreateNoteForm(): void {
    this.showCreateForm();
  }

  getEmptyStateTitle(): string {
    const filter = this.currentFilter();
    switch (filter) {
      case 'own':
        return 'Nessuna nota personale';
      case 'shared':
        return 'Nessuna nota condivisa';
      default:
        return 'Nessuna nota trovata';
    }
  }

  getEmptyStateMessage(): string {
    const filter = this.currentFilter();
    switch (filter) {
      case 'own':
        return 'Non hai ancora creato nessuna nota personale. Inizia creando la tua prima nota!';
      case 'shared':
        return 'Non hai accesso a note condivise al momento.';
      default:
        return 'Non sono state trovate note che corrispondono ai tuoi criteri di ricerca.';
    }
  }

  onNoteSave(noteData: CreateNoteRequest | UpdateNoteRequest | UpdateNoteRequestWithPermissions): void {
    const selectedNote = this.selectedNote();

    if (selectedNote) {
      // Aggiorna nota esistente

      // Controlla se ha permessi (UpdateNoteRequestWithPermissions)
      if ('permessi' in noteData) {
        console.log('Aggiornamento nota CON permessi');
        const updateWithPermissions = noteData as UpdateNoteRequestWithPermissions;

        // Verifica se i permessi sono effettivamente cambiati
        const permissionsChanged = this.hasPermissionsChanged(selectedNote, updateWithPermissions.permessi);

        if (permissionsChanged) {
          console.log(' I permessi sono cambiati, aggiorno tutto');

          // Prima aggiorna il contenuto
          const contentUpdate: UpdateNoteRequest = {
            id: selectedNote.id,
            titolo: updateWithPermissions.titolo,
            contenuto: updateWithPermissions.contenuto,
            tags: updateWithPermissions.tags,
            cartelle: updateWithPermissions.cartelle
          };

          this.notesService.updateNote(selectedNote.id, contentUpdate).subscribe({
            next: (contentResponse: any) => {
              console.log('Contenuto aggiornato:', contentResponse);

              // Poi aggiorna i permessi
              const permissionsRequest: PermissionsRequest = {
                tipoPermesso: updateWithPermissions.permessi.tipoPermesso,
                utentiLettura: updateWithPermissions.permessi.utentiLettura,
                utentiScrittura: updateWithPermissions.permessi.utentiScrittura
              };

              this.notesService.updateNotePermissions(selectedNote.id, permissionsRequest).subscribe({
                next: (permissionsResponse: any) => {
                  console.log('Permessi aggiornati:', permissionsResponse);
                  this.showSuccessMessage(permissionsResponse);
                  this.hideNoteForm();
                  this.loadNotes();
                },
                error: (error) => {
                  this.handleUpdateError(error, 'permessi');
                }
              });
            },
            error: (error) => {
              this.handleUpdateError(error, 'contenuto');
            }
          });
        } else {
          console.log(' Solo contenuto cambiato, aggiorno solo quello');

          // Solo aggiornamento contenuto (senza permessi)
          const updateRequest: UpdateNoteRequest = {
            id: selectedNote.id,
            titolo: updateWithPermissions.titolo,
            contenuto: updateWithPermissions.contenuto,
            tags: updateWithPermissions.tags,
            cartelle: updateWithPermissions.cartelle
          };

          this.notesService.updateNote(selectedNote.id, updateRequest).subscribe({
            next: (response: any) => {
              console.log('Solo contenuto aggiornato:', response);
              this.showSuccessMessage(response);
              this.hideNoteForm();
              this.loadNotes();
            },
            error: (error) => {
              this.handleUpdateError(error, 'nota');
            }
          });
        }

      } else {
        // Aggiornamento normale SENZA permessi
        console.log(' Aggiornamento nota SENZA permessi');
        const updateRequest: UpdateNoteRequest = {
          id: selectedNote.id,
          titolo: (noteData as UpdateNoteRequest).titolo,
          contenuto: (noteData as UpdateNoteRequest).contenuto,
          tags: (noteData as UpdateNoteRequest).tags,
          cartelle: (noteData as UpdateNoteRequest).cartelle
        };

        this.notesService.updateNote(selectedNote.id, updateRequest).subscribe({
          next: (response: any) => {
            console.log('Nota aggiornata (solo contenuto):', response);
            this.showSuccessMessage(response);
            this.hideNoteForm();
            this.loadNotes();
          },
          error: (error) => {
            this.handleUpdateError(error, 'nota');
          }
        });
      }

    } else {
      // Creazione nuova nota (rimane uguale)
      console.log('Creazione nuova nota');
      const createRequest = noteData as CreateNoteRequest;

      this.notesService.createNote(createRequest).subscribe({
        next: (response: any) => {
          console.log('Nota creata:', response);
          this.hideNoteForm();
          this.loadNotes();
        },
        error: (error) => {
          this.handleUpdateError(error, 'creazione');
        }
      });
    }
  }


  private showSuccessMessage(response: any): void {
    const updatedNote = response.data || response.note || response;

    if (updatedNote?.versionNumber) {
      this.versionRestoreSuccess.set(
        `Nota aggiornata con successo! Versione ${updatedNote.versionNumber}`
      );
      setTimeout(() => this.versionRestoreSuccess.set(null), 3000);
    }
  }

  private handleUpdateError(error: any, operation: string): void {
    console.error(`Errore aggiornamento ${operation}:`, error);
    this.versionRestoreError.set(`Errore durante l'aggiornamento della ${operation}`);
    setTimeout(() => this.versionRestoreError.set(null), 5000);
  }

  private finalizeNoteSave(): void {
    this.hideNoteForm();
    this.loadUserStats();
    this.loadNotes();
  }

  onNoteDelete(noteId: number): void {
    this.notesService.deleteNote(noteId).subscribe({
      next: () => {
        this.loadUserStats();
      },
      error: (error) => {
        console.error('Errore eliminazione nota:', error);
      }
    });
  }

  onNoteDuplicate(noteId: number): void {
    this.notesService.duplicateNote(noteId).subscribe({
      next: () => {
        this.loadUserStats();
      },
      error: (error) => {
        console.error('Errore duplicazione nota:', error);
      }
    });
  }

  onNoteView(note: Note): void {
    if (note.canEdit) {
      this.onNoteEdit(note);
    }
  }

  onRemoveFromSharing(noteId: number): void {
    this.notesService.removeFromSharing(noteId).subscribe({
      next: () => {
        console.log('Rimosso dalla condivisione con successo');
      },
      error: (error) => {
        console.error('Errore durante la rimozione dalla condivisione:', error);
        alert(error.error?.message || 'Errore durante la rimozione dalla condivisione');
      }
    });
  }

  // ============= GESTIONE VERSIONAMENTO =============
  onRestoreVersion(event: { noteId: number, versionNumber: number }): void {
    console.log('Ripristino versione richiesto:', event);

    this.versionRestoreLoading.set(true);
    this.versionRestoreError.set(null);
    this.versionRestoreSuccess.set(null);

    this.notesService.restoreNoteVersion(event.noteId, event.versionNumber).subscribe({
      next: (response: any) => {
        console.log('Versione ripristinata con successo:', response);

        //  Accedi ai dati tramite response.data
        const restoredNote = response.data || response.note || response;

        this.versionRestoreSuccess.set(
          `Versione ${event.versionNumber} ripristinata con successo! Creata nuova versione ${restoredNote?.versionNumber || 'N/A'}.`
        );

        this.loadNotes();

        setTimeout(() => {
          this.versionRestoreSuccess.set(null);
        }, 5000);

        this.versionRestoreLoading.set(false);
      },
      error: (error: any) => {
        console.error('Errore ripristino versione:', error);

        let errorMessage = 'Errore durante il ripristino della versione';
        if (error.message) {
          errorMessage = error.message;
        }

        this.versionRestoreError.set(errorMessage);

        setTimeout(() => {
          this.versionRestoreError.set(null);
        }, 5000);

        this.versionRestoreLoading.set(false);
      }
    });
  }

  closeVersionFeedback(): void {
    this.versionRestoreSuccess.set(null);
    this.versionRestoreError.set(null);
  }

  // ============= GESTIONE QUERY PARAMETERS =============
  private handleQueryParams(): void {
    this.route.queryParams.subscribe(params => {
      console.log('Query params ricevuti:', params);

      // Reset dei filtri prima di applicare i nuovi
      this.resetFiltersState();

      // Gestisci cartella con autoFilter
      if (params['cartella'] && params['autoFilter'] === 'true') {
        const cartellaNome = params['cartella'];
        console.log('Filtro automatico per cartella:', cartellaNome);

        this.selectedCartella.set(cartellaNome);
        this.loadNotesByCartella(cartellaNome);

        this.router.navigate([], {
          relativeTo: this.route,
          queryParams: { cartella: cartellaNome },
          replaceUrl: true
        });
      }
      // Gestisci cartella normale
      else if (params['cartella']) {
        const cartellaNome = params['cartella'];
        this.selectedCartella.set(cartellaNome);
        this.loadNotesByCartella(cartellaNome);
      }
      // Gestisci tag
      else if (params['tag']) {
        const tagNome = params['tag'];
        this.selectedTag.set(tagNome);
        this.loadNotesByTag(tagNome);
      }
      // Gestisci search
      else if (params['search']) {
        const searchQuery = params['search'];
        this.searchQuery.set(searchQuery);
        this.searchForm.patchValue({ query: searchQuery }, { emitEvent: false });
        this.loadNotesBySearch(searchQuery);
      }
      // Gestisci filtro autore
      else if (params['autore']) {
        const autore = params['autore'];
        this.selectedAutore.set(autore);
        this.loadNotesByAutore(autore);
      }
      // Gestisci filtri data
      else if (params['dataInizio'] || params['dataFine']) {
        this.selectedDataInizio.set(params['dataInizio'] || '');
        this.selectedDataFine.set(params['dataFine'] || '');
        this.loadNotesByDate(params['dataInizio'], params['dataFine']);
      }
      // Nessun filtro - carica tutte le note
      else {
        this.loadNotes();
      }

      // Aggiorna i signals per i filtri attivi (importante per l'UI)
      this.selectedAutore.set(params['autore'] || '');
      this.selectedDataInizio.set(params['dataInizio'] || '');
      this.selectedDataFine.set(params['dataFine'] || '');
    });
  }

  //  METODI DI CARICAMENTO SEPARATI
  private loadNotesByCartella(cartellaNome: string): void {
    this.notesService.getNotesByCartella(cartellaNome).subscribe({
      next: () => {
        console.log(`Note filtrate per cartella "${cartellaNome}" caricate`);
      },
      error: (error) => {
        console.error('Errore filtro per cartella:', error);
        this.loadNotes(); // Fallback
      }
    });
  }

  // Carica note per autore
  private loadNotesByAutore(autore: string): void {
    this.notesService.getNotesByAutore(autore).subscribe({
      next: () => {
        console.log(`Note filtrate per autore "${autore}" caricate`);
      },
      error: (error) => {
        console.error('Errore filtro per autore:', error);
        this.loadNotes(); // Fallback
      }
    });
  }

  //  Carica note per range di date
  private loadNotesByDate(dataInizio?: string, dataFine?: string): void {
  console.log('COMPONENT: loadNotesByDate chiamato con:', { dataInizio, dataFine, currentFilter: this.currentFilter() });
  
  this.notesService.getNotesByDateRange(dataInizio, dataFine, this.currentFilter()).subscribe({
    next: () => {
      console.log(`COMPONENT: Note filtrate per data (${dataInizio} - ${dataFine}) caricate`);
    },
    error: (error) => {
      console.error('COMPONENT: Errore filtro per data:', error);
      this.loadNotes(); // Fallback
    }
  });
}

  private loadNotesByTag(tagNome: string): void {
    this.notesService.getNotesByTag(tagNome).subscribe({
      next: () => {
        console.log(`Note filtrate per tag "${tagNome}" caricate`);
      },
      error: (error) => {
        console.error('Errore filtro per tag:', error);
        this.loadNotes(); // Fallback
      }
    });
  }

  private loadNotesBySearch(searchQuery: string): void {
    this.notesService.searchNotes(searchQuery).subscribe({
      next: () => {
        console.log(`Note trovate per ricerca "${searchQuery}"`);
      },
      error: (error) => {
        console.error('Errore ricerca:', error);
        this.loadNotes(); // Fallback
      }
    });
  }

  //  Reset di tutti i filtri
  private resetFiltersState(): void {
    this.selectedTag.set(null);
    this.selectedCartella.set(null);
    this.searchQuery.set('');
    this.selectedAutore.set('');
    this.selectedDataInizio.set('');
    this.selectedDataFine.set('');
    this.searchForm.patchValue({ query: '' }, { emitEvent: false });
  }

  // ============= METODI DI FILTRO =============
  onFilterChange(filter: 'all' | 'own' | 'shared'): void {
    console.log('Cambio filtro a:', filter);
    this.currentFilter.set(filter);

    // Pulisci URL e stati
    this.clearFiltersAndUrl();
    this.loadNotes();
  }

  onTagFilter(tag: string): void {
    console.log('Filtro per tag:', tag);

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { tag: tag },
      replaceUrl: true
    });
  }

  onCartellaFilter(cartella: string): void {
    console.log('Filtro per cartella:', cartella);

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { cartella: cartella },
      replaceUrl: true
    });
  }

  onSearch(): void {
    const query = this.searchQuery().trim();
    console.log('Ricerca avviata:', query);

    if (query) {
      this.router.navigate([], {
        relativeTo: this.route,
        queryParams: { search: query },
        replaceUrl: true
      });
    } else {
      this.clearFilters();
    }
  }

  //  Filtro per autore
  onAutoreFilter(autore: string): void {
    console.log('Filtro per autore:', autore);

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        ...this.route.snapshot.queryParams,
        autore: autore || undefined
      },
      replaceUrl: true
    });
  }

  //  Filtro per date
  onDataFilter(tipo: 'inizio' | 'fine', data: string): void {
    console.log(`Filtro per data ${tipo}:`, data);

    const queryParams: any = { ...this.route.snapshot.queryParams };

    if (tipo === 'inizio') {
      if (data) queryParams.dataInizio = data;
      else delete queryParams.dataInizio;
    } else {
      if (data) queryParams.dataFine = data;
      else delete queryParams.dataFine;
    }

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: queryParams,
      replaceUrl: true
    });
  }

  //  Pulisci filtro autore
  clearAutoreFilter(): void {
    this.onAutoreFilter('');
  }

  //  Pulisci filtri data
  clearDateFilters(): void {
    const queryParams: any = { ...this.route.snapshot.queryParams };
    delete queryParams['dataInizio'];
    delete queryParams['dataFine'];

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: queryParams,
      replaceUrl: true
    });
  }

  clearFilters(): void {
    console.log('Pulizia filtri');
    this.clearFiltersAndUrl();
    this.loadNotes();
  }

  private clearFiltersAndUrl(): void {
    // Reset stato
    this.resetFiltersState();

    // Pulisci URL
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {},
      replaceUrl: true
    });
  }

  // AGGIORNATO: Verifica se ci sono filtri attivi
  hasActiveFilters(): boolean {
    return !!(
      this.selectedTag() ||
      this.selectedCartella() ||
      this.searchQuery() ||
      this.selectedAutore() ||
      this.selectedDataInizio() ||
      this.selectedDataFine()
    );
  }

  // ============= METODI UTILITY =============
  private hasPermissionsChanged(originalNote: Note, newPermissions: any): boolean {
    if (!newPermissions) return false;

    return originalNote.tipoPermesso !== newPermissions.tipoPermesso ||
      !this.arraysEqual(originalNote.permessiLettura || [], newPermissions.utentiLettura || []) ||
      !this.arraysEqual(originalNote.permessiScrittura || [], newPermissions.utentiScrittura || []);
  }

  private arraysEqual(a: string[], b: string[]): boolean {
    if (a.length !== b.length) return false;
    return a.every(val => b.includes(val)) && b.every(val => a.includes(val));
  }

  getFilterButtonClass(filter: 'all' | 'own' | 'shared'): string {
    return this.currentFilter() === filter ? 'filter-btn active' : 'filter-btn';
  }

  toggleStats(): void {
    this.showStats.update(show => !show);
  }

  onLogout(): void {
    // Verifica se il metodo esiste prima di chiamarlo
    if (typeof this.notesService.clearCache === 'function') {
      this.notesService.clearCache();
    }
    this.authService.logout();
    this.router.navigate(['/auth']);
  }
}
