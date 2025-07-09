/**
 * Rappresenta un utente nell'applicazione con i suoi dati principali.
 */
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

/**
 * Interfaccia utilizzata per mostrare un utente con il nome da visualizzare.
 * Utile per liste o dropdown dove serve un nome completo o rappresentativo.
 */
export interface UserDisplayName {
  id: number;
  username: string;
  displayName: string;
}
