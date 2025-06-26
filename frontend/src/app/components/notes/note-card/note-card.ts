import { Component, Input, Output, EventEmitter, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Note } from '../../../models/note.model';

@Component({
  selector: 'app-note-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './note-card.html',
  styleUrls: ['./note-card.css']
})
export class NoteCardComponent {
  @Input() note!: Note;
  @Input() currentUsername: string = '';

  @Output() edit = new EventEmitter<Note>();
  @Output() delete = new EventEmitter<number>();
  @Output() duplicate = new EventEmitter<number>();
  @Output() view = new EventEmitter<Note>();
  @Output() removeFromSharing = new EventEmitter<number>();

  isOwner = computed(() => this.note?.autore === this.currentUsername);

  // Determina se l'utente è un invitato (ha accesso ma non è proprietario)
  isSharedUser = computed(() => !this.isOwner() && this.note?.autore !== this.currentUsername);

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleString('it-IT', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  getRelativeTime(dateString: string): string {
    const date = new Date(dateString);
    const now = new Date();
    const diffInMinutes = Math.floor((now.getTime() - date.getTime()) / (1000 * 60));

    if (diffInMinutes < 1) return 'Ora';
    if (diffInMinutes < 60) return `${diffInMinutes}m fa`;

    const diffInHours = Math.floor(diffInMinutes / 60);
    if (diffInHours < 24) return `${diffInHours}h fa`;

    const diffInDays = Math.floor(diffInHours / 24);
    if (diffInDays < 7) return `${diffInDays}g fa`;

    return this.formatDate(dateString);
  }

  getPermissionIcon(): string {
    switch (this.note.tipoPermesso) {
      case 'PRIVATA': return '🔒';
      case 'CONDIVISA_LETTURA': return '👁️';
      case 'CONDIVISA_SCRITTURA': return '✏️';
      default: return '🔒';
    }
  }

  getPermissionText(): string {
    switch (this.note.tipoPermesso) {
      case 'PRIVATA': return 'Privata';
      case 'CONDIVISA_LETTURA': return 'Condivisa (lettura)';
      case 'CONDIVISA_SCRITTURA': return 'Condivisa (scrittura)';
      default: return 'Privata';
    }
  }

  onEdit(): void {
    if (this.note.canEdit) {
      this.edit.emit(this.note);
    }
  }

  onDelete(): void {
    if (this.note.canDelete && confirm('Sei sicuro di voler eliminare questa nota?')) {
      this.delete.emit(this.note.id);
    }
  }

  onDuplicate(): void {
    this.duplicate.emit(this.note.id);
  }

  onView(): void {
    this.view.emit(this.note);
  }

  onRemoveFromSharing(): void {
    if (confirm('Sei sicuro di volerti rimuovere dalla condivisione di questa nota?\n\nNon potrai più accedervi a meno che il proprietario non ti reinviti.')) {
      this.removeFromSharing.emit(this.note.id);
    }
  }
}
