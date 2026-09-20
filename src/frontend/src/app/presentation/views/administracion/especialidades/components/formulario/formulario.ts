import { Component, OnInit, input, output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { EspecialidadRequest, EspecialidadResponse } from '../../../../../../core/models/especialidad.model';
import { Button } from '../../../../../../shared/components/button/button';
import { Input } from '../../../../../../shared/components/input/input';

interface EspecialidadForm {
  nombre: FormControl<string>;
  descripcion: FormControl<string>;
}

@Component({
  selector: 'app-especialidad-formulario',
  imports: [ReactiveFormsModule, Button, Input],
  templateUrl: './formulario.html',
  styleUrl: './formulario.css',
})
export class Formulario implements OnInit {
  readonly mode = input.required<'create' | 'edit'>();
  readonly initial = input<EspecialidadResponse | null>(null);
  readonly submitting = input(false);

  readonly submitForm = output<EspecialidadRequest>();
  readonly cancel = output<void>();

  protected readonly form = new FormGroup<EspecialidadForm>({
    nombre: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(100)] }),
    descripcion: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(255)] }),
  });

  ngOnInit(): void {
    const especialidad = this.initial();
    if (this.mode() === 'edit' && especialidad) {
      this.form.patchValue({ nombre: especialidad.nombre, descripcion: especialidad.descripcion ?? '' });
    }
  }

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.submitForm.emit({ nombre: value.nombre, descripcion: value.descripcion });
  }
}
