import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { Router } from '@angular/router';
import { NotesService } from '../../services/notes';
import { AuthService } from '../../services/auth';
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
  router = inject(Router);
  private fb = inject(FormBuilder);

  // Signals from services
  notes = computed(() => this.notesService.notes());
  isLoading = computed(() => this.notesService.isLoading());
  error = computed(() => this.notesService.error());
  currentUser = computed(() => this.authService.currentUser());

  // Local signals
  showNoteForm = signal(false);
  selectedNote = signal<Note | null>(null);
  currentFilter = signal<'all' | 'own' | 'shared'>('all');
  searchQuery = signal('');
  selectedTag = signal<string | null>(null);
  selectedCartella = signal<string | null>(null);
  userStats = signal<UserStats | null>(null);
  showStats = signal(false);

  // Search form
  searchForm: FormGroup;

  // Computed values
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
    let filtered = this.notes();
    
    const search = this.searchQuery().toLowerCase();
    if (search) {
      filtered = filtered.filter(note => 
        note.titolo.toLowerCase().includes(search) ||
        note.contenuto.toLowerCase().includes(search) ||
        note.tags.some(tag => tag.toLowerCase().includes(search)) ||
        note.cartelle.some(cartella => cartella.toLowerCase().includes(search))
      );
    }
    
    return filtered;
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

    // Monitor search changes
    this.searchForm.get('query')?.valueChanges.subscribe(value => {
      this.searchQuery.set(value || '');
    });
  }

  ngOnInit(): void {
    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/auth']);
      return;
    }

    this.loadNotes();
    this.loadUserStats();
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
    this.selectedCartella.set(cartella);
    this.selectedTag.set(null);
    this.searchQuery.set('');
    this.searchForm.patchValue({ query: '' });
    
    this.notesService.getNotesByCartella(cartella).subscribe({
      error: (error) => {
        console.error('Errore filtro per cartella:', error);
      }
    });
  }

  onSearch(): void {
    const query = this.searchQuery().trim();
    if (query) {
      this.selectedTag.set(null);
      this.selectedCartella.set(null);
      
      this.notesService.searchNotes(query).subscribe({
        error: (error) => {
          console.error('Errore ricerca:', error);
        }
      });
    } else {
      this.loadNotes();
    }
  }

  clearFilters(): void {
    this.selectedTag.set(null);
    this.selectedCartella.set(null);
    this.searchQuery.set('');
    this.searchForm.reset();
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
            this.loadUserStats(); // Refresh stats
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
            this.loadUserStats(); // Refresh stats
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
          this.loadUserStats(); // Refresh stats
        }
      },
      error: (error) => {
        console.error('Errore eliminazione nota:', error);
      }
    });
  }

  onNoteDuplicate(noteId: number): void {
    console.log('Duplicazione richiesta per nota ID:', noteId);
    console.log('Utente corrente:', this.currentUsername());
    
    // Verifica che l'ID sia valido
    if (!noteId || noteId <= 0) {
        console.error('ID nota non valido:', noteId);
        return;
    }
    
    // Trova la nota nella lista locale per debug
    const noteToProcess = this.notes().find(n => n.id === noteId);
    if (noteToProcess) {
        console.log('Nota da duplicare:', {
            id: noteToProcess.id,
            titolo: noteToProcess.titolo,
            autore: noteToProcess.autore,
            canEdit: noteToProcess.canEdit,
            tipoPermesso: noteToProcess.tipoPermesso
        });
    } else {
        console.error('Nota non trovata nella lista locale con ID:', noteId);
        return;
    }
    
    this.notesService.duplicateNote(noteId).subscribe({
        next: (response) => {
            console.log('Risposta duplicazione:', response);
            if (response.success) {
                console.log('Nota duplicata con successo');
                this.loadUserStats(); // Refresh stats
            } else {
                console.error('Duplicazione fallita:', response.message);
            }
        },
        error: (error) => {
            console.error('Errore duplicazione nota:', error);
            console.error('Dettagli errore:', {
                message: error.message,
                status: error.status,
                noteId: noteId,
                username: this.currentUsername()
            });
        }
    });
}

  onNoteView(note: Note): void {
    // For now, just show edit form if user can edit
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

  hasActiveFilters(): boolean {
    return this.selectedTag() !== null || 
           this.selectedCartella() !== null || 
           this.searchQuery().trim() !== '';
  }
}