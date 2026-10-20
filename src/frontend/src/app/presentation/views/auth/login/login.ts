import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { AuthService } from '../../../../core/auth/auth.service';
import { APP_VERSION } from '../../../../core/config/app-version';
import { BrandLogo } from '../../../../shared/components/brand-logo/brand-logo';
import { Button } from '../../../../shared/components/button/button';
import { Card } from '../../../../shared/components/card/card';
import { Input } from '../../../../shared/components/input/input';
import { LucideGlobe } from '../../../../shared/icons/lucide-icons';

interface LoginForm {
  email: FormControl<string>;
  password: FormControl<string>;
}

/**
 * El mismo login sirve al SGT y al panel del sitio web (route data
 * `contexto: 'web'`): cambian los textos, el destino y, en el panel, se
 * exige algún permiso web_*.read antes de dejar la sesión abierta.
 */
interface ContextoLogin {
  titulo: string;
  subtitulo: string;
  destino: string;
  pie: string;
}

const CONTEXTOS: Record<'sgt' | 'web', ContextoLogin> = {
  sgt: { titulo: 'Ingresar a SGT', subtitulo: 'Sistema de Gestión de Tickets', destino: '/app/dashboard', pie: 'SGT' },
  web: {
    titulo: 'Administración del sitio web',
    subtitulo: 'Contenido publicado en www.cfbd.co',
    destino: '/web',
    pie: 'Panel web · SGT',
  },
};

const SIN_ACCESO_WEB = 'Tu usuario no tiene permisos para administrar el sitio web.';

interface LoginErrorBody {
  detail?: string;
  message?: string;
}

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, BrandLogo, Button, Card, Input, LucideGlobe],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly version = APP_VERSION;
  protected readonly esWeb = this.route.snapshot.data['contexto'] === 'web';
  protected readonly contexto = CONTEXTOS[this.esWeb ? 'web' : 'sgt'];
  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(
    this.esWeb && this.route.snapshot.queryParamMap.has('sinAcceso') ? SIN_ACCESO_WEB : null,
  );

  constructor() {
    // Con una sesión válida no tiene sentido volver a pedir credenciales.
    if (this.authService.isAuthenticated() && (!this.esWeb || this.authService.puedeAdministrarWeb())) {
      this.router.navigateByUrl(this.contexto.destino);
    }
  }

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
        if (this.esWeb && !this.authService.puedeAdministrarWeb()) {
          this.authService.descartarSesion();
          this.submitting.set(false);
          this.errorMessage.set(SIN_ACCESO_WEB);
          return;
        }
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') ?? this.contexto.destino;
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
