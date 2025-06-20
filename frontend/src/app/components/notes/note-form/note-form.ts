import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Note, CreateNoteRequest, UpdateNoteRequest } from '../../../models/note.model';
import { CartelleService } from '../../../services/cartelle';

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
  
  // Cartelle disponibili dal servizio
  availableCartelle = computed(() => this.cartelleService.cartelle());

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
    }
  }

  private resetForm(): void {
    this.noteForm.reset();
    this.selectedTags.set([]);
    this.selectedCartelle.set([]);
    this.characterCount.set(0);
    this.tagInputValue.set('');
    this.showCartelleDropdown.set(false);
  }

  // Helper methods per template (per evitare errori di parsing)
  getCharacterWidth(): number {
    return (this.characterCount() / this.maxCharacters) * 100;
  }

  getCharacterDisplay(): string {
    return `${this.characterCount()}/${this.maxCharacters}`;
  }

  getTagInputValue(): string {
    return this.tagInputValue();
  }

  getSelectedTags(): string[] {
    return this.selectedTags();
  }

  getSelectedCartelle(): string[] {
    return this.selectedCartelle();
  }

  hasSelectedTags(): boolean {
    return this.selectedTags().length > 0;
  }

  hasSelectedCartelle(): boolean {
    return this.selectedCartelle().length > 0;
  }

  // Cartelle dropdown methods
  toggleCartelleDropdown(): void {
    this.showCartelleDropdown.update(show => !show);
  }

  selectCartella(cartellaId: number, cartellaNome: string): void {
    if (!this.selectedCartelle().includes(cartellaNome)) {
      this.selectedCartelle.update(cartelle => [...cartelle, cartellaNome]);
    }
    this.showCartelleDropdown.set(false);
  }

  removeCartella(cartella: string): void {
    this.selectedCartelle.update(cartelle => cartelle.filter(c => c !== cartella));
  }

  getUnselectedCartelle() {
    const selected = this.selectedCartelle();
    return this.availableCartelle().filter(cartella => !selected.includes(cartella.nome));
  }

  // Tag methods
  onTagInputChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.tagInputValue.set(target.value);
  }

  onTagInputKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter' || event.key === ',') {
      event.preventDefault();
      this.addTag();
    }
    // Aggiunge supporto per backspace quando input è vuoto
    if (event.key === 'Backspace' && !this.tagInputValue() && this.selectedTags().length > 0) {
      const tags = this.selectedTags();
      this.selectedTags.set(tags.slice(0, -1));
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

  // Form submission
  onSubmit(): void {
    if (this.noteForm.valid) {
      const formValue = this.noteForm.value;
      const noteData = {
        titolo: formValue.titolo.trim(),
        contenuto: formValue.contenuto.trim(),
        tags: this.selectedTags(),
        cartelle: this.selectedCartelle()
      };

      this.save.emit(noteData);
    }
  }

  onCancel(): void {
    this.cancel.emit();
  }
}