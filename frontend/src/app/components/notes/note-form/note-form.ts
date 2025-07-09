import { Component, OnInit, OnChanges, OnDestroy, SimpleChanges, Input, Output, EventEmitter, inject, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CartelleService } from '../../../services/cartelle';
import { UserService } from '../../../services/user.service';
import { NotesService } from '../../../services/notes';
import { CreateNoteRequest, Note, Permission, UpdateNoteRequest, UpdateNoteRequestWithPermissions } from '../../../models/note.model';
import { interval, Subscription } from 'rxjs';
import { AuthService } from '../../../services/auth';

// Interfaccia per i permessi (definita all'interno del file)
interface PermissionsRequest {
  tipoPermesso: 'PRIVATA' | 'CONDIVISA_LETTURA' | 'CONDIVISA_SCRITTURA';
  utentiLettura: string[];
  utentiScrittura: string[];
}

/**
 * Componente standalone per la gestione del form di creazione/modifica di una nota.
 * Supporta:
 * - Modalità creazione e modifica (basato su presenza di nota in input)
 * - Gestione permessi di condivisione (privata, lettura, scrittura)
 * - Gestione tag e cartelle associate
 * - Sistema di lock per evitare modifiche concorrenti (con rinnovo periodico)
 * - Validazione form, conteggio caratteri e suggerimenti tag
 * - Iniezione di servizi per cartelle, utenti, note e autenticazione
 */

@Component({
  selector: 'app-note-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './note-form.html',
  styleUrls: ['./note-form.css']
})
export class NoteFormComponent implements OnInit, OnChanges, OnDestroy {
  private fb = inject(FormBuilder);
  private cartelleService = inject(CartelleService);
  private userService = inject(UserService);
  private notesService = inject(NotesService);
  private authService = inject(AuthService);

  @Input() note: Note | null = null;
  @Input() isVisible = false;
  @Input() allTags: string[] = [];
  @Input() allCartelle: string[] = [];
  @Input() isLoading = false;

  @Output() save = new EventEmitter<CreateNoteRequest | UpdateNoteRequest | UpdateNoteRequestWithPermissions>();
  @Output() cancel = new EventEmitter<void>();

  noteForm: FormGroup;

  // Signals per la gestione dello stato
  noteSignal = signal<Note | null>(null);

  // Sistema di lock
  private lockRefreshSubscription?: Subscription;
  isNoteLocked = signal(false);
  lockedByUser = signal<string | null>(null);
  lockError = signal<string | null>(null);

  isEditMode = computed(() => {
    const editMode = this.noteSignal() !== null && this.noteSignal() !== undefined;
    console.log('isEditMode computed called, note:', this.noteSignal(), 'result:', editMode);
    return editMode;
  });

  isOwner = computed(() => {
    const note = this.noteSignal();
    if (!note) return true;

    const currentUser = this.authService.getCurrentUser();
    if (!currentUser) return false;

    // Confronta l'username dell'utente corrente con l'autore della nota
    return note.autore === currentUser.username;
  });

