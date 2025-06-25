import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Note, CreateNoteRequest, UpdateNoteRequest, Permission } from '../../../models/note.model';
import { CartelleService } from '../../../services/cartelle';
import { UserService } from '../../../services/user.service';

@Component({
  selector: 'app-note-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './note-form.html',
  styleUrls: ['./note-form.css']
})
export class NoteFormComponent implements OnInit, OnChanges {
  private fb = inject(FormBuilder);
  private cartelleService = inject(CartelleService);
  private userService = inject(UserService);

  @Input() note: Note | null = null;
  @Input() isVisible = false;
  @Input() allTags: string[] = [];
  @Input() allCartelle: string[] = [];
  @Input() isLoading = false;

  @Output() save = new EventEmitter<CreateNoteRequest | UpdateNoteRequest>();
  @Output() cancel = new EventEmitter<void>();

  noteForm: FormGroup;

  isEditMode = computed(() => this.note !== null);

  characterCount = signal(0);
  maxCharacters = 280;

  tagInputValue = signal('');
  showCartelleDropdown = signal(false);

  selectedTags = signal<string[]>([]);
  selectedCartelle = signal<string[]>([]);

  //  PROPRIETÀ PER PERMESSI
  permissionType = signal<'PRIVATA' | 'CONDIVISA_LETTURA' | 'CONDIVISA_SCRITTURA'>('PRIVATA');
  selectedUsersForReading = signal<string[]>([]);
  selectedUsersForWriting = signal<string[]>([]);
  showUserDropdown = signal(false);

  // Cartelle disponibili dal servizio
  availableCartelle = computed(() => this.cartelleService.cartelle());
  //  Utenti disponibili dal servizio
  availableUsers = computed(() => this.userService.users());

  constructor() {
    this.noteForm = this.fb.group({
      titolo: ['', [Validators.required, Validators.maxLength(100)]],
      contenuto: ['', [Validators.required, Validators.maxLength(280)]]
    });

    // Monitor character count
    this.noteForm.get('contenuto')?.valueChanges.subscribe(value => {
      this.characterCount.set(value ? value.length : 0);
    });
  }

  ngOnInit(): void {
    if (this.note) {
      this.loadNoteData();
    }
    // Carica le cartelle all'inizializzazione
    this.loadCartelle();
    //  Carica gli utenti per i permessi
    this.loadUsers();
  }

  ngOnChanges(changes: SimpleChanges): void {
    // Reset form quando isVisible diventa false
    if (changes['isVisible'] && !changes['isVisible'].currentValue) {
      this.resetForm();
    }

    // Carica dati della nota quando cambia
    if (changes['note']) {
      if (this.note) {
        this.loadNoteData();
      } else {
        this.resetForm();
      }
    }
  }

  private loadCartelle(): void {
    this.cartelleService.getAllCartelle().subscribe({
      next: () => {
        console.log('Cartelle caricate per il form');
      },
      error: (error) => {
        console.error('Errore caricamento cartelle:', error);
      }
    });
  }

  //  Carica utenti per i permessi
  private loadUsers(): void {
    console.log('---Caricamento utenti...');
    this.userService.getAllUsers().subscribe({
      next: (users) => {
        console.log('-----Utenti caricati:', users);
        console.log('-----Numero utenti:', users.length);
        console.log('-----Signal utenti:', this.availableUsers());
      },
      error: (error) => {
        console.error('Errore caricamento utenti:', error);
      }
    });
  }

  private loadNoteData(): void {
    if (this.note) {
      this.noteForm.patchValue({
        titolo: this.note.titolo,
        contenuto: this.note.contenuto
      });

      // Copia array per evitare mutazioni
      this.selectedTags.set([...this.note.tags]);
      this.selectedCartelle.set([...this.note.cartelle]);
      this.characterCount.set(this.note.contenuto.length);

      //  Carica dati permessi se in edit mode
      this.permissionType.set(this.note.tipoPermesso);
      this.selectedUsersForReading.set([...this.note.permessiLettura]);
      this.selectedUsersForWriting.set([...this.note.permessiScrittura]);
    }
  }

  private resetForm(): void {
    this.noteForm.reset();
    this.selectedTags.set([]);
    this.selectedCartelle.set([]);
    this.characterCount.set(0);
    this.tagInputValue.set('');
    this.showCartelleDropdown.set(false);

    //  Reset permessi
    this.permissionType.set('PRIVATA');
    this.selectedUsersForReading.set([]);
    this.selectedUsersForWriting.set([]);
    this.showUserDropdown.set(false);
  }

  // Helper methods per template (per evitare errori di parsing)
  getCharacterWidth(): number {
    return (this.characterCount() / this.maxCharacters) * 100;
  }

