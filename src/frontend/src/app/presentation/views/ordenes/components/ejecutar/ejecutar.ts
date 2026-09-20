import { Component, OnInit, input, output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { EstadoCatalogo } from '../../../../../core/models/catalogo.model';
import { OrdenResponse } from '../../../../../core/models/orden.model';
import { Button } from '../../../../../shared/components/button/button';
import { Input } from '../../../../../shared/components/input/input';
import { Select, SelectOption } from '../../../../../shared/components/select/select';
import { esEstadoEditablePorPut } from '../../orden-estados.config';

interface EjecutarForm {
  id_estado: FormControl<string>;
  url_adjunto: FormControl<string>;
}

const URL_PATTERN = /^https?:\/\/.+/i;

export interface EjecutarResultado {
  id_estado: string;
  url_adjunto: string | null;
}

/**
 * Formulario de ejecución de Orden — NO es un CRUD: solo expone id_estado
 * (transición validada) y url_adjunto, los dos únicos campos que
 * OrdenServiceImpl.editarOrden aplica realmente del PUT (id_usuario,
 * id_especialidad, id_solicitud e id_requerimiento se ignoran en backend;
 * no tiene sentido ofrecerlos como editables acá).
 */
@Component({
  selector: 'app-orden-ejecutar',
  imports: [ReactiveFormsModule, Button, Input, Select],
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
    url_adjunto: new FormControl('', { nonNullable: true, validators: [Validators.pattern(URL_PATTERN)] }),
  });

  ngOnInit(): void {
    const orden = this.initial();
    this.form.patchValue({
      id_estado: orden.id_estado,
      url_adjunto: orden.url_adjunto ?? '',
    });
  }

  // Solo Pendiente/En revisión/En progreso (OrdenServiceImpl.validarTransicion):
  // Finalizado se alcanza únicamente vía cerrarOrden, nunca desde acá.
  protected get estadoOptions(): SelectOption[] {
    return this.estados()
      .filter((estado) => esEstadoEditablePorPut(estado.nombre))
      .map((estado) => ({ value: estado.id_estado, label: estado.nombre }));
  }

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.submitForm.emit({ id_estado: value.id_estado, url_adjunto: value.url_adjunto || null });
  }
}
