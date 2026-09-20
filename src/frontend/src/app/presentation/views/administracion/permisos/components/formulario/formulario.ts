import { Component, OnInit, input, output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { PermisoRequest, PermisoResponse } from '../../../../../../core/models/permiso.model';
import { Button } from '../../../../../../shared/components/button/button';
import { Input } from '../../../../../../shared/components/input/input';

interface PermisoForm {
  codigo: FormControl<string>;
  descripcion: FormControl<string>;
}

/** codigo sigue la nomenclatura real <recurso>.<accion> (ver PermisoController/CLAUDE.md 5.2). */
const CODIGO_PATTERN = /^[a-z_]+\.[a-z_]+$/;

@Component({
  selector: 'app-permiso-formulario',
  imports: [ReactiveFormsModule, Button, Input],
  templateUrl: './formulario.html',
  styleUrl: './formulario.css',
})
export class Formulario implements OnInit {
  readonly mode = input.required<'create' | 'edit'>();
  readonly initial = input<PermisoResponse | null>(null);
  readonly submitting = input(false);

  readonly submitForm = output<PermisoRequest>();
  readonly cancel = output<void>();

  protected readonly form = new FormGroup<PermisoForm>({
    codigo: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(CODIGO_PATTERN)],
    }),
    descripcion: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(255)] }),
  });

  ngOnInit(): void {
    const permiso = this.initial();
    if (this.mode() === 'edit' && permiso) {
      this.form.patchValue({ codigo: permiso.codigo, descripcion: permiso.descripcion ?? '' });
    }
  }

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.submitForm.emit({ codigo: value.codigo, descripcion: value.descripcion });
  }
}
