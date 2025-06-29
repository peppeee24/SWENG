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
}

export interface UserStats {
  noteCreate: number;
  noteCondivise: number;
  tagUtilizzati: number;
  cartelleCreate: number;
  allTags: string[];
  allCartelle: string[];
}

export interface PermissionsRequest {
  tipoPermesso: 'PRIVATA' | 'CONDIVISA_LETTURA' | 'CONDIVISA_SCRITTURA';
  utentiLettura?: string[];
  utentiScrittura?: string[];
}

