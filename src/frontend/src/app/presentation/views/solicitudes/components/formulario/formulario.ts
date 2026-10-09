import { Component, OnInit, effect, input, output, signal, untracked } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { ActivoCatalogo, EstadoCatalogo } from '../../../../../core/models/catalogo.model';
import { SolicitudRequest, SolicitudResponse } from '../../../../../core/models/solicitud.model';
import { Button } from '../../../../../shared/components/button/button';
import { FileUpload } from '../../../../../shared/components/file-upload/file-upload';
import { Select, SelectOption } from '../../../../../shared/components/select/select';
import { PRIORIDAD_OPTIONS } from '../../../../../shared/utils/prioridad-badge.util';
import { esEstadoEditablePorPut } from '../../solicitud-estados.config';

interface SolicitudForm {
  id_activo: FormControl<string>;
  id_especialidad: FormControl<string>;
  id_estado: FormControl<string>;
  prioridad: FormControl<string>;
  descripcion: FormControl<string>;
}

/** Lo que emite el formulario al guardar: los datos de Solicitud (aún con
 * id_usuario en blanco — lo completa el contenedor, ver Solicitudes.
 * handleFormSubmit) más, por separado, el adjunto nuevo o el pedido de
 * quitar el actual. */
export interface SolicitudFormSubmit {
  request: SolicitudRequest;
  archivo: File | null;
  eliminarArchivo: boolean;
}

/**
 * Formulario reactivo para crear/editar Solicitud (contrato real de
 * SolicitudRequest). id_usuario no aparece aquí: en creación se asigna el
 * usuario autenticado y en edición no es reasignable desde este formulario.
 *
 * id_especialidad tampoco se pide como select independiente: en la matriz de
 * permisos real (ver DataSeeder) los roles que crean/actualizan Solicitud
 * (Cliente, Despachador) no tienen especialidad.read, así que se deriva
 * automáticamente de la especialidad del Activo seleccionado (activo.read sí
 * lo tienen todos los roles que operan Solicitud).
 */
@Component({
  selector: 'app-solicitud-formulario',
  imports: [ReactiveFormsModule, Button, Select, FileUpload],
  templateUrl: './formulario.html',
  styleUrl: './formulario.css',
})
export class Formulario implements OnInit {
  readonly mode = input.required<'create' | 'edit'>();
  readonly initial = input<SolicitudResponse | null>(null);
  readonly activos = input<ActivoCatalogo[]>([]);
  readonly estados = input<EstadoCatalogo[]>([]);
  readonly submitting = input(false);

  readonly submitForm = output<SolicitudFormSubmit>();
  readonly cancel = output<void>();

  protected readonly prioridadOptions = PRIORIDAD_OPTIONS;

  protected readonly form = new FormGroup<SolicitudForm>({
    id_activo: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    id_especialidad: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    id_estado: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    prioridad: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
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
      const solicitudInicial = this.initial();
      if (this.mode() === 'edit' && solicitudInicial) {
        untracked(() => this.cargarGuardado(solicitudInicial));
      }
    });

    // El formulario se proyecta dentro de app-dialog (que solo oculta su
    // contenido con @if interno), así que esta instancia se crea una única
    // vez al montar la vista, antes de que resuelva el forkJoin de catálogos
    // del padre. Por eso el estado por defecto de creación NO puede fijarse
    // solo en ngOnInit (correría con estados() aún vacío): debe reaccionar
    // cuando el catálogo llegue.
    effect(() => {
      const estadosDisponibles = this.estados();
      if (this.mode() === 'create' && estadosDisponibles.length > 0 && !this.form.controls.id_estado.value) {
        const pendiente = estadosDisponibles.find((estado) => estado.nombre.toLowerCase().includes('pendiente'));
        this.form.controls.id_estado.setValue(pendiente?.id_estado ?? estadosDisponibles[0].id_estado);
      }
    });
  }

  ngOnInit(): void {
    this.form.controls.id_activo.valueChanges.subscribe((idActivo) => {
      const activoSeleccionado = this.activos().find((item) => item.id_activo === idActivo);
      if (activoSeleccionado) {
        this.form.controls.id_especialidad.setValue(activoSeleccionado.id_especialidad);
      }
    });
    this.form.valueChanges.subscribe(() => this.actualizarCambios());
    this.imagenControl.valueChanges.subscribe(() => this.actualizarCambios());
  }

  private cargarGuardado(solicitud: SolicitudResponse): void {
    this.form.patchValue(
      {
        id_activo: solicitud.id_activo,
        id_especialidad: solicitud.id_especialidad,
        id_estado: solicitud.id_estado,
        prioridad: solicitud.prioridad,
        descripcion: solicitud.descripcion,
      },
      { emitEvent: false },
    );
    this.imagenControl.setValue(null, { emitEvent: false });
    this.eliminarImagenActual = false;
    this.urlAdjuntoActual = solicitud.url_adjunto;
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

  protected get activoOptions(): SelectOption[] {
    return this.activos().map((activo) => ({ value: activo.id_activo, label: `${activo.codigo} · ${activo.nombre}` }));
  }

  // Solo las transiciones que el PUT genérico realmente permite
  // (SolicitudServiceImpl.validarTransicion): "Finalizado" (y cualquier otro
  // estado ajeno a Solicitud, como "Aprobado"/"Rechazado" de Requerimiento
  // si llegaran a estar en este catálogo compartido) queda fuera — se
  // alcanza únicamente vía generar-orden.
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
        id_activo: value.id_activo,
        id_estado: value.id_estado,
        id_especialidad: value.id_especialidad,
        prioridad: value.prioridad,
        descripcion: value.descripcion,
      },
      archivo: this.imagenControl.value,
      eliminarArchivo: this.eliminarImagenActual,
    });
  }
}
