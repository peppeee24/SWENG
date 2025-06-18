import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService, RegisterRequest, LoginRequest } from '../../services/auth';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

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
  
  usernameChecking = signal(false);
  usernameAvailable = signal<boolean | null>(null);
  emailChecking = signal(false);
  emailAvailable = signal<boolean | null>(null);

  loginForm: FormGroup;
  registerForm: FormGroup;

  constructor() {
    this.loginForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });

    this.registerForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
      email: ['', [Validators.email, Validators.maxLength(150)]], 
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]],
      
      nome: ['', [Validators.maxLength(100)]],
      cognome: ['', [Validators.maxLength(100)]],
      sesso: [''],  
      dataNascita: [''],
      
      telefono: ['', [Validators.pattern(/^[\+]?[0-9\s\-\(\)]{8,20}$/)]],
      citta: ['', [Validators.maxLength(100)]],

      accettaTermini: [false, [Validators.requiredTrue]],
      accettaPrivacy: [false, [Validators.requiredTrue]]
    }, { validators: this.passwordMatchValidator });
  }

  ngOnInit(): void {
    // Se l'utente è già loggato, reindirizza alle note
    if (this.authService.isLoggedIn()) {
      console.log('Utente già autenticato, redirect alle note');
      this.router.navigate(['/notes']);
      return;
    }
    
    this.setupUsernameValidation();
    this.setupEmailValidation();
    this.testBackendConnection();
  }

  private testBackendConnection(): void {
    this.authService.healthCheck().subscribe({
      next: (response) => {
        console.log('Backend SWENG connesso:', response);
      },
      error: (error) => {
        console.error('Backend SWENG non raggiungibile:', error);
        this.errorMessage.set('Backend non raggiungibile. Verifica che sia avviato su http://localhost:8080');
      }
    });
  }

  private setupUsernameValidation(): void {
    this.registerForm.get('username')?.valueChanges
      .pipe(
        debounceTime(500),
        distinctUntilChanged()
      )
      .subscribe(username => {
        if (username && username.length >= 3 && this.registerForm.get('username')?.valid) {
          this.checkUsernameAvailability(username);
        } else {
          this.usernameAvailable.set(null);
        }
      });
  }

  private setupEmailValidation(): void {
    this.registerForm.get('email')?.valueChanges
      .pipe(
        debounceTime(500),
        distinctUntilChanged()
      )
      .subscribe(email => {
        if (email && email.length > 0 && this.registerForm.get('email')?.valid) {
          this.checkEmailAvailability(email);
        } else {
          this.emailAvailable.set(null);
        }
      });
  }

  private checkUsernameAvailability(username: string): void {
    this.usernameChecking.set(true);
    this.usernameAvailable.set(null);
    
    this.authService.checkUsernameAvailability(username).subscribe({
      next: (response) => {
        this.usernameAvailable.set(response.available);
        this.usernameChecking.set(false);
        console.log('Username check:', response);
      },
      error: (error) => {
        console.error('Errore verifica username:', error);
        this.usernameChecking.set(false);
        this.usernameAvailable.set(null);
      }
    });
  }

  private checkEmailAvailability(email: string): void {
    this.emailChecking.set(true);
    this.emailAvailable.set(null);
    
    this.authService.checkEmailAvailability(email).subscribe({
      next: (response) => {
        this.emailAvailable.set(response.available);
        this.emailChecking.set(false);
        console.log('Email check:', response);
      },
      error: (error) => {
        console.error('Errore verifica email:', error);
        this.emailChecking.set(false);
        this.emailAvailable.set(null);
      }
    });
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
    this.usernameAvailable.set(null);
    this.emailAvailable.set(null);
  }

  onLogin(): void {
    if (this.loginForm.valid) {
      this.isLoading.set(true);
      this.errorMessage.set('');
      this.successMessage.set('');

      const loginRequest: LoginRequest = {
        username: this.loginForm.value.username,
        password: this.loginForm.value.password
      };

      console.log('Tentativo login per:', loginRequest.username);

      this.authService.login(loginRequest).subscribe({
        next: (response) => {
          this.isLoading.set(false);
          if (response.success) {
            this.successMessage.set(`Bentornato ${response.user?.username || loginRequest.username}! Reindirizzamento...`);
            console.log('Login riuscito, redirect alle note:', response);
            
            setTimeout(() => {
              this.router.navigate(['/notes']);
            }, 1000);
          } else {
            this.errorMessage.set(response.message || 'Errore durante il login');
          }
        },
        error: (error) => {
          this.isLoading.set(false);
          this.errorMessage.set(error.message || 'Errore durante il login');
          console.error('Errore login:', error);
        }
      });
    } else {
      this.markFormGroupTouched(this.loginForm);
      this.errorMessage.set('Compila tutti i campi correttamente.');
    }
  }

  onRegister(): void {
    if (this.registerForm.valid && this.usernameAvailable() !== false && this.emailAvailable() !== false) {
      this.isLoading.set(true);
      this.errorMessage.set('');

      const formValue = this.registerForm.value;
      const registerRequest: RegisterRequest = {
        username: formValue.username,
        password: formValue.password,
        nome: formValue.nome || undefined,
        cognome: formValue.cognome || undefined,
        email: formValue.email || undefined,
        sesso: formValue.sesso || undefined,
        numeroTelefono: formValue.telefono || undefined,
        citta: formValue.citta || undefined,
        dataNascita: formValue.dataNascita || undefined
      };

      Object.keys(registerRequest).forEach(key => {
        if (registerRequest[key as keyof RegisterRequest] === undefined) {
          delete registerRequest[key as keyof RegisterRequest];
        }
      });

      console.log('Invio registrazione:', registerRequest);

      this.authService.register(registerRequest).subscribe({
        next: (response) => {
          this.isLoading.set(false);
          if (response.success) {
            this.successMessage.set(`Registrazione completata! Benvenuto ${response.username || formValue.username}! Ora puoi accedere.`);
            console.log('Registrazione riuscita:', response);
            
            setTimeout(() => {
              this.isLoginMode.set(true);
              this.successMessage.set('Ora effettua il login con le tue credenziali.');
              this.loginForm.patchValue({
                username: registerRequest.username
              });
            }, 2000);
          } else {
            this.errorMessage.set(response.message || 'Errore durante la registrazione');
          }
        },
        error: (error) => {
          this.isLoading.set(false);
          this.errorMessage.set(error.message || 'Errore durante la registrazione');
          console.error('Errore registrazione:', error);
        }
      });
    } else {
      this.markFormGroupTouched(this.registerForm);
      
      if (this.usernameAvailable() === false) {
        this.errorMessage.set('Username non disponibile. Scegline un altro.');
      } else if (this.emailAvailable() === false) {
        this.errorMessage.set('Email già in uso. Usa un\'altra email.');
      } else {
        this.errorMessage.set('Compila tutti i campi obbligatori correttamente.');
      }
    }
  }

  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();
    });
  }

  get loginUsername() { return this.loginForm.get('username'); }
  get loginPassword() { return this.loginForm.get('password'); }
  
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