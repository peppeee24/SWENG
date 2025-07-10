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

/**
 * Componente Angular  che gestisce l'elenco di note personali e condivise dell'utente.
 * Fornisce funzionalità di creazione, modifica, cancellazione, duplicazione e ricerca di note,
 * nonché gestione delle versioni, dei permessi di condivisione e dei filtri (per autore, data, tag e cartella).
 *
 * Include inoltre la visualizzazione di statistiche personali, gestione dei parametri URL per filtri automatici,
 * e feedback utente in caso di operazioni di versionamento o aggiornamento.
 */
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

  //  SIGNALS per filtri autore e data
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

  /**
   * Carica tutte le note disponibili per l'utente corrente in base al filtro attivo (all, own, shared).
   * Esegue la richiesta tramite `NotesService`.
   *
   * @returns void
   */
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

  /**
   * Recupera e aggiorna le statistiche personali dell'utente corrente, come numero di note,
   * tag e cartelle disponibili. I dati vengono salvati in una signal.
   *
   * @returns void
   */
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

  /**
   * Carica tutte le cartelle esistenti associate alle note.
   * I dati vengono aggiornati nel servizio `CartelleService`.
   *
   * @returns void
   */
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

  /**
   * Recupera la lista di tutti gli autori disponibili per l'utente corrente
   * da utilizzare come filtro. Aggiorna la relativa signal.
   *
   * @returns void
   */
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

  /**
   * Mostra il form per la creazione di una nuova nota.
   * Reset della nota selezionata e attivazione della UI.
   *
   * @returns void
   */
  showCreateForm(): void {
    this.selectedNote.set(null);
    this.showNoteForm.set(true);
  }

  /**
   * Mostra il form di modifica per una nota esistente.
   * Se la nota ha più di una versione, chiede conferma all'utente.
   *
   * @param note La nota da modificare.
   * @returns void
   */
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

  /**
   * Metodo chiamato al click sulla modifica di una nota.
   * Wrapper di `showEditForm`.
   *
   * @param note La nota selezionata per la modifica.
   * @returns void
   */
  onNoteEdit(note: Note): void {
    this.showEditForm(note);
  }

  /**
   * Nasconde il form di creazione o modifica nota.
   * Reset dello stato relativo.
   *
   * @returns void
   */
  hideNoteForm(): void {
    this.showNoteForm.set(false);
    this.selectedNote.set(null);
  }

  /**
   * Mostra direttamente il form per creare una nuova nota.
   * Alias di `showCreateForm`.
   *
   * @returns void
   */
  showCreateNoteForm(): void {
    this.showCreateForm();
  }

  /**
   * Restituisce il titolo da mostrare nella schermata di stato vuoto,
   * in base al filtro corrente selezionato.
   *
   * @returns string - Titolo da visualizzare.
   */
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

  /**
   * Restituisce il messaggio di stato vuoto da mostrare in base al filtro selezionato.
   *
   * @returns string - Messaggio informativo.
   */
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

  /**
   * Salva una nuova nota o aggiorna una esistente.
   * Se è presente una `selectedNote`, aggiorna la nota, gestendo eventualmente anche i permessi.
   * In caso contrario, crea una nuova nota.
   *
   * @param noteData I dati della nota da creare o aggiornare.
   * @returns void
   */
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


  /**
   * Mostra un messaggio di successo al completamento di un'operazione di aggiornamento o ripristino versione.
   *
   * @param response La risposta del backend contenente la versione aggiornata.
   * @returns void
   */
  private showSuccessMessage(response: any): void {
    const updatedNote = response.data || response.note || response;

    if (updatedNote?.versionNumber) {
      this.versionRestoreSuccess.set(
        `Nota aggiornata con successo! Versione ${updatedNote.versionNumber}`
      );
      setTimeout(() => this.versionRestoreSuccess.set(null), 3000);
    }
  }

  /**
   * Gestisce e mostra un errore verificatosi durante il salvataggio o aggiornamento di una nota.
   *
   * @param error L'oggetto errore ricevuto dal backend.
   * @param operation Il tipo di operazione in cui si è verificato l'errore (nota, permessi, creazione).
   * @returns void
   */
  private handleUpdateError(error: any, operation: string): void {
    console.error(`Errore aggiornamento ${operation}:`, error);
    this.versionRestoreError.set(`Errore durante l'aggiornamento della ${operation}`);
    setTimeout(() => this.versionRestoreError.set(null), 5000);
  }

  /**
   * Esegue operazioni post-salvataggio: chiude il form e ricarica note e statistiche.
   *
   * @returns void
   */
  private finalizeNoteSave(): void {
    this.hideNoteForm();
    this.loadUserStats();
    this.loadNotes();
  }

  /**
   * Elimina una nota dato il suo ID.
   *
   * @param noteId L'ID della nota da eliminare.
   * @returns void
   */
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

  /**
   * Duplica una nota dato il suo ID.
   *
   * @param noteId L'ID della nota da duplicare.
   * @returns void
   */
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

  /**
   * Visualizza una nota in modalità modifica se l'utente ha permesso di modificarla.
   *
   * @param note La nota da visualizzare/modificare.
   * @returns void
   */
  onNoteView(note: Note): void {
    if (note.canEdit) {
      this.onNoteEdit(note);
    }
  }

  /**
   * Rimuove una nota dalla condivisione.
   *
   * @param noteId L'ID della nota da rimuovere dalla condivisione.
   * @returns void
   */
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

  /**
   * Esegue il ripristino di una versione precedente di una nota.
   *
   * @param event Oggetto contenente noteId e versionNumber della versione da ripristinare.
   * @returns void
   */
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

  /**
   * Pulisce i feedback visivi di successo o errore per il versionamento.
   *
   * @returns void
   */
  closeVersionFeedback(): void {
    this.versionRestoreSuccess.set(null);
    this.versionRestoreError.set(null);
  }


  /**
   * Gestisce i parametri URL ricevuti come query string ed esegue automaticamente
   * i filtri richiesti.
   *
   * @returns void
   */
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

  /**
 * Carica le note filtrate per una cartella specifica.
 *
 * @param cartellaNome Il nome della cartella su cui filtrare.
 * @returns void
 */
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

  /**
 * Carica le note filtrate per autore.
 *
 * @param autore Il nome utente o display name dell'autore.
 * @returns void
 */
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

  /**
 * Carica le note filtrate per intervallo di date.
 *
 * @param dataInizio Data di inizio (opzionale)
 * @param dataFine Data di fine (opzionale)
 * @returns void
 */
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
/**
 * Carica le note filtrate per un determinato tag.
 *
 * @param tagNome Il nome del tag da applicare al filtro.
 * @returns void
 */
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

  /**
 * Carica le note che corrispondono alla ricerca testuale.
 *
 * @param searchQuery La stringa di ricerca.
 * @returns void
 */
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

 
  /**
 * Resetta tutti i filtri impostati lato UI e nella query string.
 *
 * @returns void
 */
  private resetFiltersState(): void {
    this.selectedTag.set(null);
    this.selectedCartella.set(null);
    this.searchQuery.set('');
    this.selectedAutore.set('');
    this.selectedDataInizio.set('');
    this.selectedDataFine.set('');
    this.searchForm.patchValue({ query: '' }, { emitEvent: false });
  }

  /**
 * Modifica il filtro attivo (all, own, shared), cancella eventuali filtri attivi
 * e ricarica tutte le note.
 *
 * @param filter Il filtro da applicare.
 * @returns void
 */
  onFilterChange(filter: 'all' | 'own' | 'shared'): void {
    console.log('Cambio filtro a:', filter);
    this.currentFilter.set(filter);

    // Pulisci URL e stati
    this.clearFiltersAndUrl();
    this.loadNotes();
  }

  /**
 * Applica un filtro per tag, aggiornando l'URL con il parametro tag.
 *
 * @param tag Il nome del tag.
 * @returns void
 */
  onTagFilter(tag: string): void {
    console.log('Filtro per tag:', tag);

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { tag: tag },
      replaceUrl: true
    });
  }
  /**
 * Applica un filtro per cartella, aggiornando l'URL con il parametro cartella.
 *
 * @param cartella Il nome della cartella.
 * @returns void
 */
  onCartellaFilter(cartella: string): void {
    console.log('Filtro per cartella:', cartella);

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { cartella: cartella },
      replaceUrl: true
    });
  }

  /**
 * Esegue una ricerca testuale nelle note.
 * Se la query è vuota, resetta i filtri.
 *
 * @returns void
 */
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

  /**
 * Applica un filtro per autore, aggiornando l'URL.
 *
 * @param autore Il nome dell'autore.
 * @returns void
 */
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

  /**
 * Applica un filtro per data di inizio o di fine,
 * aggiornando la query string nell'URL.
 *
 * @param tipo Specifica se è una data di 'inizio' o 'fine'.
 * @param data La data da applicare al filtro.
 * @returns void
 */
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

  /**
 * Rimuove il filtro autore dalla query string.
 *
 * @returns void
 */
  clearAutoreFilter(): void {
    this.onAutoreFilter('');
  }

  /**
 * Rimuove entrambi i filtri di data di inizio e fine dalla query string.
 *
 * @returns void
 */
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

  /**
 * Rimuove tutti i filtri e ricarica le note complete.
 *
 * @returns void
 */
  clearFilters(): void {
    console.log('Pulizia filtri');
    this.clearFiltersAndUrl();
    this.loadNotes();
  }

  /**
 * Cancella i filtri impostati lato UI e pulisce i parametri URL.
 *
 * @returns void
 */
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

  /**
 * Verifica se è attivo almeno uno tra i filtri di tag, cartella, autore,
 * data di inizio, data di fine o ricerca testuale.
 *
 * @returns boolean True se almeno un filtro è attivo, false altrimenti.
 */
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


  /**
 * Verifica se i permessi di una nota sono cambiati rispetto a quelli originali.
 *
 * @param originalNote La nota corrente prima della modifica.
 * @param newPermissions I nuovi permessi proposti.
 * @returns boolean True se i permessi sono cambiati, false altrimenti.
 */
  private hasPermissionsChanged(originalNote: Note, newPermissions: any): boolean {
    if (!newPermissions) return false;

    return originalNote.tipoPermesso !== newPermissions.tipoPermesso ||
      !this.arraysEqual(originalNote.permessiLettura || [], newPermissions.utentiLettura || []) ||
      !this.arraysEqual(originalNote.permessiScrittura || [], newPermissions.utentiScrittura || []);
  }

  /**
 * Confronta due array di stringhe per verificare se contengono gli stessi elementi.
 * L'ordine non è rilevante.
 *
 * @param a Primo array di stringhe.
 * @param b Secondo array di stringhe.
 * @returns boolean True se i due array contengono gli stessi elementi, false altrimenti.
 */
  private arraysEqual(a: string[], b: string[]): boolean {
    if (a.length !== b.length) return false;
    return a.every(val => b.includes(val)) && b.every(val => a.includes(val));
  }

  /**
 * Restituisce la classe CSS da applicare al pulsante di filtro note,
 * in base a quale filtro è attivo.
 *
 * @param filter Il filtro da valutare (all, own, shared).
 * @returns string Classe CSS corrispondente.
 */
  getFilterButtonClass(filter: 'all' | 'own' | 'shared'): string {
    return this.currentFilter() === filter ? 'filter-btn active' : 'filter-btn';
  }

  /**
 * Attiva o disattiva la visualizzazione delle statistiche personali.
 *
 * @returns void
 */
  toggleStats(): void {
    this.showStats.update(show => !show);
  }

  /**
 * Esegue il logout dell'utente corrente.
 * Pulisce la cache locale e reindirizza alla pagina di login.
 *
 * @returns void
 */
  onLogout(): void {
    // Verifica se il metodo esiste prima di chiamarlo
    if (typeof this.notesService.clearCache === 'function') {
      this.notesService.clearCache();
    }
    this.authService.logout();
    this.router.navigate(['/auth']);
  }
}
