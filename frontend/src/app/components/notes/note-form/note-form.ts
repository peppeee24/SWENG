import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';


interface UpdateNoteRequestWithPermissions extends UpdateNoteRequest {
  permessi?: Permission;
}
import { CartelleService } from '../../../services/cartelle';
import { UserService } from '../../../services/user.service';
import {CreateNoteRequest, Note, Permission, UpdateNoteRequest} from '../../../models/note.model';

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

  @Output() save = new EventEmitter<CreateNoteRequest | UpdateNoteRequest | UpdateNoteRequestWithPermissions>();
  @Output() cancel = new EventEmitter<void>();

  noteForm: FormGroup;


  noteSignal = signal<Note | null>(null);
  isEditMode = computed(() => {
    const editMode = this.noteSignal() !== null && this.noteSignal() !== undefined;
    console.log('isEditMode computed called, note:', this.noteSignal(), 'result:', editMode);
    return editMode;
  });


  isOwner = computed(() => {
    const note = this.noteSignal();
    if (!note) return true; // In modalità creazione, l'utente è sempre "proprietario"

    // Qui dovremmo controllare se l'utente corrente è il proprietario
    // Per ora assumiamo che in edit mode sia sempre il proprietario
    // Nel prossimo sprint si implementerà la logica per utenti condivisi
    return true;
  });


  canEditPermissions = computed(() => {
    return this.isOwner() && !this.isEditMode(); // Solo proprietario e solo in creazione per ora
  });

  // Computed per determinare se può modificare tutto (proprietario in edit)
  canEditEverything = computed(() => {
    return this.isOwner();
  });

  characterCount = signal(0);
  maxCharacters = 280;

  tagInputValue = signal('');
  showCartelleDropdown = signal(false);

  selectedTags = signal<string[]>([]);
  selectedCartelle = signal<string[]>([]);

  permissionType = signal<'PRIVATA' | 'CONDIVISA_LETTURA' | 'CONDIVISA_SCRITTURA'>('PRIVATA');
  selectedUsersForReading = signal<string[]>([]);
  selectedUsersForWriting = signal<string[]>([]);
  showUserDropdown = signal(false);

  availableCartelle = computed(() => this.cartelleService.cartelle());
  availableUsers = computed(() => this.userService.users());

  constructor() {
    this.noteForm = this.fb.group({
      titolo: ['', [Validators.required, Validators.maxLength(100)]],
      contenuto: ['', [Validators.required, Validators.maxLength(280)]]
    });

    this.noteForm.get('contenuto')?.valueChanges.subscribe(value => {
      this.characterCount.set(value ? value.length : 0);
    });
  }

  ngOnInit(): void {
    this.loadCartelle();
    this.loadUsers();

    if (this.note) {
      this.loadNoteData();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    console.log('ngOnChanges triggered:', changes);
    console.log('note value:', this.note);
    console.log('isVisible value:', this.isVisible);

    // Aggiorna il signal quando cambia l'input
    if (changes['note']) {
      this.noteSignal.set(this.note);
      console.log('noteSignal updated to:', this.noteSignal());
      console.log('isEditMode after signal update:', this.isEditMode());
    }

    if (changes['isVisible']) {
      if (!changes['isVisible'].currentValue && changes['isVisible'].previousValue) {
        // Modal chiuso
        this.resetForm();
      } else if (changes['isVisible'].currentValue && !changes['isVisible'].previousValue) {
        // Modal aperto
        this.noteSignal.set(this.note);
        if (this.note) {
          this.loadNoteData();
        } else {
          this.resetForm();
        }
      }
    }

    if (changes['note']) {
      console.log('Note changed from', changes['note'].previousValue, 'to', changes['note'].currentValue);
      if (this.note && this.isVisible) {
        this.loadNoteData();
      } else if (!this.note && this.isVisible) {
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
    console.log('loadNoteData called with note:', this.note);
    if (!this.note) {
      console.log('No note to load, returning');
      return;
    }

    console.log('Loading note data for edit mode');
    this.noteForm.patchValue({
      titolo: this.note.titolo,
      contenuto: this.note.contenuto
    });

    this.selectedTags.set([...this.note.tags]);
    this.selectedCartelle.set([...this.note.cartelle]);
    this.characterCount.set(this.note.contenuto.length);

    if (this.note.tipoPermesso) {
      this.permissionType.set(this.note.tipoPermesso);
    }

    if (this.note.permessiLettura) {
      this.selectedUsersForReading.set([...this.note.permessiLettura]);
    }

    if (this.note.permessiScrittura) {
      this.selectedUsersForWriting.set([...this.note.permessiScrittura]);
    }

    console.log('Note data loaded, isEditMode now:', this.isEditMode());
  }

  private resetForm(): void {
    this.noteForm.reset();
    this.selectedTags.set([]);
    this.selectedCartelle.set([]);
    this.characterCount.set(0);
    this.tagInputValue.set('');
    this.showCartelleDropdown.set(false);

    if (!this.isEditMode()) {
      this.permissionType.set('PRIVATA');
      this.selectedUsersForReading.set([]);
      this.selectedUsersForWriting.set([]);
    }
    this.showUserDropdown.set(false);

    // Reset del signal
    if (!this.note) {
      this.noteSignal.set(null);
    }
  }

  getCharacterWidth(): number {
    return (this.characterCount() / this.maxCharacters) * 100;
  }

  getCharacterDisplay(): string {
    return `${this.characterCount()}/${this.maxCharacters}`;
  }

  onTagInputChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.tagInputValue.set(target.value);
  }

  onTagInputKeyup(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      event.preventDefault();
      this.addTag();
    } else if (event.key === 'Backspace' && this.tagInputValue() === '' && this.selectedTags().length > 0) {
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

  onCartellaToggle(cartella: string): void {
    const current = this.selectedCartelle();
    if (current.includes(cartella)) {
      this.selectedCartelle.set(current.filter(c => c !== cartella));
    } else {
      this.selectedCartelle.set([...current, cartella]);
    }
  }

  onPermissionTypeChange(type: 'PRIVATA' | 'CONDIVISA_LETTURA' | 'CONDIVISA_SCRITTURA'): void {
    console.log('Cambio tipo permesso a:', type);
    console.log('Utenti disponibili:', this.availableUsers());
    console.log('Numero utenti disponibili:', this.availableUsers().length);

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
        if (!this.selectedUsersForReading().includes(username)) {
          this.selectedUsersForReading.set([...this.selectedUsersForReading(), username]);
        }
      }
    } else {
      const current = this.selectedUsersForReading();
      if (current.includes(username)) {
        this.selectedUsersForReading.set(current.filter(u => u !== username));
        this.selectedUsersForWriting.set(this.selectedUsersForWriting().filter(u => u !== username));
      } else {
        this.selectedUsersForReading.set([...current, username]);
      }
    }
  }

  onSubmit(): void {
    if (this.noteForm.valid) {
      const formValue = this.noteForm.value;

      if (this.isEditMode()) {
        const updateRequest: UpdateNoteRequest = {
          id: this.note!.id,
          titolo: formValue.titolo.trim(),
          contenuto: formValue.contenuto.trim(),
          tags: this.selectedTags(),
          cartelle: this.selectedCartelle()
        };
        this.save.emit(updateRequest);
      } else {
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
