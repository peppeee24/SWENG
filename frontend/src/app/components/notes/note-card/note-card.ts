// note-card.component.ts
import { Component, Input, Output, EventEmitter, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Note } from '../../../models/note.model';
import { NoteVersionHistoryComponent } from '../note-version-history/note-version-history.component';

/**
 * Componente standalone per la visualizzazione di una scheda nota (Note Card).
 * Mostra informazioni sulla nota, permessi, versione e fornisce azioni 
 * come modifica, eliminazione, duplicazione, visualizzazione e gestione della cronologia versioni.
 */


@Component({
  selector: 'app-note-card',
  standalone: true,
  imports: [CommonModule, NoteVersionHistoryComponent],
  templateUrl: './note-card.html',
  styleUrls: ['./note-card.css']
})
export class NoteCardComponent {
  /** Nota da visualizzare */
  @Input() note!: Note;
  @Input() currentUsername: string = '';


  /** EventEmitter per comunicare azioni verso il genitore */
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

  /**
   * Controlla se mostrare la cronologia versioni basandosi su:
   * - La nota ha più versioni
   * - L'utente ha permessi per visualizzare la cronologia (edit/delete)
   */
  showVersionHistory(): boolean {
    const hasMultiple = this.hasMultipleVersions();
    const canView = this.canViewHistory();
    return hasMultiple && canView;
  }

  /**
   * Formatta una data ISO in stringa leggibile in formato italiano con giorno, mese, anno, ora e minuti.
   * @param dateString data ISO
   * @returns data formattata
   */
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

  /**
   * Calcola il tempo trascorso rispetto a "ora" e restituisce una stringa relativa:
   * es. "Ora", "15m fa", "3h fa", "5g fa" o data completa se oltre 7 giorni.
   * @param dateString data ISO
   * @returns stringa relativa
   */
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

  /**
   * Restituisce l'icona da mostrare in base al tipo di permesso della nota.
   */
  getPermissionIcon(): string {
    switch (this.note.tipoPermesso) {
      case 'PRIVATA': return '🔒';
      case 'CONDIVISA_LETTURA': return '👁️';
      case 'CONDIVISA_SCRITTURA': return '✏️';
      default: return '🔒';
    }
  }

  /**
   * Restituisce il testo descrittivo del tipo di permesso della nota.
   */
  getPermissionText(): string {
    switch (this.note.tipoPermesso) {
      case 'PRIVATA': return 'Privata';
      case 'CONDIVISA_LETTURA': return 'Condivisa (lettura)';
      case 'CONDIVISA_SCRITTURA': return 'Condivisa (scrittura)';
      default: return 'Privata';
    }
  }

  /** Azioni utente emesse agli eventi genitore */
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

  /**
   * Controlla se l'utente può vedere la cronologia versioni
   * in base ai permessi di modifica o eliminazione
   */
  canViewHistory(): boolean {
    return this.note?.canEdit || this.note?.canDelete || false;
  }

  
  /**
   * Controlla se la nota ha più versioni
   */
  hasMultipleVersions(): boolean {
    return this.note?.versionNumber ? this.note.versionNumber > 1 : false;
  }

  /**
   * Indica se mostrare l'indicatore badge versione anche se versione = 1
   */
  shouldShowVersionIndicator(): boolean {
    return this.note?.versionNumber ? this.note.versionNumber >= 1 : false;
  }

  /**
   * Testo del badge versione, ad es. "v1", "v2", ...
   */
  getVersionBadgeText(): string {
    if (!this.note?.versionNumber) return 'v1';
    return `v${this.note.versionNumber}`;
  }

}
