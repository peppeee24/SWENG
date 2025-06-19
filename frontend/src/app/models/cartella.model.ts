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

export interface CreateCartellaRequest {
  nome: string;
  descrizione?: string;
  colore?: string;
}

export interface UpdateCartellaRequest {
  nome: string;
  descrizione?: string;
  colore?: string;
}

export interface CartellaResponse {
  success: boolean;
  message: string;
  cartella?: Cartella;
}

export interface CartelleListResponse {
  success: boolean;
  cartelle: Cartella[];
  count: number;
}

export interface CartelleStats {
  numeroCartelle: number;
  nomiCartelle: string[];
}