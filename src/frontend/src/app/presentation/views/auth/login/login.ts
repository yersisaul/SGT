import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { AuthService } from '../../../../core/auth/auth.service';
import { APP_VERSION } from '../../../../core/config/app-version';
import { BrandLogo } from '../../../../shared/components/brand-logo/brand-logo';
import { Button } from '../../../../shared/components/button/button';
import { Card } from '../../../../shared/components/card/card';
import { Input } from '../../../../shared/components/input/input';

interface LoginForm {
  email: FormControl<string>;
  password: FormControl<string>;
}

interface LoginErrorBody {
  detail?: string;
  message?: string;
}

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, BrandLogo, Button, Card, Input],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly version = APP_VERSION;
  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = new FormGroup<LoginForm>({
    email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.errorMessage.set(null);
    this.submitting.set(true);

    const { email, password } = this.form.getRawValue();

    this.authService.login({ email, password }).subscribe({
      next: () => {
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') ?? '/app/dashboard';
        this.router.navigateByUrl(returnUrl);
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        this.errorMessage.set(this.extractMessage(error));
      },
    });
  }

  private extractMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const body = error.error as LoginErrorBody | null;
      if (body?.detail) return body.detail;
      if (body?.message) return body.message;
      if (error.status === 429) return 'Demasiados intentos. Espera un momento antes de volver a intentar.';
    }
    return 'No se pudo iniciar sesión. Inténtalo de nuevo.';
  }
}
