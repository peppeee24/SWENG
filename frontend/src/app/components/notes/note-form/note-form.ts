import { Component, OnInit, OnChanges, SimpleChanges, Input, Output, EventEmitter, inject, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CartelleService } from '../../../services/cartelle';
import { UserService } from '../../../services/user.service';
import { AuthService } from '../../../services/auth';
import { CreateNoteRequest, Note, Permission, UpdateNoteRequest, UpdateNoteRequestWithPermissions } from '../../../models/note.model';

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
  private authService = inject(AuthService);

  @Input() note: Note | null = null;
  @Input() isVisible = false;
  @Input() allTags: string[] = [];
  @Input() allCartelle: string[] = [];
  @Input() isLoading = false;

  @Output() save = new EventEmitter<CreateNoteRequest | UpdateNoteRequest | UpdateNoteRequestWithPermissions>();
  @Output() cancel = new EventEmitter<void>();

  noteForm: FormGroup;

  noteSignal = signal<Note | null>(null);
  currentUser = this.authService.currentUser;

  isEditMode = computed(() => {
    const editMode = this.noteSignal() !== null && this.noteSignal() !== undefined;
    console.log('isEditMode computed called, note:', this.noteSignal(), 'result:', editMode);
    return editMode;
  });

  isOwner = computed(() => {
    const note = this.noteSignal();
    const user = this.currentUser();

    if (!note || !user) {
      console.log('isOwner: no note or user', { note: !!note, user: !!user });
      return !note; // Se non c'è nota, stiamo creando (quindi è owner)
    }

    const isOwnerResult = note.autore === user.username;
    console.log('isOwner computed:', {
      noteAuthor: note.autore,
      currentUser: user.username,
      isOwner: isOwnerResult
    });

    return isOwnerResult;
  });

  canEditPermissions = computed(() => {
    const canEdit = this.isOwner();
    console.log('canEditPermissions:', canEdit);
    return canEdit;
  });

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

  ngOnChanges(changes: SimpleChanges): void {
    console.log('ngOnChanges triggered:', changes);
    console.log('note value:', this.note);
    console.log('isVisible value:', this.isVisible);

    // Aggiorna il signal quando cambia l'input
    if (changes['note']) {
      this.noteSignal.set(this.note);
      console.log('noteSignal updated to:', this.noteSignal());
      console.log('isEditMode after signal update:', this.isEditMode());
      console.log('isOwner after signal update:', this.isOwner());
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

  private loadNoteData() {
    if (!this.note) return;

    console.log('Loading note data:', this.note);
    console.log('Note author:', this.note.autore);
    console.log('Current user:', this.currentUser()?.username);

    this.noteForm.patchValue({
      titolo: this.note.titolo,
      contenuto: this.note.contenuto
    });

    this.selectedTags.set([...this.note.tags]);
    this.selectedCartelle.set([...this.note.cartelle]);
    this.permissionType.set(this.note.tipoPermesso);
    this.selectedUsersForReading.set([...this.note.permessiLettura]);
    this.selectedUsersForWriting.set([...this.note.permessiScrittura]);
    this.characterCount.set(this.note.contenuto.length);
  }

  private resetForm() {
    this.noteForm.reset();
    this.selectedTags.set([]);
    this.selectedCartelle.set([]);
    this.permissionType.set('PRIVATA');
    this.selectedUsersForReading.set([]);
    this.selectedUsersForWriting.set([]);
    this.characterCount.set(0);
    this.tagInputValue.set('');
    this.showCartelleDropdown.set(false);
    this.showUserDropdown.set(false);
  }

  onPermissionTypeChange(type: 'PRIVATA' | 'CONDIVISA_LETTURA' | 'CONDIVISA_SCRITTURA') {
    // Solo il proprietario può modificare i permessi
    if (!this.canEditPermissions()) {
      console.log('User cannot edit permissions, ignoring permission change');
      return;
    }

    this.permissionType.set(type);
    if (type === 'PRIVATA') {
      this.selectedUsersForReading.set([]);
      this.selectedUsersForWriting.set([]);
    } else if (type === 'CONDIVISA_LETTURA') {
      this.selectedUsersForWriting.set([]);
    } else if (type === 'CONDIVISA_SCRITTURA') {
      this.selectedUsersForReading.set([]);
    }
    this.showUserDropdown.set(false);
  }



  isUserSelected(username: string): boolean {
    const permType = this.permissionType();
    if (permType === 'CONDIVISA_LETTURA') {
      return this.selectedUsersForReading().includes(username);
    } else if (permType === 'CONDIVISA_SCRITTURA') {
      return this.selectedUsersForWriting().includes(username);
    }
    return false;
  }

  removeUserFromReading(username: string) {
    if (!this.canEditPermissions()) return;
    this.selectedUsersForReading.set(
      this.selectedUsersForReading().filter(u => u !== username)
    );
  }

  removeUserFromWriting(username: string) {
    if (!this.canEditPermissions()) return;
    this.selectedUsersForWriting.set(
      this.selectedUsersForWriting().filter(u => u !== username)
    );
  }

  onTagInputChange(event: any) {
    this.tagInputValue.set(event.target.value);
  }

  onTagInputKeyup(event: KeyboardEvent) {
    if (event.key === 'Enter') {
      event.preventDefault();
      this.addTag();
    }
  }

  canAddTag(): boolean {
    const tagValue = this.tagInputValue().trim();
    return tagValue.length > 0 && !this.selectedTags().includes(tagValue);
  }

  addTag() {
    const tagValue = this.tagInputValue().trim();
    if (this.canAddTag()) {
      this.selectedTags.set([...this.selectedTags(), tagValue]);
      this.tagInputValue.set('');
    }
  }

  addTagFromSuggestion(tag: string) {
    if (!this.selectedTags().includes(tag)) {
      this.selectedTags.set([...this.selectedTags(), tag]);
      this.tagInputValue.set('');
    }
  }

  removeTag(tag: string) {
    this.selectedTags.set(this.selectedTags().filter(t => t !== tag));
  }

  shouldShowTagSuggestions(): boolean {
    const inputValue = this.tagInputValue().trim().toLowerCase();
    return inputValue.length > 0 && this.getFilteredTagSuggestions().length > 0;
  }

  getFilteredTagSuggestions(): string[] {
    const inputValue = this.tagInputValue().trim().toLowerCase();
    if (inputValue.length === 0) return [];

    return this.allTags
      .filter(tag =>
        tag.toLowerCase().includes(inputValue) &&
        !this.selectedTags().includes(tag)
      )
      .slice(0, 5);
  }

  onCartellaToggle(cartella: string) {
    const current = this.selectedCartelle();
    if (current.includes(cartella)) {
      this.selectedCartelle.set(current.filter(c => c !== cartella));
    } else {
      this.selectedCartelle.set([...current, cartella]);
    }
  }

  removeCartella(cartella: string) {
    this.selectedCartelle.set(this.selectedCartelle().filter(c => c !== cartella));
  }

  onSubmit() {
    if (this.noteForm.valid) {
      const formData = this.noteForm.value;

      const permission: Permission = {
        tipoPermesso: this.permissionType(),
        utentiLettura: this.permissionType() === 'CONDIVISA_LETTURA' ? this.selectedUsersForReading() : [],
        utentiScrittura: this.permissionType() === 'CONDIVISA_SCRITTURA' ? this.selectedUsersForWriting() : []
      };

      if (this.isEditMode()) {
        const updateRequest: UpdateNoteRequestWithPermissions = {
          id: this.note!.id,
          titolo: formData.titolo,
          contenuto: formData.contenuto,
          tags: this.selectedTags(),
          cartelle: this.selectedCartelle(),
          permessi: this.canEditPermissions() ? permission : undefined
        };

        console.log('Submitting update request:', updateRequest);
        console.log('Can edit permissions:', this.canEditPermissions());

        this.save.emit(updateRequest);
      } else {
        const createRequest: CreateNoteRequest = {
          titolo: formData.titolo,
          contenuto: formData.contenuto,
          tags: this.selectedTags(),
          cartelle: this.selectedCartelle(),
          permessi: permission
        };

        console.log('Submitting create request:', createRequest);

        this.save.emit(createRequest);
      }
    }
  }


  onUserToggle(username: string) {
    // Solo il proprietario può modificare i permessi
    if (!this.canEditPermissions()) {
      console.log('User cannot edit permissions, ignoring user toggle');
      return;
    }

    const permType = this.permissionType();
    console.log('onUserToggle called:', {
      username,
      permissionType: permType,
      currentReadingUsers: this.selectedUsersForReading(),
      currentWritingUsers: this.selectedUsersForWriting()
    });

    if (permType === 'CONDIVISA_LETTURA') {
      const current = this.selectedUsersForReading();
      if (current.includes(username)) {
        this.selectedUsersForReading.set(current.filter(u => u !== username));
        console.log('User removed from reading:', username);
      } else {
        this.selectedUsersForReading.set([...current, username]);
        console.log('User added to reading:', username);
      }
    } else if (permType === 'CONDIVISA_SCRITTURA') {
      const current = this.selectedUsersForWriting();
      if (current.includes(username)) {
        this.selectedUsersForWriting.set(current.filter(u => u !== username));
        console.log('User removed from writing:', username);
      } else {
        this.selectedUsersForWriting.set([...current, username]);
        console.log('User added to writing:', username);
      }
    }

    console.log('After toggle:', {
      readingUsers: this.selectedUsersForReading(),
      writingUsers: this.selectedUsersForWriting()
    });
  }

  onCancel() {
    this.cancel.emit();
  }

  onOverlayClick() {
    this.onCancel();
  }
}
