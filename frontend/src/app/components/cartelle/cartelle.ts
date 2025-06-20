import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { CartelleService } from '../../services/cartelle';
import { AuthService } from '../../services/auth';
import { Cartella, CreateCartellaRequest, UpdateCartellaRequest } from '../../models/cartella.model';

@Component({
  selector: 'app-cartelle',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './cartelle.html',
  styleUrls: ['./cartelle.css']
})
export class CartelleComponent implements OnInit {
  private cartelleService = inject(CartelleService);
  private authService = inject(AuthService);
  router = inject(Router);
  private fb = inject(FormBuilder);

  // Signals from services
  cartelle = computed(() => this.cartelleService.cartelle());
  isLoading = computed(() => this.cartelleService.isLoading());
  error = computed(() => this.cartelleService.error());
  currentUser = computed(() => this.authService.currentUser());

  // Local signals
  showCartellaForm = signal(false);
  selectedCartella = signal<Cartella | null>(null);
  cartellaForm: FormGroup;

  // Computed values
  displayName = computed(() => {
    const user = this.currentUser();
    if (!user) return 'Utente';
    
    if (user.nome && user.cognome) {
      return `${user.nome} ${user.cognome}`;
    } else if (user.nome) {
      return user.nome;
    } else {
      return user.username;
    }
  });

  currentUsername = computed(() => this.currentUser()?.username || '');

  isEditMode = computed(() => this.selectedCartella() !== null);

  constructor() {
    this.cartellaForm = this.fb.group({
      nome: ['', [Validators.required, Validators.minLength(1), Validators.maxLength(100)]],
      descrizione: ['', [Validators.maxLength(500)]],
      colore: ['#667eea', [Validators.pattern(/^#[0-9A-F]{6}$/i)]]
    });
  }

  ngOnInit(): void {
    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/auth']);
      return;
    }

    this.loadCartelle();
  }

  loadCartelle(): void {
    this.cartelleService.getAllCartelle().subscribe({
      next: () => {
        console.log('Cartelle caricate');
      },
      error: (error) => {
        console.error('Errore caricamento cartelle:', error);
      }
    });
  }

  showCreateForm(): void {
    this.selectedCartella.set(null);
    this.cartellaForm.reset({
      nome: '',
      descrizione: '',
      colore: '#667eea'
    });
    this.showCartellaForm.set(true);
  }

  showEditForm(cartella: Cartella): void {
    this.selectedCartella.set(cartella);
    this.cartellaForm.patchValue({
      nome: cartella.nome,
      descrizione: cartella.descrizione || '',
      colore: cartella.colore || '#667eea'
    });
    this.showCartellaForm.set(true);
  }

  hideCartellaForm(): void {
    this.showCartellaForm.set(false);
    this.selectedCartella.set(null);
    this.cartellaForm.reset();
  }

  onCartellaSave(): void {
    if (this.cartellaForm.valid) {
      const formData = this.cartellaForm.value;
      const selectedCartella = this.selectedCartella();
      
      if (selectedCartella) {
        // Update existing cartella
        const updateRequest: UpdateCartellaRequest = {
          nome: formData.nome.trim(),
          descrizione: formData.descrizione?.trim() || undefined,
          colore: formData.colore
        };

        this.cartelleService.updateCartella(selectedCartella.id, updateRequest).subscribe({
          next: (response) => {
            if (response.success) {
              this.hideCartellaForm();
            }
          },
          error: (error) => {
            console.error('Errore aggiornamento cartella:', error);
          }
        });
      } else {
        // Create new cartella
        const createRequest: CreateCartellaRequest = {
          nome: formData.nome.trim(),
          descrizione: formData.descrizione?.trim() || undefined,
          colore: formData.colore
        };

        this.cartelleService.createCartella(createRequest).subscribe({
          next: (response) => {
            if (response.success) {
              this.hideCartellaForm();
            }
          },
          error: (error) => {
            console.error('Errore creazione cartella:', error);
          }
        });
      }
    } else {
      this.markFormGroupTouched();
    }
  }

  onCartellaDelete(cartellaId: number): void {
    if (confirm('Sei sicuro di voler eliminare questa cartella? Assicurati che non contenga note.')) {
      this.cartelleService.deleteCartella(cartellaId).subscribe({
        next: (response) => {
          if (response.success) {
            console.log('Cartella eliminata con successo');
          }
        },
        error: (error) => {
          console.error('Errore eliminazione cartella:', error);
        }
      });
    }
  }

 viewCartellaNotes(cartella: Cartella): void {
  // Naviga alla nuova pagina dedicata per le note della cartella
  const encodedNome = encodeURIComponent(cartella.nome);
  this.router.navigate(['/cartelle', encodedNome, 'notes']);
}

  onLogout(): void {
    this.cartelleService.clearCartelle();
    this.authService.logout();
    this.router.navigate(['/auth']);
  }

  private markFormGroupTouched(): void {
    Object.keys(this.cartellaForm.controls).forEach(key => {
      const control = this.cartellaForm.get(key);
      control?.markAsTouched();
    });
  }

  // Getters per template
  get nome() { return this.cartellaForm.get('nome'); }
  get descrizione() { return this.cartellaForm.get('descrizione'); }
  get colore() { return this.cartellaForm.get('colore'); }
}