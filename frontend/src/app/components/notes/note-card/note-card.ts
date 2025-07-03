// note-card.component.ts
import { Component, Input, Output, EventEmitter, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Note } from '../../../models/note.model';
import { NoteVersionHistoryComponent } from '../note-version-history/note-version-history.component';

@Component({
  selector: 'app-note-card',
  standalone: true,
  imports: [CommonModule, NoteVersionHistoryComponent],
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
  @Output() restoreVersion = new EventEmitter<{ noteId: number, versionNumber: number }>();


  isVersionHistoryVisible = signal(false);

  // Computed properties
  isOwner = computed(() => this.note?.autore === this.currentUsername);

  // Determina se l'utente è un invitato (ha accesso ma non è proprietario)
  isSharedUser = computed(() => !this.isOwner() && this.note?.autore !== this.currentUsername);


  /*
  canViewHistory(): boolean {
    return this.note?.canEdit || this.note?.canDelete || false;
  }

  hasMultipleVersions(): boolean {
    return this.note?.versionNumber ? this.note.versionNumber > 1 : false;
  }

   */


  showVersionHistory(): boolean {
    const hasMultiple = this.hasMultipleVersions();
    const canView = this.canViewHistory();
    return hasMultiple && canView;
  }

  // Metodi per formattazione date
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

  // Metodi per permessi
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

  // Metodi per azioni
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

  shouldShowVersionHistory(): boolean {
    return this.hasMultipleVersions() && this.canViewHistory();
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


  onShowVersionHistory(): void {
    this.isVersionHistoryVisible.set(true);
  }

  onCloseVersionHistory(): void {
    this.isVersionHistoryVisible.set(false);
  }

  onRestoreVersion(event: { noteId: number, versionNumber: number }): void {
    this.restoreVersion.emit(event);
    this.isVersionHistoryVisible.set(false);
  }

  /*
  // Metodo per ottenere il testo del badge della versione
  getVersionBadgeText(): string {
    if (!this.note.versionNumber) return '';

    if (this.note.versionNumber === 1) {
      return 'v1';
    } else {
      return `v${this.note.versionNumber}`;
    }
  }

  // Metodo per determinare se mostrare l'indicatore di versioni multiple
  shouldShowVersionIndicator(): boolean {
    return this.hasMultipleVersions() && this.canViewHistory();
  }

   */


  canViewHistory(): boolean {
    return this.note?.canEdit || this.note?.canDelete || false;
  }

  hasMultipleVersions(): boolean {
    return this.note?.versionNumber ? this.note.versionNumber > 1 : false;
  }

  shouldShowVersionIndicator(): boolean {
    return this.note?.versionNumber ? this.note.versionNumber >= 1 : false;
  }

  getVersionBadgeText(): string {
    if (!this.note?.versionNumber) return 'v1';
    return `v${this.note.versionNumber}`;
  }

}
