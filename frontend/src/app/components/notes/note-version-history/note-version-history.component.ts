// note-version-history.component.ts - Versione completa con tutti i metodi richiesti

import { Component, OnInit, OnChanges, SimpleChanges, Input, Output, EventEmitter, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NotesService } from '../../../services/notes';
import { NoteVersionDto } from '../../../models/note.model';

@Component({
  selector: 'app-note-version-history',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './note-version-history.component.html',
  styleUrls: ['./note-version-history.component.css']
})
export class NoteVersionHistoryComponent implements OnInit, OnChanges {
  private notesService = inject(NotesService);

  @Input() noteId!: number;
  @Input() isVisible = false;
  @Output() close = new EventEmitter<void>();
  @Output() restoreVersion = new EventEmitter<{noteId: number, versionNumber: number}>();

  versions = signal<NoteVersionDto[]>([]);
  isLoading = signal(false);
  error = signal<string | null>(null);
  selectedVersion = signal<NoteVersionDto | null>(null);
  showComparison = signal(false);

  sortedVersions = computed(() => {
    const versionsArray = this.versions();

    console.log('Computed sortedVersions chiamato, versions:', versionsArray);
    console.log('Type of versions:', typeof versionsArray);
    console.log('Is array:', Array.isArray(versionsArray));

    if (!versionsArray) {
      console.log('Versions è null/undefined, restituisco array vuoto');
      return [];
    }

    if (!Array.isArray(versionsArray)) {
      console.error('Versions non è un array:', versionsArray);
      return [];
    }

    if (versionsArray.length === 0) {
      console.log('Array versions è vuoto');
      return [];
    }

    try {
      const sorted = versionsArray.sort((a, b) => {
        if (!a || !b || typeof a.versionNumber !== 'number' || typeof b.versionNumber !== 'number') {
          console.error('Oggetti versione non validi:', a, b);
          return 0;
        }
        return b.versionNumber - a.versionNumber;
      });

      console.log('Versions ordinate:', sorted);
      return sorted;
    } catch (error) {
      console.error('Errore durante il sort:', error);
      return [];
    }
  });

  uniqueContributorsCount = computed(() => {
    const versions = this.versions();
    if (!Array.isArray(versions)) return 0;

    const contributors = new Set(versions.map(v => v.createdBy));
    return contributors.size;
  });

