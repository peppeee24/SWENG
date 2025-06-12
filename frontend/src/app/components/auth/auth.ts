
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService, RegisterRequest, LoginRequest } from '../../services/auth';

@Component({
  selector: 'app-auth',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './auth.html',
  styleUrls: ['./auth.css']
})
export class AuthComponent implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  
  isLoginMode = signal(true);
  isLoading = signal(false);
  errorMessage = signal('');
  successMessage = signal('');

  loginForm: FormGroup;
  registerForm: FormGroup;

  constructor() {
    this.loginForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });

    this.registerForm = this.fb.group({
      // Dati di accesso
      username: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]],
      
      // Dati personali
      nome: ['', [Validators.required, Validators.minLength(2)]],
      cognome: ['', [Validators.required, Validators.minLength(2)]],
      sesso: ['', [Validators.required]],
      dataNascita: ['', [Validators.required]],
      
      // Contatti
      telefono: ['', [Validators.required, Validators.pattern(/^[0-9+\-\s()]+$/)]],
      citta: ['', [Validators.required, Validators.minLength(2)]],
      
      // Privacy
      accettaTermini: [false, [Validators.requiredTrue]],
      accettaPrivacy: [false, [Validators.requiredTrue]]
    }, { validators: this.passwordMatchValidator });
  }

  ngOnInit(): void {
    // Se l'utente è già loggato, reindirizza alla home
    if (this.authService.isLoggedIn()) {
      this.router.navigate(['/dashboard']);
    }
  }

  passwordMatchValidator(form: FormGroup) {
    const password = form.get('password');
    const confirmPassword = form.get('confirmPassword');
    
    if (password && confirmPassword && password.value !== confirmPassword.value) {
      return { passwordMismatch: true };
    }
    return null;
  }

  switchMode(): void {
    this.isLoginMode.set(!this.isLoginMode());
    this.errorMessage.set('');
    this.successMessage.set('');
    this.resetForms();
  }

  resetForms(): void {
    this.loginForm.reset();
    this.registerForm.reset();
  }

  onLogin(): void {
    if (this.loginForm.valid) {
      this.isLoading.set(true);
      this.errorMessage.set('');

      const loginRequest: LoginRequest = {
        username: this.loginForm.value.username,
        password: this.loginForm.value.password
      };

      this.authService.login(loginRequest).subscribe({
        next: (response) => {
          this.isLoading.set(false);
          this.successMessage.set('Login effettuato con successo!');
          setTimeout(() => {
            this.router.navigate(['/dashboard']);
          }, 1000);
        },
        error: (error) => {
          this.isLoading.set(false);
          this.errorMessage.set(this.handleError(error));
        }
      });
    } else {
      this.markFormGroupTouched(this.loginForm);
    }
  }

  onRegister(): void {
    if (this.registerForm.valid) {
      this.isLoading.set(true);
      this.errorMessage.set('');

      const registerRequest: RegisterRequest = {
        username: this.registerForm.value.username,
        email: this.registerForm.value.email,
        password: this.registerForm.value.password,
        nome: this.registerForm.value.nome,
        cognome: this.registerForm.value.cognome,
        sesso: this.registerForm.value.sesso,
        telefono: this.registerForm.value.telefono,
        citta: this.registerForm.value.citta,
        dataNascita: this.registerForm.value.dataNascita
      };

      this.authService.register(registerRequest).subscribe({
        next: (response) => {
          this.isLoading.set(false);
          this.successMessage.set('Registrazione completata! Benvenuto in NOTA BENE!');
          setTimeout(() => {
            this.router.navigate(['/dashboard']);
          }, 1500);
        },
        error: (error) => {
          this.isLoading.set(false);
          this.errorMessage.set(this.handleError(error));
        }
      });
    } else {
      this.markFormGroupTouched(this.registerForm);
    }
  }

  private handleError(error: any): string {
    if (error.status === 400) {
      return 'Dati non validi. Controlla username e password.';
    } else if (error.status === 401) {
      return 'Credenziali non corrette.';
    } else if (error.status === 409) {
      return 'Username già esistente. Scegli un altro username.';
    } else if (error.status === 0) {
      return 'Impossibile connettersi al server. Verifica che il backend sia attivo.';
    }
    return 'Si è verificato un errore. Riprova più tardi.';
  }

  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();
    });
  }

  // Getters per validazione form
  get loginUsername() { return this.loginForm.get('username'); }
  get loginPassword() { return this.loginForm.get('password'); }
  
  // Getters per form registrazione
  get registerUsername() { return this.registerForm.get('username'); }
  get registerEmail() { return this.registerForm.get('email'); }
  get registerPassword() { return this.registerForm.get('password'); }
  get registerConfirmPassword() { return this.registerForm.get('confirmPassword'); }
  get registerNome() { return this.registerForm.get('nome'); }
  get registerCognome() { return this.registerForm.get('cognome'); }
  get registerSesso() { return this.registerForm.get('sesso'); }
  get registerDataNascita() { return this.registerForm.get('dataNascita'); }
  get registerTelefono() { return this.registerForm.get('telefono'); }
  get registerCitta() { return this.registerForm.get('citta'); }
  get registerAccettaTermini() { return this.registerForm.get('accettaTermini'); }
  get registerAccettaPrivacy() { return this.registerForm.get('accettaPrivacy'); }
}