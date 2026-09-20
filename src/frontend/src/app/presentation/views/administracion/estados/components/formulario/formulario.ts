import { Component, OnInit, input, output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { EstadoRequest, EstadoResponse, esEstadoCritico } from '../../../../../../core/models/estado.model';
import { Button } from '../../../../../../shared/components/button/button';
import { Input } from '../../../../../../shared/components/input/input';

interface EstadoForm {
  nombre: FormControl<string>;
}

@Component({
  selector: 'app-estado-formulario',
  imports: [ReactiveFormsModule, Button, Input],
  templateUrl: './formulario.html',
  styleUrl: './formulario.css',
})
export class Formulario implements OnInit {
  readonly mode = input.required<'create' | 'edit'>();
  readonly initial = input<EstadoResponse | null>(null);
  readonly submitting = input(false);

  readonly submitForm = output<EstadoRequest>();
  readonly cancel = output<void>();

  protected readonly form = new FormGroup<EstadoForm>({
    nombre: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(100)] }),
  });

  ngOnInit(): void {
    const estado = this.initial();
    if (this.mode() === 'edit' && estado) {
      this.form.patchValue({ nombre: estado.nombre });
    }
  }

  protected get esCritico(): boolean {
    return this.mode() === 'edit' && !!this.initial() && esEstadoCritico(this.initial()!.nombre);
  }

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitForm.emit({ nombre: this.form.getRawValue().nombre });
  }
}
