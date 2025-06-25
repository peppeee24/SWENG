// notes.component.ts - Versione corretta completa

import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { NotesService } from '../../services/notes';
import { AuthService } from '../../services/auth';
import { CartelleService } from '../../services/cartelle';
import { Note, CreateNoteRequest, UpdateNoteRequest, UserStats } from '../../models/note.model';
import { NoteCardComponent } from './note-card/note-card';
import { NoteFormComponent } from './note-form/note-form';

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

  notes = computed(() => this.notesService.notes());
  isLoading = computed(() => this.notesService.isLoading());
  error = computed(() => this.notesService.error());
  currentUser = computed(() => this.authService.currentUser());

  showNoteForm = signal(false);
  selectedNote = signal<Note | null>(null);
  currentFilter = signal<'all' | 'own' | 'shared'>('all');
  searchQuery = signal('');
  selectedTag = signal<string | null>(null);
  selectedCartella = signal<string | null>(null);
  userStats = signal<UserStats | null>(null);
  showStats = signal(false);

  searchForm: FormGroup;

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

    // Carica note e statistiche
    this.loadNotes();
    this.loadUserStats();
    this.loadCartelle();

    // Gestisci query parameters per filtri automatici
    this.handleQueryParams();
  }

  // Metodo per gestire i query parameters
  private handleQueryParams(): void {
    this.route.queryParams.subscribe(params => {
      // Se c'è un parametro cartella e autoFilter è true
      if (params['cartella'] && params['autoFilter'] === 'true') {
        const cartellaNome = params['cartella'];
        console.log('Filtro automatico per cartella:', cartellaNome);

        // Applica il filtro per cartella
        this.applyCartellaFilter(cartellaNome);

        // Rimuovi il parametro autoFilter dall'URL
        this.router.navigate([], {
          relativeTo: this.route,
          queryParams: { cartella: cartellaNome },
          queryParamsHandling: 'merge'
        });
      }
      // Se c'è solo il parametro cartella (senza autoFilter), mantieni il filtro
      else if (params['cartella']) {
        const cartellaNome = params['cartella'];
        this.selectedCartella.set(cartellaNome);
      }
      // Gestisci altri parametri
      else if (params['tag']) {
        const tagNome = params['tag'];
        this.selectedTag.set(tagNome);
      }
      else if (params['search']) {
        const searchQuery = params['search'];
        this.searchQuery.set(searchQuery);
        this.searchForm.patchValue({ query: searchQuery });
      }
    });
  }

  // Metodo separato per applicare il filtro cartella
  private applyCartellaFilter(cartellaNome: string): void {
    // Reset altri filtri
    this.selectedTag.set(null);
    this.searchQuery.set('');
    this.searchForm.patchValue({ query: '' });

    // Imposta il filtro per cartella
    this.selectedCartella.set(cartellaNome);

    // Carica le note filtrate dal backend
    this.notesService.getNotesByCartella(cartellaNome).subscribe({
      next: () => {
        console.log(`Note filtrate per cartella "${cartellaNome}" caricate`);
      },
      error: (error) => {
        console.error('Errore filtro per cartella:', error);
        // In caso di errore, mostra tutte le note
        this.loadNotes();
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
      next: (response) => {
        if (response.success) {
          this.userStats.set(response.stats);
        }
      },
      error: (error) => {
        console.error('Errore caricamento statistiche:', error);
      }
    });
  }

  onFilterChange(filter: 'all' | 'own' | 'shared'): void {
    this.currentFilter.set(filter);
    this.clearFilters();
    this.loadNotes();
  }

  onTagFilter(tag: string): void {
    // Aggiorna l'URL con il  filtro
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { tag: tag },
      queryParamsHandling: 'merge'
    });

    this.selectedTag.set(tag);
    this.selectedCartella.set(null);
    this.searchQuery.set('');
    this.searchForm.patchValue({ query: '' });

    this.notesService.getNotesByTag(tag).subscribe({
      error: (error) => {
        console.error('Errore filtro per tag:', error);
      }
    });
  }

  onCartellaFilter(cartella: string): void {
    // Aggiorna l'URL con il filtro
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { cartella: cartella },
      queryParamsHandling: 'merge'
    });

    this.applyCartellaFilter(cartella);
  }

  onSearch(): void {
    const query = this.searchQuery().trim();
    if (query) {
      // Aggiorna l'URL con la ricerca
      this.router.navigate([], {
        relativeTo: this.route,
        queryParams: { search: query },
        queryParamsHandling: 'merge'
      });

      this.selectedTag.set(null);
      this.selectedCartella.set(null);

      this.notesService.searchNotes(query).subscribe({
        error: (error) => {
          console.error('Errore ricerca:', error);
        }
      });
    } else {
      this.clearFilters();
    }
  }

  clearFilters(): void {
    this.selectedTag.set(null);
    this.selectedCartella.set(null);
    this.searchQuery.set('');
    this.searchForm.reset();

    // Rimuovi query parameters dall'URL
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {},
      queryParamsHandling: 'replace'
    });

    // Ricarica tutte le note
    this.loadNotes();
  }

  hasActiveFilters(): boolean {
    return !!(this.selectedTag() || this.selectedCartella() || this.searchQuery());
  }

  showCreateForm(): void {
    this.selectedNote.set(null);
    this.showNoteForm.set(true);
  }

  showEditForm(note: Note): void {
    this.selectedNote.set(note);
    this.showNoteForm.set(true);
  }

  hideNoteForm(): void {
    this.showNoteForm.set(false);
    this.selectedNote.set(null);
  }

  onNoteSave(noteData: CreateNoteRequest | UpdateNoteRequest): void {
    const selectedNote = this.selectedNote();

    if (selectedNote) {
      // Update existing note
      this.notesService.updateNote(selectedNote.id, noteData as UpdateNoteRequest).subscribe({
        next: (response) => {
          if (response.success) {
            this.hideNoteForm();
            this.loadUserStats();
          }
        },
        error: (error) => {
          console.error('Errore aggiornamento nota:', error);
        }
      });
    } else {
      // Create new note
      this.notesService.createNote(noteData as CreateNoteRequest).subscribe({
        next: (response) => {
          if (response.success) {
            this.hideNoteForm();
            this.loadUserStats();
          }
        },
        error: (error) => {
          console.error('Errore creazione nota:', error);
        }
      });
    }
  }

  onNoteDelete(noteId: number): void {
    this.notesService.deleteNote(noteId).subscribe({
      next: (response) => {
        if (response.success) {
          this.loadUserStats();
        }
      },
      error: (error) => {
        console.error('Errore eliminazione nota:', error);
      }
    });
  }

  onNoteDuplicate(noteId: number): void {
    this.notesService.duplicateNote(noteId).subscribe({
      next: (response) => {
        if (response.success) {
          this.loadUserStats();
        }
      },
      error: (error) => {
        console.error('Errore duplicazione nota:', error);
      }
    });
  }

  onNoteView(note: Note): void {
    if (note.canEdit) {
      this.showEditForm(note);
    }
  }

  toggleStats(): void {
    this.showStats.update(show => !show);
  }

  onLogout(): void {
    this.notesService.clearNotes();
    this.authService.logout();
    this.router.navigate(['/auth']);
  }

  getFilterButtonClass(filter: 'all' | 'own' | 'shared'): string {
    return this.currentFilter() === filter ? 'filter-btn active' : 'filter-btn';
  }
}