  canEditPermissions = computed(() => {
    return this.isOwner();
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

  /**
   * Carica dati esterni cartelle e utenti all'inizializzazione
   * Se c'è una nota, carica i dati della nota nel form
   */
  ngOnInit(): void {
    this.loadCartelle();
    this.loadUsers();

    if (this.note) {
      this.loadNoteData();
    }
  }

  /**
   * Pulisce risorse all'uscita (rilascia lock)
   */
  ngOnDestroy(): void {
    // Rilascia il lock quando il componente viene distrutto
    this.releaseLock();
  }

  /**
   * Carica le cartelle da servizio
   */
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

  /**
   * Carica gli utenti da servizio
   */
  private loadUsers(): void {
    console.log(' Caricamento utenti...');
    this.userService.getAllUsers().subscribe({
      next: (users) => {
        console.log(' Utenti caricati:', users);
        console.log(' Numero utenti:', users.length);
        console.log(' Signal utenti:', this.availableUsers());
      },
      error: (error) => {
        console.error(' Errore caricamento utenti:', error);
      }
    });
  }

  // SISTEMA DI LOCK - METODI PRINCIPALI
  private async acquireLock(): Promise<boolean> {
    if (!this.note?.id) {
      console.log(' Nuova nota, non serve lock');
      return true; // Nuova nota, non serve lock
    }

    console.log(' Tentativo di acquisire lock per nota:', this.note.id);
    this.lockError.set(null);

    try {
      const response = await this.notesService.lockNote(this.note.id).toPromise();

      if (response?.success) {
        console.log(' Lock acquisito con successo per nota:', this.note.id);
        this.isNoteLocked.set(true);
        this.lockedByUser.set(response.lockedBy || 'current_user');
        this.startLockRefresh();
        return true;
      } else {
        const errorMsg = response?.message || 'Nota già in modifica';
        console.log(' Lock non acquisito:', errorMsg);
        this.lockError.set(errorMsg);
        alert(`Impossibile modificare la nota: ${errorMsg}`);
        return false;
      }
    } catch (error: any) {
      console.error(' Errore acquisizione lock:', error);
      const errorMsg = error.error?.message || 'La nota è già in modifica da un altro utente';
      this.lockError.set(errorMsg);
      alert(`Errore: ${errorMsg}`);
      return false;
    }
  }

  /**
   * Avvia il rinnovo automatico del lock ogni 90 secondi per mantenerlo attivo.
   */
  private startLockRefresh(): void {
    if (!this.note?.id) return;

    console.log(' Avvio refresh automatico lock per nota:', this.note.id);

    // Rinnova il lock ogni 90 secondi (il lock dura 2 minuti)
    this.lockRefreshSubscription = interval(90000).subscribe(() => {
      if (this.note?.id && this.isNoteLocked()) {
        console.log(' Rinnovo automatico lock...');
        this.notesService.refreshLock(this.note.id).subscribe({
          next: (response) => {
            if (response?.success && this.note?.id) {
              console.log(' Lock rinnovato automaticamente per nota:', this.note.id);
            } else {
              console.warn('️ Errore rinnovo lock:', response?.message);
              this.isNoteLocked.set(false);
              this.lockError.set('Lock scaduto - la nota potrebbe essere modificata da altri');
            }
          },
          error: (error) => {
            console.error(' Errore rinnovo automatico lock:', error);
            this.isNoteLocked.set(false);
            this.lockError.set('Lock scaduto - riprova ad aprire la nota');
          }
        });
      }
    });
  }

  /**
   * Rilascia il lock sulla nota (ad esempio quando si chiude il form o salva).
   */
  private releaseLock(): void {
    if (this.note?.id && this.isNoteLocked()) {
      console.log(' Rilascio lock per nota:', this.note.id);

      this.notesService.unlockNote(this.note.id).subscribe({
        next: (response) => {
          if (this.note?.id) {
            console.log('Lock rilasciato con successo per nota:', this.note.id);
          }
        },
        error: (error) => {
          console.error(' Errore rilascio lock:', error);
        }
      });
    }

    // Reset stato lock
    this.isNoteLocked.set(false);
    this.lockedByUser.set(null);
    this.lockError.set(null);

    // Stop refresh automatico
    if (this.lockRefreshSubscription) {
      this.lockRefreshSubscription.unsubscribe();
      this.lockRefreshSubscription = undefined;
      console.log(' Fermato refresh automatico lock');
    }
  }

  onPermissionsSave(): void {
    if (!this.note) {
      console.error(' Errore: nessuna nota selezionata per il cambio permessi');
      return;
    }

    // Debug dello stato corrente
    console.log(' Stato PRIMA del cambio permessi:');
    console.log('   - Nota ID:', this.note.id);
    console.log('   - Tipo permesso attuale:', this.note.tipoPermesso);
    console.log('   - Permessi lettura attuali:', this.note.permessiLettura);
    console.log('   - Permessi scrittura attuali:', this.note.permessiScrittura);

    // Debug dei valori del form
    console.log(' Valori dal form:');
    console.log('   - permissionType():', this.permissionType());
    console.log('   - selectedUsersForReading():', this.selectedUsersForReading());
    console.log('   - selectedUsersForWriting():', this.selectedUsersForWriting());

    const permessi: PermissionsRequest = {
      tipoPermesso: this.permissionType(),
      utentiLettura: this.selectedUsersForReading(),
      utentiScrittura: this.selectedUsersForWriting()
    };

    console.log(' Permessi da inviare al server:', permessi);

    // Verifica che i dati siano validi
    if (!permessi.tipoPermesso) {
      console.error(' Errore: tipo permesso non valido');
      alert('Errore: tipo permesso non valido');
      return;
    }

    console.log(' Invio richiesta al server...');

    this.notesService.updateNotePermissions(this.note.id, permessi).subscribe({
      next: (response) => {
        console.log(' Risposta ricevuta dal server:', response);

        if (response.success) {
          console.log(' Permessi salvati con successo');
          console.log(' Nota aggiornata:', response.note || response.data);

          // Verifica che la nota sia stata aggiornata correttamente
          const updatedNote = response.note || response.data;
          if (updatedNote) {
            console.log(' Verifica stato DOPO il salvataggio:');
            console.log('   - Tipo permesso nuovo:', updatedNote.tipoPermesso);
            console.log('   - Permessi lettura nuovi:', updatedNote.permessiLettura);
            console.log('   - Permessi scrittura nuovi:', updatedNote.permessiScrittura);
          }

          // Forza il refresh della nota dopo un piccolo delay
          console.log(' Avvio refresh della nota...');
          setTimeout(() => {
            this.notesService.refreshNote(this.note!.id);
            console.log(' Refresh completato');
          }, 500);

          // Chiudi il form e mostra messaggio
          this.cancel.emit();
          alert('Permessi aggiornati con successo!');
        } else {
          console.error(' Errore: risposta del server indica fallimento');
          console.error('   - Messaggio:', response.message);
          alert('Errore durante l\'aggiornamento dei permessi: ' + (response.message || 'Errore sconosciuto'));
        }
      },
      error: (error) => {
        console.error(' Errore durante la chiamata al server:', error);
        console.error('   - Status:', error.status);
        console.error('   - Message:', error.message);
        console.error('   - Error body:', error.error);

        let errorMessage = 'Errore durante l\'aggiornamento dei permessi';
        if (error.error?.message) {
          errorMessage += ': ' + error.error.message;
        } else if (error.message) {
          errorMessage += ': ' + error.message;
        }

        alert(errorMessage);
      }
    });
  }

  /**
   * Gestisce cambiamenti degli input (nota, visibilità)
   * - Quando si apre il modal tenta di acquisire il lock sulla nota da modificare
   * - Quando si chiude il modal rilascia il lock e resetta il form
   * - Quando cambia la nota aggiorna il form con i nuovi dati
   */
  async ngOnChanges(changes: SimpleChanges): Promise<void> {
    console.log('ngOnChanges triggered:', changes);
    console.log('note value:', this.note);
    console.log(' isVisible value:', this.isVisible);

    // Aggiorna il signal quando cambia l'input
    if (changes['note']) {
      this.noteSignal.set(this.note);
      console.log(' noteSignal updated to:', this.noteSignal());
      console.log('✏ isEditMode after signal update:', this.isEditMode());
    }

    if (changes['isVisible']) {
      if (!changes['isVisible'].currentValue && changes['isVisible'].previousValue) {
        // Modal chiuso - rilascia lock
        console.log(' Modal chiuso - rilascio lock');
        this.releaseLock();
        this.resetForm();
      } else if (changes['isVisible'].currentValue && !changes['isVisible'].previousValue) {
        // Modal aperto
        console.log(' Modal aperto');
        this.noteSignal.set(this.note);

        if (this.note) {
          // ACQUISIRE LOCK PRIMA DI CARICARE I DATI
          console.log(' Nota esistente - acquisizione lock necessaria');
          const lockAcquired = await this.acquireLock();

          if (lockAcquired) {
            console.log(' Lock acquisito - caricamento dati nota');
            this.loadNoteData();
          } else {
            console.log(' Lock non acquisito - chiusura modal');
            // Se non riesci ad acquisire il lock, chiudi il modal
            this.cancel.emit();
            return;
          }
        } else {
          console.log(' Nuova nota - nessun lock necessario');
          this.resetForm();
        }
      }
    }

    if (changes['note']) {
      console.log(' Note changed from', changes['note'].previousValue, 'to', changes['note'].currentValue);
      if (this.note && this.isVisible) {
        this.loadNoteData();
      } else if (!this.note && this.isVisible) {
        this.resetForm();
      }
    }
  }

  /**
   * Carica i dati della nota nel form e aggiorna gli stati interni
   */
  private loadNoteData() {
    if (!this.note) return;

    console.log(' Loading note data:', this.note);
    console.log(' Note author:', this.note.autore);

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

  /**
   * Reset completo del form e degli stati
   */
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
    this.lockError.set(null);
  }

  /**
   * Gestisce il cambio del tipo di permesso aggiornando le liste utenti appropriate
   */
  onPermissionTypeChange(type: 'PRIVATA' | 'CONDIVISA_LETTURA' | 'CONDIVISA_SCRITTURA') {
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

  // Metodi per dropdown utenti, aggiunta/rimozione utenti da liste di permessi
  toggleUserDropdown() {
    this.showUserDropdown.set(!this.showUserDropdown());
  }

  addUserToReading(username: string) {
    const current = this.selectedUsersForReading();
    if (!current.includes(username)) {
      this.selectedUsersForReading.set([...current, username]);
    }
    this.showUserDropdown.set(false);
  }

  addUserToWriting(username: string) {
    const current = this.selectedUsersForWriting();
    if (!current.includes(username)) {
      this.selectedUsersForWriting.set([...current, username]);
    }
    this.showUserDropdown.set(false);
  }

  removeUserFromReading(username: string) {
    const current = this.selectedUsersForReading();
    this.selectedUsersForReading.set(current.filter(u => u !== username));
  }

  removeUserFromWriting(username: string) {
    const current = this.selectedUsersForWriting();
    this.selectedUsersForWriting.set(current.filter(u => u !== username));
  }

  // Metodi per gestione tag
  addTag() {
    const tagValue = this.tagInputValue().trim();
    if (tagValue && !this.selectedTags().includes(tagValue)) {
      this.selectedTags.set([...this.selectedTags(), tagValue]);
      this.tagInputValue.set('');
    }
  }

  removeTag(tag: string) {
    this.selectedTags.set(this.selectedTags().filter(t => t !== tag));
  }

  onTagInputKeydown(event: KeyboardEvent) {
    if (event.key === 'Enter') {
      event.preventDefault();
      this.addTag();
    }
  }

  // Dropdown cartelle
  toggleCartelleDropdown() {
    this.showCartelleDropdown.set(!this.showCartelleDropdown());
  }

  selectCartella(cartella: string) {
    const current = this.selectedCartelle();
    if (!current.includes(cartella)) {
      this.selectedCartelle.set([...current, cartella]);
    }
    this.showCartelleDropdown.set(false);
  }

  removeCartella(cartella: string) {
    this.selectedCartelle.set(this.selectedCartelle().filter(c => c !== cartella));
  }

  /**
   * Submit del form: crea o aggiorna nota a seconda della modalità.
   * Gestisce anche i permessi se permesso all'utente.
   */
  onSubmit(): void {
    if (this.noteForm.valid) {
      console.log(' Submitting form...');

      const formData = {
        titolo: this.noteForm.value.titolo,
        contenuto: this.noteForm.value.contenuto,
        tags: this.selectedTags(),
        cartelle: this.selectedCartelle()
      };

      if (this.isEditMode()) {
        // Modifica nota esistente
        if (this.canEditPermissions()) {
          // Se può modificare i permessi, invia anche quelli
          const updateWithPermissions: UpdateNoteRequestWithPermissions = {
            id: this.note?.id || 0,
            ...formData,
            permessi: {
              tipoPermesso: this.permissionType(),
              utentiLettura: this.selectedUsersForReading(),
              utentiScrittura: this.selectedUsersForWriting()
            }
          };
          console.log(' Aggiornamento nota con permessi:', updateWithPermissions);
          this.save.emit(updateWithPermissions);
        } else {
          // Solo modifica contenuto
          const updateRequest: UpdateNoteRequest = {
            id: this.note?.id || 0,
            ...formData
          };
          console.log('Aggiornamento nota senza permessi:', updateRequest);
          this.save.emit(updateRequest);
        }
      } else {
        // Creazione nuova nota
        const createRequest: CreateNoteRequest = {
          ...formData,
          permessi: {
            tipoPermesso: this.permissionType(),
            utentiLettura: this.selectedUsersForReading(),
            utentiScrittura: this.selectedUsersForWriting()
          }
        };
        console.log(' Creazione nuova nota:', createRequest);
        this.save.emit(createRequest);
      }

      // IMPORTANTE: Rilascia il lock dopo aver salvato
      if (this.isEditMode()) {
        console.log(' Rilascio lock dopo salvataggio');
        this.releaseLock();
      }
    } else {
      console.log(' Form non valido:', this.noteForm.errors);
      // Marca tutti i campi come "touched" per mostrare gli errori
      Object.keys(this.noteForm.controls).forEach(key => {
        this.noteForm.get(key)?.markAsTouched();
      });
    }
  }

  /**
   * Gestisce la cancellazione/chiusura del form
   * Rilascia il lock e notifica il genitore
   */
  onCancel(): void {
    console.log(' Cancellazione form - rilascio lock');
    this.releaseLock(); // Rilascia lock quando chiudi
    this.cancel.emit();
  }

  // Metodi di utilità per il template
  isFieldInvalid(fieldName: string): boolean {
    const field = this.noteForm.get(fieldName);
    return !!(field && field.invalid && field.touched);
  }

  getFieldError(fieldName: string): string {
    const field = this.noteForm.get(fieldName);
    if (field && field.errors && field.touched) {
      if (field.errors['required']) {
        return `${fieldName} è obbligatorio`;
      }
      if (field.errors['maxlength']) {
        return `${fieldName} troppo lungo`;
      }
    }
    return '';
  }

  // Proprietà getter per messaggi e stati nel template
  get isCharacterLimitWarning(): boolean {
    return this.characterCount() > this.maxCharacters * 0.8;
  }

  get isCharacterLimitExceeded(): boolean {
    return this.characterCount() > this.maxCharacters;
  }

  get lockStatusMessage(): string {
    if (this.lockError()) {
      return this.lockError()!;
    }
    if (this.isNoteLocked()) {
      return `Nota bloccata per modifica`;
    }
    return '';
  }

  get showLockStatus(): boolean {
    return this.isEditMode() && (this.isNoteLocked() || !!this.lockError());
  }

  onUserToggle(username: string): void {
    const permissionType = this.permissionType();

    if (permissionType === 'CONDIVISA_LETTURA') {
      const current = this.selectedUsersForReading();
      if (current.includes(username)) {
        this.removeUserFromReading(username);
      } else {
        this.addUserToReading(username);
      }
    } else if (permissionType === 'CONDIVISA_SCRITTURA') {
      const current = this.selectedUsersForWriting();
      if (current.includes(username)) {
        this.removeUserFromWriting(username);
      } else {
        this.addUserToWriting(username);
      }
    }
  }

  isUserSelected(username: string): boolean {
    const permissionType = this.permissionType();

    if (permissionType === 'CONDIVISA_LETTURA') {
      return this.selectedUsersForReading().includes(username);
    } else if (permissionType === 'CONDIVISA_SCRITTURA') {
      return this.selectedUsersForWriting().includes(username);
    }

    return false;
  }

  // Gestione input tag
  onTagInputChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.tagInputValue.set(target.value);
  }

  onTagInputKeyup(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      event.preventDefault();
      this.addTag();
    }
  }

  canAddTag(): boolean {
    return this.tagInputValue().trim().length > 0 &&
      !this.selectedTags().includes(this.tagInputValue().trim());
  }

  shouldShowTagSuggestions(): boolean {
    return this.tagInputValue().length > 0 && this.getFilteredTagSuggestions().length > 0;
  }

  getFilteredTagSuggestions(): string[] {
    const input = this.tagInputValue().toLowerCase();
    const selectedTags = this.selectedTags();

    return this.allTags
      .filter(tag => tag.toLowerCase().includes(input) && !selectedTags.includes(tag))
      .slice(0, 5); // Mostra max 5 suggerimenti
  }

  addTagFromSuggestion(tag: string): void {
    this.selectedTags.set([...this.selectedTags(), tag]);
    this.tagInputValue.set('');
  }

  onCartellaToggle(cartellaNome: string): void {
    const current = this.selectedCartelle();
    if (current.includes(cartellaNome)) {
      this.removeCartella(cartellaNome);
    } else {
      this.selectCartella(cartellaNome);
    }
  }
}
