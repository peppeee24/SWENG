export interface User {
  id: number;
  username: string;
  nome?: string;
  cognome?: string;
  email?: string;
  sesso?: string;
  numeroTelefono?: string;
  citta?: string;
  dataNascita?: string;
  createdAt?: string;
}

export interface UserDisplayName {
  id: number;
  username: string;
  displayName: string;
}
