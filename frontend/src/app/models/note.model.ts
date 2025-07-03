// note.model.ts

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

export interface Permission {
  tipoPermesso: 'PRIVATA' | 'CONDIVISA_LETTURA' | 'CONDIVISA_SCRITTURA';
  utentiLettura: string[];
  utentiScrittura: string[];
}

export interface CreateNoteRequest {
  titolo: string;
  contenuto: string;
  tags: string[];
  cartelle: string[];
  permessi: Permission;
}

export interface UpdateNoteRequest {
  id: number;
  titolo: string;
  contenuto: string;
  tags: string[];
  cartelle: string[];
}

export interface UpdateNoteRequestWithPermissions extends UpdateNoteRequest {
  permessi: Permission;
}

export interface PermissionsRequest {
  tipoPermesso: 'PRIVATA' | 'CONDIVISA_LETTURA' | 'CONDIVISA_SCRITTURA';
  utentiLettura: string[];
  utentiScrittura: string[];
}

export interface NoteLockRequest {
  noteId: number;
  action: 'LOCK' | 'UNLOCK';
}

export interface NoteLockResponse {
  success: boolean;
  locked: boolean;
  lockedByUser?: string;
  lockExpiresAt?: string;
  message: string;
}

export interface NoteResponse {
  success: boolean;
  message: string;
  data?: Note;
  note?: Note; // Mantenuto per retrocompatibilità
}

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

export interface NoteVersion {
  id: number;
  versionNumber: number;
  titolo: string;
  contenuto: string;
  createdAt: string;
  createdBy: string;
  changeDescription: string;
}

export interface UserStats {
  noteCreate: number;
  noteCondivise: number;
  tagUtilizzati: number;
  cartelleCreate: number;
  allTags: string[];
  allCartelle: string[];
}

export interface NoteVersionDto {
  id: number;
  versionNumber: number;
  contenuto: string;
  titolo: string;
  createdAt: string;
  createdBy: string;
  changeDescription?: string;
}

export interface RestoreVersionRequest {
  versionNumber: number;
}

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
