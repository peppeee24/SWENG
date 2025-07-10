/**
 * Definisce il modello dati per una cartella nel sistema di note.
 */
export interface Cartella {
  id: number;
  nome: string;
  descrizione?: string;
  proprietario: string;
  dataCreazione: string;
  dataModifica: string;
  colore: string;
  numeroNote: number;
}

/**
 * Modello per la richiesta di creazione di una nuova cartella.
 */
export interface CreateCartellaRequest {
  nome: string;
  descrizione?: string;
  colore?: string;
}

/**
 * Modello per la richiesta di aggiornamento di una cartella esistente.
 */
export interface UpdateCartellaRequest {
  nome: string;
  descrizione?: string;
  colore?: string;
}

/**
 * Modello di risposta restituito dalle API dopo operazioni su una singola cartella.
 */
export interface CartellaResponse {
  success: boolean;
  message: string;
  cartella?: Cartella;
}

/**
 * Modello di risposta per richieste che restituiscono una lista di cartelle.
 */
export interface CartelleListResponse {
  success: boolean;
  cartelle: Cartella[];
  count: number;
}

/**
 * Statistiche generali sulle cartelle di un utente o di un sistema.
 */
export interface CartelleStats {
  numeroCartelle: number;
  nomiCartelle: string[];
}