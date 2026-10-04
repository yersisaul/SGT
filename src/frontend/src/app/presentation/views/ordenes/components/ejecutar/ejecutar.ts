import { Component, OnInit, input, output } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { OrdenResponse } from '../../../../../core/models/orden.model';
import { Button } from '../../../../../shared/components/button/button';
import { FileUpload } from '../../../../../shared/components/file-upload/file-upload';

export interface EjecutarResultado {
  archivo: File | null;
  eliminarArchivo: boolean;
}

/**
 * Informe técnico / entregable de la Orden. Ya no cambia el estado: la OT
 * avanza solo con las operaciones de negocio (tomar, asignar, verificar,
 * reasignar, cerrar — PRD E3). El archivo se sube vía ArchivoService al
 * fileserver propio del backend (ver Ordenes.handleEjecutar).
 */
@Component({
  selector: 'app-orden-ejecutar',
  imports: [ReactiveFormsModule, Button, FileUpload],
  templateUrl: './ejecutar.html',
  styleUrl: './ejecutar.css',
})
export class Ejecutar implements OnInit {
  readonly initial = input.required<OrdenResponse>();
  readonly submitting = input(false);

  readonly submitForm = output<EjecutarResultado>();

  protected readonly imagenControl = new FormControl<File | null>(null);
  protected eliminarImagenActual = false;
  protected urlAdjuntoActual: string | null = null;

  ngOnInit(): void {
    this.urlAdjuntoActual = this.initial().url_adjunto;
  }

  protected onEliminarImagenActual(): void {
    this.eliminarImagenActual = true;
  }

  protected submit(): void {
    if (this.submitting()) return;
    this.submitForm.emit({
      archivo: this.imagenControl.value,
      eliminarArchivo: this.eliminarImagenActual,
    });
  }
}
