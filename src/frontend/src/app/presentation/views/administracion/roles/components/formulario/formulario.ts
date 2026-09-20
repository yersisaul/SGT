import { Component, OnInit, input, output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { RolRequest, RolResponse } from '../../../../../../core/models/rol.model';
import { Button } from '../../../../../../shared/components/button/button';
import { Input } from '../../../../../../shared/components/input/input';

interface RolForm {
  nombre: FormControl<string>;
  descripcion: FormControl<string>;
}

@Component({
  selector: 'app-rol-formulario',
  imports: [ReactiveFormsModule, Button, Input],
  templateUrl: './formulario.html',
  styleUrl: './formulario.css',
})
export class Formulario implements OnInit {
  readonly mode = input.required<'create' | 'edit'>();
  readonly initial = input<RolResponse | null>(null);
  readonly submitting = input(false);

  readonly submitForm = output<RolRequest>();
  readonly cancel = output<void>();

  protected readonly form = new FormGroup<RolForm>({
    nombre: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(100)] }),
    descripcion: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(255)] }),
  });

  ngOnInit(): void {
    const rol = this.initial();
    if (this.mode() === 'edit' && rol) {
      this.form.patchValue({ nombre: rol.nombre, descripcion: rol.descripcion ?? '' });
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
