import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Note, CreateNoteRequest, UpdateNoteRequest } from '../../../models/note.model';

@Component({
  selector: 'app-note-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './note-form.html',
  styleUrls: ['./note-form.css']
})
export class NoteFormComponent implements OnInit, OnChanges {
  private fb = inject(FormBuilder);

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
  cartellaInputValue = signal('');
  
  selectedTags = signal<string[]>([]);
  selectedCartelle = signal<string[]>([]);

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
    this.cartellaInputValue.set('');
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

  getCartellaInputValue(): string {
    return this.cartellaInputValue();
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

  canAddTag(): boolean {
    return this.tagInputValue().trim().length > 0;
  }

  canAddCartella(): boolean {
    return this.cartellaInputValue().trim().length > 0;
  }

  shouldShowTagSuggestions(): boolean {
    return this.tagInputValue().length > 0 && this.getFilteredTagSuggestions().length > 0;
  }

  shouldShowCartelleSuggestions(): boolean {
    return this.cartellaInputValue().length > 0 && this.getFilteredCartelleSuggestions().length > 0;
  }

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

  removeTag(tag: string): void {
    this.selectedTags.update(tags => tags.filter(t => t !== tag));
  }

  addTagFromSuggestion(tag: string): void {
    if (!this.selectedTags().includes(tag)) {
      this.selectedTags.update(tags => [...tags, tag]);
      this.tagInputValue.set('');
    }
  }

  onCartellaInputChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.cartellaInputValue.set(target.value);
  }

  onCartellaInputKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter' || event.key === ',') {
      event.preventDefault();
      this.addCartella();
    }
    // Aggiunge supporto per backspace quando input è vuoto
    if (event.key === 'Backspace' && !this.cartellaInputValue() && this.selectedCartelle().length > 0) {
      const cartelle = this.selectedCartelle();
      this.selectedCartelle.set(cartelle.slice(0, -1));
    }
  }

  addCartella(): void {
    const cartellaValue = this.cartellaInputValue().trim();
    if (cartellaValue && !this.selectedCartelle().includes(cartellaValue) && cartellaValue.length <= 50) {
      this.selectedCartelle.update(cartelle => [...cartelle, cartellaValue]);
      this.cartellaInputValue.set('');
    }
  }

  removeCartella(cartella: string): void {
    this.selectedCartelle.update(cartelle => cartelle.filter(c => c !== cartella));
  }

  addCartellaFromSuggestion(cartella: string): void {
    if (!this.selectedCartelle().includes(cartella)) {
      this.selectedCartelle.update(cartelle => [...cartelle, cartella]);
      this.cartellaInputValue.set('');
    }
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

  getFilteredCartelleSuggestions(): string[] {
    const input = this.cartellaInputValue().toLowerCase();
    if (!input) return [];
    
    return this.allCartelle
      .filter(cartella => 
        cartella.toLowerCase().includes(input) && 
        !this.selectedCartelle().includes(cartella)
      )
      .slice(0, 5);
  }

  getCharacterProgressClass(): string {
    const count = this.characterCount();
    const percentage = (count / this.maxCharacters) * 100;
    
    if (percentage >= 90) return 'danger';
    if (percentage >= 80) return 'warning';
    return 'normal';
  }

  onSubmit(): void {
    if (this.noteForm.valid) {
      const formValue = this.noteForm.value;
      
      const noteData = {
        titolo: formValue.titolo.trim(),
        contenuto: formValue.contenuto.trim(),
        tags: this.selectedTags(),
        cartelle: this.selectedCartelle()
      };

      // Se è in modalità edit, aggiungi l'ID
      if (this.isEditMode() && this.note) {
        const updateData: UpdateNoteRequest = {
          id: this.note.id,
          ...noteData
        };
        this.save.emit(updateData);
      } else {
        this.save.emit(noteData as CreateNoteRequest);
      }
    } else {
      this.markFormGroupTouched();
    }
  }

  onCancel(): void {
    this.resetForm();
    this.cancel.emit();
  }

  private markFormGroupTouched(): void {
    Object.keys(this.noteForm.controls).forEach(key => {
      const control = this.noteForm.get(key);
      control?.markAsTouched();
    });
  }

  // Getters per template
  get titolo() { return this.noteForm.get('titolo'); }
  get contenuto() { return this.noteForm.get('contenuto'); }
}