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
  tags?: string[];
  cartelle?: string[];
  permessi?: Permission;
}

export interface UpdateNoteRequest {
  id: number;
  titolo: string;
  contenuto: string;
  tags?: string[];
  cartelle?: string[];
}

export interface UpdateNoteRequestWithPermissions extends UpdateNoteRequest {
  permessi?: Permission;
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
  note?: Note;
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

export interface UserStats {
  noteCreate: number;
  noteCondivise: number;
  tagUtilizzati: number;
  cartelleCreate: number;
  allTags: string[];
  allCartelle: string[];
}
