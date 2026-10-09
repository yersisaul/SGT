import { Component, OnInit, effect, input, output, signal, untracked } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { EspecialidadCatalogo, EstadoCatalogo } from '../../../../../core/models/catalogo.model';
import { RequerimientoRequest, RequerimientoResponse } from '../../../../../core/models/requerimiento.model';
import { Button } from '../../../../../shared/components/button/button';
import { FileUpload } from '../../../../../shared/components/file-upload/file-upload';
import { Select, SelectOption } from '../../../../../shared/components/select/select';
import { esEstadoEditablePorPut } from '../../requerimiento-estados.config';

interface RequerimientoForm {
  id_especialidad: FormControl<string>;
  id_estado: FormControl<string>;
  descripcion: FormControl<string>;
}

/** Lo que emite el formulario al guardar: los datos de Requerimiento (aún
 * con id_usuario en blanco — lo completa el contenedor) más, por separado,
 * el adjunto nuevo o el pedido de quitar el actual. */
export interface RequerimientoFormSubmit {
  request: RequerimientoRequest;
  archivo: File | null;
  eliminarArchivo: boolean;
}

/**
 * Formulario reactivo para crear/editar Requerimiento (contrato real de
 * RequerimientoRequest: id_usuario, id_estado, id_especialidad, descripcion).
 * Sin id_activo ni prioridad — Requerimiento no tiene esos campos. id_usuario
 * no aparece acá: se asigna el usuario autenticado en creación y no es
 * reasignable en edición (mismo criterio que Solicitud).
 */
@Component({
  selector: 'app-requerimiento-formulario',
  imports: [ReactiveFormsModule, Button, Select, FileUpload],
  templateUrl: './formulario.html',
  styleUrl: './formulario.css',
})
export class Formulario implements OnInit {
  readonly mode = input.required<'create' | 'edit'>();
  readonly initial = input<RequerimientoResponse | null>(null);
  readonly especialidades = input<EspecialidadCatalogo[]>([]);
  readonly estados = input<EstadoCatalogo[]>([]);
  readonly submitting = input(false);

  readonly submitForm = output<RequerimientoFormSubmit>();
  readonly cancel = output<void>();

  protected readonly form = new FormGroup<RequerimientoForm>({
    id_especialidad: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    id_estado: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    descripcion: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(1000)] }),
  });

  protected readonly imagenControl = new FormControl<File | null>(null);
  protected eliminarImagenActual = false;
  protected urlAdjuntoActual: string | null = null;

  /** En edición, "Guardar cambios" solo se habilita si algo difiere de lo guardado. */
  protected readonly hayCambios = signal(false);
  private valorGuardado = '';

  constructor() {
    // Edición: el formulario sigue montado tras guardar (la ficha no se
    // cierra), así que cada nuevo `initial` redefine la línea base.
    effect(() => {
      const requerimientoInicial = this.initial();
      if (this.mode() === 'edit' && requerimientoInicial) {
        untracked(() => this.cargarGuardado(requerimientoInicial));
      }
    });

    // El formulario se proyecta dentro de app-dialog (que solo oculta su
    // contenido con @if interno), así que esta instancia se crea una única
    // vez al montar la vista, antes de que resuelva el forkJoin de catálogos
    // del padre. Por eso el estado por defecto de creación NO puede fijarse
    // solo en ngOnInit (correría con estados() aún vacío): debe reaccionar
    // cuando el catálogo llegue.
    effect(() => {
      // El backend ignora el id_estado enviado al crear (siempre fuerza "En
      // revisión" — ver RequerimientoServiceImpl.crearRequerimiento); este
      // valor por defecto es solo para que el campo (oculto en modo create,
      // ver formulario.html) no quede vacío.
      const estadosDisponibles = this.estados();
      if (this.mode() === 'create' && estadosDisponibles.length > 0 && !this.form.controls.id_estado.value) {
        const enRevision = estadosDisponibles.find((estado) => estado.nombre.toLowerCase().includes('revisi'));
        this.form.controls.id_estado.setValue(enRevision?.id_estado ?? estadosDisponibles[0].id_estado);
      }
    });
  }

  ngOnInit(): void {
    this.form.valueChanges.subscribe(() => this.actualizarCambios());
    this.imagenControl.valueChanges.subscribe(() => this.actualizarCambios());
  }

  private cargarGuardado(requerimiento: RequerimientoResponse): void {
    this.form.patchValue(
      {
        id_especialidad: requerimiento.id_especialidad,
        id_estado: requerimiento.id_estado,
        descripcion: requerimiento.descripcion,
      },
      { emitEvent: false },
    );
    this.imagenControl.setValue(null, { emitEvent: false });
    this.eliminarImagenActual = false;
    this.urlAdjuntoActual = requerimiento.url_adjunto;
    this.valorGuardado = JSON.stringify(this.form.getRawValue());
    this.hayCambios.set(false);
  }

  private actualizarCambios(): void {
    this.hayCambios.set(
      JSON.stringify(this.form.getRawValue()) !== this.valorGuardado ||
        this.imagenControl.value !== null ||
        this.eliminarImagenActual,
    );
  }

  protected get especialidadOptions(): SelectOption[] {
    return this.especialidades().map((especialidad) => ({
      value: especialidad.id_especialidad,
      label: especialidad.nombre,
    }));
  }

  // Solo Pendiente/En revisión son editables por PUT (RequerimientoServiceImpl.
  // validarTransicion): Aprobado/Rechazado se alcanzan únicamente vía
  // AprobacionService, nunca desde este formulario.
  protected get estadoOptions(): SelectOption[] {
    return this.estados()
      .filter((estado) => esEstadoEditablePorPut(estado.nombre))
      .map((estado) => ({ value: estado.id_estado, label: estado.nombre }));
  }

  protected onEliminarImagenActual(): void {
    this.eliminarImagenActual = true;
    this.actualizarCambios();
  }

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    this.submitForm.emit({
      request: {
        id_usuario: '',
        id_estado: value.id_estado,
        id_especialidad: value.id_especialidad,
        descripcion: value.descripcion,
      },
      archivo: this.imagenControl.value,
      eliminarArchivo: this.eliminarImagenActual,
    });
  }
}
