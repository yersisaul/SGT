import { Component, OnInit, input, output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { ActivoRequest, ActivoResponse } from '../../../../../../core/models/activo.model';
import { EspecialidadCatalogo } from '../../../../../../core/models/catalogo.model';
import { Button } from '../../../../../../shared/components/button/button';
import { FileUpload } from '../../../../../../shared/components/file-upload/file-upload';
import { Input } from '../../../../../../shared/components/input/input';
import { Select, SelectOption } from '../../../../../../shared/components/select/select';

interface ActivoForm {
  id_especialidad: FormControl<string>;
  codigo: FormControl<string>;
  nombre: FormControl<string>;
  descripcion: FormControl<string>;
  ubicacion: FormControl<string>;
}

/** Lo que emite el formulario al guardar: los datos de Activo más, por
 * separado, la imagen nueva (si se seleccionó una) o el pedido de quitar la
 * actual. El contenedor decide cuándo llamar a ArchivoService (CLAUDE.md
 * sección 33: seleccionar -> preview -> guardar formulario -> subir). */
export interface ActivoFormSubmit {
  request: ActivoRequest;
  archivo: File | null;
  eliminarArchivo: boolean;
}

@Component({
  selector: 'app-activo-formulario',
  imports: [ReactiveFormsModule, Button, Input, Select, FileUpload],
  templateUrl: './formulario.html',
  styleUrl: './formulario.css',
})
export class Formulario implements OnInit {
  readonly mode = input.required<'create' | 'edit'>();
  readonly initial = input<ActivoResponse | null>(null);
  readonly especialidades = input<EspecialidadCatalogo[]>([]);
  readonly submitting = input(false);

  readonly submitForm = output<ActivoFormSubmit>();
  readonly cancel = output<void>();

  protected readonly form = new FormGroup<ActivoForm>({
    id_especialidad: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    codigo: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(50)] }),
    nombre: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(150)] }),
    descripcion: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(255)] }),
    ubicacion: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(150)] }),
  });

  protected readonly imagenControl = new FormControl<File | null>(null);
  protected eliminarImagenActual = false;
  protected urlImagenActual: string | null = null;

  ngOnInit(): void {
    const activo = this.initial();
    if (this.mode() === 'edit' && activo) {
      this.form.patchValue({
        id_especialidad: activo.id_especialidad,
        codigo: activo.codigo,
        nombre: activo.nombre,
        descripcion: activo.descripcion ?? '',
        ubicacion: activo.ubicacion ?? '',
      });
      this.urlImagenActual = activo.url_img;
    }
  }

  protected get especialidadOptions(): SelectOption[] {
    return this.especialidades().map((item) => ({ value: item.id_especialidad, label: item.nombre }));
  }

  protected onEliminarImagenActual(): void {
    this.eliminarImagenActual = true;
  }

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.submitForm.emit({
      request: {
        id_especialidad: value.id_especialidad,
        codigo: value.codigo,
        nombre: value.nombre,
        descripcion: value.descripcion || null,
        ubicacion: value.ubicacion || null,
      },
      archivo: this.imagenControl.value,
      eliminarArchivo: this.eliminarImagenActual,
    });
  }
}
