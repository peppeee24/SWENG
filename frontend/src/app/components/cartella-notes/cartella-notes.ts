import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { NotesService } from '../../services/notes';
import { AuthService } from '../../services/auth';
import { CartelleService } from '../../services/cartelle';
import { Note, CreateNoteRequest, UpdateNoteRequest } from '../../models/note.model';
import { Cartella } from '../../models/cartella.model';
import { NoteCardComponent } from '../notes/note-card/note-card';
import { NoteFormComponent } from '../notes/note-form/note-form';

@Component({
  selector: 'app-cartella-notes',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NoteCardComponent, NoteFormComponent],
  templateUrl: './cartella-notes.html',
  styleUrls: ['./cartella-notes.css']
})
export class CartellaNoteComponent implements OnInit {
  private notesService = inject(NotesService);
  private authService = inject(AuthService);
  private cartelleService = inject(CartelleService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private fb = inject(FormBuilder);

  cartellaNome = signal<string>('');
  cartellaInfo = signal<Cartella | null>(null);
  notes = signal<Note[]>([]);
  isLoading = signal(false);
  error = signal<string | null>(null);
  showNoteForm = signal(false);
  selectedNote = signal<Note | null>(null);
  searchQuery = signal('');

  searchForm: FormGroup;

  currentUser = computed(() => this.authService.currentUser());

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

  filteredNotes = computed(() => {
    let filtered = this.notes();

    const search = this.searchQuery().toLowerCase();
    if (search) {
      filtered = filtered.filter(note =>
        note.titolo.toLowerCase().includes(search) ||
        note.contenuto.toLowerCase().includes(search) ||
        note.tags.some(tag => tag.toLowerCase().includes(search))
      );
    }

    return filtered;
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

    // Ottieni il nome della cartella dai parametri URL
    this.route.params.subscribe(params => {
      const nomeCartella = params['nome'];
      if (nomeCartella) {
        this.cartellaNome.set(decodeURIComponent(nomeCartella));
        this.loadCartellaInfo();
        this.loadNotesForCartella();
      } else {
        this.router.navigate(['/cartelle']);
      }
    });
  }

  loadCartellaInfo(): void {
    const nome = this.cartellaNome();

    // Trova la cartella nelle cartelle caricate
    this.cartelleService.getAllCartelle().subscribe({
      next: () => {
        const cartelle = this.cartelleService.cartelle();
        const cartella = cartelle.find(c => c.nome === nome);
        if (cartella) {
          this.cartellaInfo.set(cartella);
        }
      },
      error: (error) => {
        console.error('Errore caricamento info cartella:', error);
      }
    });
  }

  loadNotesForCartella(): void {
    const nome = this.cartellaNome();
    if (!nome) return;

    this.isLoading.set(true);
    this.error.set(null);

    // Usa l'API per ottenere le note filtrate per cartella
    this.notesService.getNotesByCartella(nome).subscribe({
      next: (response) => {
        this.isLoading.set(false);
        if (response.success) {
          this.notes.set(response.notes);
          console.log(`Caricate ${response.count} note per cartella: ${nome}`);
        }
      },
      error: (error) => {
        this.isLoading.set(false);
        this.error.set('Errore durante il caricamento delle note');
        console.error('Errore caricamento note cartella:', error);
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/cartelle']);
  }

  goToAllNotes(): void {
    this.router.navigate(['/notes']);
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
      this.notesService.updateNote(selectedNote.id, noteData as UpdateNoteRequest).subscribe({
        next: (response) => {
          if (response.success) {
            this.hideNoteForm();
            this.loadNotesForCartella(); // Ricarica le note
          }
        },
        error: (error) => {
          console.error('Errore aggiornamento nota:', error);
        }
      });
    } else {
      // Create new note
      const createData = noteData as CreateNoteRequest;
      if (!createData.cartelle) {
        createData.cartelle = [];
      }
      if (!createData.cartelle.includes(this.cartellaNome())) {
        createData.cartelle.push(this.cartellaNome());
      }

      this.notesService.createNote(createData).subscribe({
        next: (response) => {
          if (response.success) {
            this.hideNoteForm();
            this.loadNotesForCartella(); // Ricarica le note
          }
        },
        error: (error) => {
          console.error('Errore creazione nota:', error);
        }
      });
    }
  }

  onNoteDelete(noteId: number): void {
    if (confirm('Sei sicuro di voler eliminare questa nota?')) {
      this.notesService.deleteNote(noteId).subscribe({
        next: (response) => {
          if (response.success) {
            this.loadNotesForCartella(); // Ricarica le note
          }
        },
        error: (error) => {
          console.error('Errore eliminazione nota:', error);
        }
      });
    }
  }

  onNoteDuplicate(noteId: number): void {
    this.notesService.duplicateNote(noteId).subscribe({
      next: (response) => {
        if (response.success) {
          this.loadNotesForCartella(); // Ricarica le note
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

  clearSearch(): void {
    this.searchQuery.set('');
    this.searchForm.reset();
  }

  onLogout(): void {
    this.authService.logout();
    this.router.navigate(['/auth']);
  }
}
