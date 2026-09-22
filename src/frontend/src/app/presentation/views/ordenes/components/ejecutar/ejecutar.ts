import { Component, OnInit, input, output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { EstadoCatalogo } from '../../../../../core/models/catalogo.model';
import { OrdenResponse } from '../../../../../core/models/orden.model';
import { Button } from '../../../../../shared/components/button/button';
import { FileUpload } from '../../../../../shared/components/file-upload/file-upload';
import { Select, SelectOption } from '../../../../../shared/components/select/select';
import { esEstadoEditablePorPut } from '../../orden-estados.config';

interface EjecutarForm {
  id_estado: FormControl<string>;
}

export interface EjecutarResultado {
  id_estado: string;
  archivo: File | null;
  eliminarArchivo: boolean;
}

/**
 * Formulario de ejecución de Orden — NO es un CRUD: solo expone id_estado
 * (transición validada, único campo que OrdenServiceImpl.editarOrden aplica
 * realmente del PUT genérico — CLAUDE.md sección 21) y, por separado, el
 * informe técnico/entregable, que se sube vía ArchivoService al fileserver
 * propio del backend (no es un campo del PUT: ver Ordenes.handleEjecutar).
 */
@Component({
  selector: 'app-orden-ejecutar',
  imports: [ReactiveFormsModule, Button, Select, FileUpload],
  templateUrl: './ejecutar.html',
  styleUrl: './ejecutar.css',
})
export class Ejecutar implements OnInit {
  readonly initial = input.required<OrdenResponse>();
  readonly estados = input<EstadoCatalogo[]>([]);
  readonly submitting = input(false);

  readonly submitForm = output<EjecutarResultado>();

  protected readonly form = new FormGroup<EjecutarForm>({
    id_estado: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  protected readonly imagenControl = new FormControl<File | null>(null);
  protected eliminarImagenActual = false;
  protected urlAdjuntoActual: string | null = null;

  ngOnInit(): void {
    const orden = this.initial();
    this.form.patchValue({ id_estado: orden.id_estado });
    this.urlAdjuntoActual = orden.url_adjunto;
  }

  // Solo Pendiente/En revisión/En progreso (OrdenServiceImpl.validarTransicion):
  // Finalizado se alcanza únicamente vía cerrarOrden, nunca desde acá.
  protected get estadoOptions(): SelectOption[] {
    return this.estados()
      .filter((estado) => esEstadoEditablePorPut(estado.nombre))
      .map((estado) => ({ value: estado.id_estado, label: estado.nombre }));
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
      id_estado: value.id_estado,
      archivo: this.imagenControl.value,
      eliminarArchivo: this.eliminarImagenActual,
    });
  }
}
