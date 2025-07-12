# Nota Bene 📝

Un'applicazione web moderna per la gestione di note testuali con funzionalità avanzate di organizzazione, condivisione e collaborazione.

## 🚀 Caratteristiche

- **Gestione Note Completa**: Creazione, modifica, duplicazione ed eliminazione di note
- **Ricerca Avanzata**: Trova rapidamente le tue note con potenti filtri
- **Condivisione e Collaborazione**: Gestione dei permessi per condividere note con altri utenti
- **Sicurezza**: Autenticazione JWT e gestione sicura degli accessi
- **Interfaccia Moderna**: UI intuitiva e responsive built con Angular

## 🛠️ Stack Tecnologico

### Backend
- **Java 17+** - Linguaggio di programmazione
- **Spring Boot** - Framework per applicazioni Java
- **Maven** - Gestione dipendenze e build
- **JWT (jsonwebtoken 0.11.5)** - Autenticazione basata su token

### Frontend
- **Angular** - Framework per applicazioni web
- **TypeScript** - Linguaggio di programmazione
- **Node.js 20.19.0** - Runtime JavaScript
- **Angular CLI** - Strumenti di sviluppo

### Database
- **PostgreSQL** - Database principale per produzione

### DevOps & Quality
- **Docker** - Containerizzazione dei servizi
- **Docker Compose** - Orchestrazione multi-container
- **GitHub Actions** - CI/CD pipeline
- **JUnit 5** - Framework di testing
- **Jacoco** - Coverage dei test
- **Checkstyle** - Controllo qualità del codice

## 📋 Requisiti di Sistema

### Software Necessario
- Java Development Kit (JDK) 17 o 21
- Node.js 20.19.0+
- npm (incluso con Node.js)
- PostgreSQL 13+
- Maven 3.8+
- Angular CLI

### Porte Utilizzate
- **Frontend Angular**: http://localhost:4200
- **Backend Spring Boot**: http://localhost:8080
- **Database PostgreSQL**: localhost:5432

## 🔧 Setup e Installazione

### 1. Clonazione del Repository
```bash
git clone https://github.com/peppeee24/SWENG.git
cd nota-bene
```

### 2. Setup del Database
```bash
# Accedere a PostgreSQL
psql -U postgres

# Creare il database
CREATE DATABASE sweng_db;

# Creare utente (se necessario)
CREATE USER postgres WITH PASSWORD 'root';
GRANT ALL PRIVILEGES ON DATABASE sweng_db TO postgres;

# Uscire da PostgreSQL
\q
```

### 3. Avvio dell'Ambiente di Sviluppo

#### Backend
```bash
cd backend/sweng
mvn clean compile
mvn spring-boot:run
```

#### Frontend
```bash
cd frontend/sweng-frontend
npm install
ng serve
```

### 4. Avvio con Docker (Alternativa)
```bash
# Build e avvio con Docker Compose
docker-compose up --build

# Solo avvio (se già buildato)
docker-compose up
```



## 📖 Documentazione

Questo ReadMe contiene un incipit del progetto, le informazioni COMPLETE sono disponibili nei seiguenti documenti allegati:

- **Manuale dello Sviluppatore**: Documentazione tecnica completa
- **Manuale Utente**: Istruzioni per l'avvio e l'esecuzione dell'applicazione per tutti gli utenti
- **Diario del Progetto**: Cronologia degli sprint e sviluppo
- **Documentazione Generale**: Fase di Inception - Diagramma di Dominio e  Diagramma dei Casid'Uso

## 📄 Licenza

Questo progetto è sviluppato per scopi educativi nel contesto del corso di Ingegneria del Software.

## 👥 Team di Sviluppo

- **Giuseppe Cozza**: 0001077442
- **Simone Magli**: 0001069295
- **Federico Sgambelluri**: 0001068826
- **Manuel Leotta**: 

## 🔗 Link Utili

- [Repository GitHub](https://github.com/peppeee24/SWENG)
- [Documentazione Spring Boot](https://spring.io/projects/spring-boot)
- [Documentazione Angular](https://angular.io/docs)
- [Docker Hub](https://hub.docker.com/)

---

**Nota Bene** - Il tuo compagno digitale per la gestione delle note! �
