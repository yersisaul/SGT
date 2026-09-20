import { Component, OnInit, input, output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { ActivoRequest, ActivoResponse } from '../../../../../../core/models/activo.model';
import { EspecialidadCatalogo } from '../../../../../../core/models/catalogo.model';
import { Button } from '../../../../../../shared/components/button/button';
import { Input } from '../../../../../../shared/components/input/input';
import { Select, SelectOption } from '../../../../../../shared/components/select/select';

interface ActivoForm {
  id_especialidad: FormControl<string>;
  codigo: FormControl<string>;
  nombre: FormControl<string>;
  descripcion: FormControl<string>;
  ubicacion: FormControl<string>;
  url_img: FormControl<string>;
}

const URL_PATTERN = /^https?:\/\/.+/i;

@Component({
  selector: 'app-activo-formulario',
  imports: [ReactiveFormsModule, Button, Input, Select],
  templateUrl: './formulario.html',
  styleUrl: './formulario.css',
})
export class Formulario implements OnInit {
  readonly mode = input.required<'create' | 'edit'>();
  readonly initial = input<ActivoResponse | null>(null);
  readonly especialidades = input<EspecialidadCatalogo[]>([]);
  readonly submitting = input(false);

  readonly submitForm = output<ActivoRequest>();
  readonly cancel = output<void>();

  protected readonly form = new FormGroup<ActivoForm>({
    id_especialidad: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    codigo: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(50)] }),
    nombre: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(150)] }),
    descripcion: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(255)] }),
    ubicacion: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(150)] }),
    url_img: new FormControl('', { nonNullable: true, validators: [Validators.pattern(URL_PATTERN)] }),
  });

  ngOnInit(): void {
    const activo = this.initial();
    if (this.mode() === 'edit' && activo) {
      this.form.patchValue({
        id_especialidad: activo.id_especialidad,
        codigo: activo.codigo,
        nombre: activo.nombre,
        descripcion: activo.descripcion ?? '',
        ubicacion: activo.ubicacion ?? '',
        url_img: activo.url_img ?? '',
      });
    }
  }

  protected get especialidadOptions(): SelectOption[] {
    return this.especialidades().map((item) => ({ value: item.id_especialidad, label: item.nombre }));
  }

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.submitForm.emit({
      id_especialidad: value.id_especialidad,
      codigo: value.codigo,
      nombre: value.nombre,
      descripcion: value.descripcion || null,
      ubicacion: value.ubicacion || null,
      url_img: value.url_img || null,
    });
  }
}
