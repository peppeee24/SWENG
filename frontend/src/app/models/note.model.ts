// note.model.ts
/**
 * Modello dati di una nota nel sistema.
 */
export interface Note {
  id: number;
  titolo: string;
  contenuto: string;
  autore: string;
  dataCreazione: string;
  dataModifica: string;
  tags: string[];
  cartelle: string[];
  tipoPermesso: 'PRIVATA' | 'CONDIVISA_LETTURA' | 'CONDIVISA_SCRITTURA';
  permessiLettura: string[];
  permessiScrittura: string[];
  canEdit: boolean;
  canDelete: boolean;
  versionNumber?: number;
  isLockedForEditing?: boolean;
  lockedByUser?: string;
  lockExpiresAt?: string;
}

/**
 * Rappresenta i permessi di una nota.
 */
export interface Permission {
  tipoPermesso: 'PRIVATA' | 'CONDIVISA_LETTURA' | 'CONDIVISA_SCRITTURA';
  utentiLettura: string[];
  utentiScrittura: string[];
}


/**
 * Modello di richiesta per la creazione di una nuova nota.
 */
export interface CreateNoteRequest {
  titolo: string;
  contenuto: string;
  tags: string[];
  cartelle: string[];
  permessi: Permission;
}


/**
 * Modello di richiesta per l'aggiornamento di una nota esistente (senza permessi).
 */
export interface UpdateNoteRequest {
  id: number;
  titolo: string;
  contenuto: string;
  tags: string[];
  cartelle: string[];
}

/**
 * Modello di richiesta di aggiornamento di una nota comprensivo di permessi.
 */
export interface UpdateNoteRequestWithPermissions extends UpdateNoteRequest {
  permessi: Permission;
}

/**
 * Modello per aggiornare esclusivamente i permessi di una nota.
 */
export interface PermissionsRequest {
  tipoPermesso: 'PRIVATA' | 'CONDIVISA_LETTURA' | 'CONDIVISA_SCRITTURA';
  utentiLettura: string[];
  utentiScrittura: string[];
}

/**
 * Modello di richiesta per bloccare o sbloccare una nota.
 */
export interface NoteLockRequest {
  noteId: number;
  action: 'LOCK' | 'UNLOCK';
}

/**
 * Risposta del backend per operazioni di lock/unlock su una nota.
 */
export interface NoteLockResponse {
  success: boolean;
  locked: boolean;
  lockedByUser?: string;
  lockExpiresAt?: string;
  message: string;
}

/**
 * Risposta standard per operazioni su una singola nota.
 */
export interface NoteResponse {
  success: boolean;
  message: string;
  data?: Note;
  note?: Note; // Mantenuto per retrocompatibilità
}

/**
 * Risposta per liste di note.
 */
export interface NotesListResponse {
  success: boolean;
  notes: Note[];
  count: number;
  keyword?: string;
  tag?: string;
  cartella?: string;
  autore?: string;
  dataCreazione?: string;
  dataModifica?: string;
}

/**
 * Modello di una versione di una nota.
 */
export interface NoteVersion {
  id: number;
  versionNumber: number;
  titolo: string;
  contenuto: string;
  createdAt: string;
  createdBy: string;
  changeDescription: string;
}

/**
 * Statistiche aggregate per un utente.
 */
export interface UserStats {
  noteCreate: number;
  noteCondivise: number;
  tagUtilizzati: number;
  cartelleCreate: number;
  allTags: string[];
  allCartelle: string[];
}

/**
 * DTO per una versione di nota, usato per confronti e visualizzazione.
 */
export interface NoteVersionDto {
  id: number;
  versionNumber: number;
  contenuto: string;
  titolo: string;
  createdAt: string;
  createdBy: string;
  changeDescription?: string;
}

/**
 * Modello per richiedere il ripristino di una versione di una nota.
 */
export interface RestoreVersionRequest {
  versionNumber: number;
}

/**
 * DTO per il confronto tra due versioni di una nota.
 */
export interface VersionComparisonDto {
  version1: NoteVersionDto;
  version2: NoteVersionDto;
  version1Number: number;
  version2Number: number;
  differences: {
    titleChanged: boolean;
    contentChanged: boolean;
    titleDiff?: string;
    contentDiff?: string;
  };


}

/**
 * Filtro combinato per ricerca note.
 */
export interface SearchFilters {
  search?: string;
  tag?: string;
  cartella?: string;
  autore?: string;
  dataInizio?: string;
  dataFine?: string;
  filter?: 'all' | 'own' | 'shared';
}