  ngOnInit(): void {
    console.log('NoteVersionHistory ngOnInit - noteId:', this.noteId, 'isVisible:', this.isVisible);
    if (this.noteId && this.isVisible) {
      this.loadVersionHistory();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    console.log('NoteVersionHistory ngOnChanges:', changes);

    if (changes['noteId'] || changes['isVisible']) {
      const currentNoteId = changes['noteId']?.currentValue || this.noteId;
      const currentVisible = changes['isVisible']?.currentValue || this.isVisible;

      if (currentNoteId && currentVisible) {
        this.loadVersionHistory();
      }
    }
  }

  loadVersionHistory(): void {
    if (!this.noteId) {
      console.error('NoteId non fornito per il caricamento delle versioni');
      this.error.set('ID nota non valido');
      return;
    }

    console.log('Inizio caricamento cronologia versioni per nota:', this.noteId);

    this.isLoading.set(true);
    this.error.set(null);
    this.versions.set([]);
    this.selectedVersion.set(null);
    this.showComparison.set(false);

    this.notesService.getNoteVersionHistory(this.noteId).subscribe({
      next: (response: any) => {
        console.log('Risposta cronologia versioni ricevuta:', response);
        this.isLoading.set(false);

        let versionsData: NoteVersionDto[] = [];

        if (Array.isArray(response)) {
          versionsData = response;
          console.log('Risposta è array diretto');
        } else if (response && Array.isArray(response.versions)) {
          versionsData = response.versions;
          console.log('Risposta ha proprietà versions array');
        } else if (response && Array.isArray(response.data)) {
          versionsData = response.data;
          console.log('Risposta ha proprietà data array');
        } else if (response && Array.isArray(response.result)) {
          versionsData = response.result;
          console.log('Risposta ha proprietà result array');
        } else if (response && response.success === false) {
          console.log('Risposta indica errore:', response.message);
          this.error.set(response.message || 'Errore dal server');
          this.versions.set([]);
          return;
        } else {
          console.error('Formato risposta versioni non riconosciuto:', response);
          console.error('Tipo risposta:', typeof response);
          this.error.set('Formato dati versioni non valido');
          this.versions.set([]);
          return;
        }

        console.log('Versioni elaborate count:', versionsData.length);
        console.log('Primi 3 elementi:', versionsData.slice(0, 3));

        const validVersions = versionsData.filter(v => v && typeof v.versionNumber === 'number');
        if (validVersions.length !== versionsData.length) {
          console.warn('Alcune versioni hanno dati non validi. Filtrate da', versionsData.length, 'a', validVersions.length);
        }

        this.versions.set(validVersions);
      },
      error: (error: any) => {
        console.error('Errore caricamento cronologia:', error);
        this.isLoading.set(false);

        let errorMessage = 'Errore durante il caricamento delle versioni';
        if (error.status === 404) {
          errorMessage = 'Nessuna versione trovata per questa nota';
        } else if (error.message) {
          errorMessage = error.message;
        }

        this.error.set(errorMessage);
        this.versions.set([]);
      }
    });
  }

  onVersionSelect(version: NoteVersionDto): void {
    console.log('Versione selezionata:', version);
    this.selectedVersion.set(version);
    this.showComparison.set(true);
  }

  onRestoreVersion(versionOrNumber: NoteVersionDto | number): void {
    let versionNumber: number;

    if (typeof versionOrNumber === 'number') {
      versionNumber = versionOrNumber;
    } else {
      versionNumber = versionOrNumber.versionNumber;
    }

    const confirmed = confirm(`Sei sicuro di voler ripristinare la versione ${versionNumber}? Questo creerà una nuova versione della nota.`);

    if (confirmed) {
      console.log('Ripristino versione:', versionNumber, 'per nota:', this.noteId);
      this.restoreVersion.emit({
        noteId: this.noteId,
        versionNumber: versionNumber
      });
    }
  }

  getVersionBadgeClass(version: NoteVersionDto): string {
    const sorted = this.sortedVersions();
    if (sorted.length === 0) return 'version-badge';

    if (version.versionNumber === sorted[0].versionNumber) {
      return 'version-badge latest';
    }

    const selected = this.selectedVersion();
    if (selected && version.versionNumber === selected.versionNumber) {
      return 'version-badge selected';
    }

    return 'version-badge';
  }

  getVersionIcon(version: NoteVersionDto): string {
    const sorted = this.sortedVersions();
    if (sorted.length === 0) return '📄';

    if (version.versionNumber === sorted[0].versionNumber) {
      return '⭐';
    }

    return '📄';
  }



  getRelativeTime(dateString: string): string {
    if (!dateString) return 'Data sconosciuta';

    try {
      const date = new Date(dateString);
      const now = new Date();
      const diffMs = now.getTime() - date.getTime();
      const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
      const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
      const diffMinutes = Math.floor(diffMs / (1000 * 60));

      if (diffDays > 0) {
        return `${diffDays} giorni fa`;
      } else if (diffHours > 0) {
        return `${diffHours} ore fa`;
      } else if (diffMinutes > 0) {
        return `${diffMinutes} minuti fa`;
      } else {
        return 'Appena ora';
      }
    } catch (error) {
      console.error('Errore calcolo tempo relativo:', error);
      return 'Data non valida';
    }
  }

  onClose(): void {
    console.log('Chiusura cronologia versioni');
    this.selectedVersion.set(null);
    this.showComparison.set(false);
    this.close.emit();
  }

  formatDate(dateString: string): string {
    if (!dateString) return 'Data non disponibile';

    try {
      const date = new Date(dateString);
      return date.toLocaleString('it-IT', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (error) {
      console.error('Errore formattazione data:', error);
      return 'Data non valida';
    }
  }
}