  getCharacterDisplay(): string {
    return `${this.characterCount()}/${this.maxCharacters}`;
  }

  // Tag management methods
  onTagInputChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.tagInputValue.set(target.value);
  }

  onTagInputKeyup(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      event.preventDefault();
      this.addTag();
    } else if (event.key === 'Backspace' && this.tagInputValue() === '' && this.selectedTags().length > 0) {
      // Remove last tag when backspace on empty input
      this.selectedTags.set(this.selectedTags().slice(0, -1));
    }
  }

  addTag(): void {
    const tagValue = this.tagInputValue().trim();
    if (tagValue && !this.selectedTags().includes(tagValue) && tagValue.length <= 50) {
      this.selectedTags.update(tags => [...tags, tagValue]);
      this.tagInputValue.set('');
    }
  }

  canAddTag(): boolean {
    const tagValue = this.tagInputValue().trim();
    return tagValue.length > 0 &&
      !this.selectedTags().includes(tagValue) &&
      tagValue.length <= 50;
  }

  removeTag(tag: string): void {
    this.selectedTags.update(tags => tags.filter(t => t !== tag));
  }

  addTagFromSuggestion(tag: string): void {
    if (!this.selectedTags().includes(tag)) {
      this.selectedTags.update(tags => [...tags, tag]);
      this.tagInputValue.set('');
    }
  }

  shouldShowTagSuggestions(): boolean {
    return this.tagInputValue().length > 0 && this.getFilteredTagSuggestions().length > 0;
  }

  getFilteredTagSuggestions(): string[] {
    const input = this.tagInputValue().toLowerCase();
    if (!input) return [];

    return this.allTags
      .filter(tag =>
        tag.toLowerCase().includes(input) &&
        !this.selectedTags().includes(tag)
      )
      .slice(0, 5);
  }

  // Cartelle management methods
  onCartellaToggle(cartella: string): void {
    const current = this.selectedCartelle();
    if (current.includes(cartella)) {
      this.selectedCartelle.set(current.filter(c => c !== cartella));
    } else {
      this.selectedCartelle.set([...current, cartella]);
    }
  }

  // NUOVI METODI PER GESTIONE PERMESSI
  onPermissionTypeChange(type: 'PRIVATA' | 'CONDIVISA_LETTURA' | 'CONDIVISA_SCRITTURA'): void {
    console.log('🔄 Cambio tipo permesso a:', type);
    console.log('👥 Utenti disponibili:', this.availableUsers());
    console.log('📊 Numero utenti disponibili:', this.availableUsers().length);

    this.permissionType.set(type);
    if (type === 'PRIVATA') {
      this.selectedUsersForReading.set([]);
      this.selectedUsersForWriting.set([]);
    }
  }

  onUserToggle(username: string, forWriting = false): void {
    if (forWriting) {
      const current = this.selectedUsersForWriting();
      if (current.includes(username)) {
        this.selectedUsersForWriting.set(current.filter(u => u !== username));
      } else {
        this.selectedUsersForWriting.set([...current, username]);
        // Se aggiungi per scrittura, aggiungi automaticamente anche per lettura
        if (!this.selectedUsersForReading().includes(username)) {
          this.selectedUsersForReading.set([...this.selectedUsersForReading(), username]);
        }
      }
    } else {
      const current = this.selectedUsersForReading();
      if (current.includes(username)) {
        this.selectedUsersForReading.set(current.filter(u => u !== username));
        // Se rimuovi dalla lettura, rimuovi anche dalla scrittura
        this.selectedUsersForWriting.set(this.selectedUsersForWriting().filter(u => u !== username));
      } else {
        this.selectedUsersForReading.set([...current, username]);
      }
    }
  }

  // Form submission
  onSubmit(): void {
    if (this.noteForm.valid) {
      const formValue = this.noteForm.value;

      if (this.isEditMode()) {
        // Modalità modifica - non include permessi
        const updateRequest: UpdateNoteRequest = {
          id: this.note!.id,
          titolo: formValue.titolo.trim(),
          contenuto: formValue.contenuto.trim(),
          tags: this.selectedTags(),
          cartelle: this.selectedCartelle()
        };
        this.save.emit(updateRequest);
      } else {
        // Modalità creazione - include permessi
        const permission: Permission = {
          tipoPermesso: this.permissionType(),
          utentiLettura: this.selectedUsersForReading(),
          utentiScrittura: this.selectedUsersForWriting()
        };

        const createRequest: CreateNoteRequest = {
          titolo: formValue.titolo.trim(),
          contenuto: formValue.contenuto.trim(),
          tags: this.selectedTags(),
          cartelle: this.selectedCartelle(),
          permessi: permission
        };
        this.save.emit(createRequest);
      }
    }
  }

  onCancel(): void {
    this.cancel.emit();
  }
}
