import { Component, OnInit, input, output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { RolCatalogo } from '../../../../../../core/models/catalogo.model';
import { UsuarioRequest, UsuarioResponse } from '../../../../../../core/models/usuario.model';
import { Button } from '../../../../../../shared/components/button/button';
import { FileUpload } from '../../../../../../shared/components/file-upload/file-upload';
import { Input } from '../../../../../../shared/components/input/input';
import { Select, SelectOption } from '../../../../../../shared/components/select/select';

interface UsuarioForm {
  id_rol: FormControl<string>;
  email: FormControl<string>;
  password: FormControl<string>;
  nombres: FormControl<string>;
  apellidos: FormControl<string>;
}

/** Lo que emite el formulario al guardar: los datos de Usuario más, por
 * separado, la foto nueva (si se seleccionó una) o el pedido de quitar la
 * actual. */
export interface UsuarioFormSubmit {
  request: UsuarioRequest;
  archivo: File | null;
  eliminarArchivo: boolean;
}

/** En creación la contraseña es obligatoria; en edición, vacía = no cambiarla
 * (UsuarioServiceImpl solo actualiza password_hash si viene no-blank). */
@Component({
  selector: 'app-usuario-formulario',
  imports: [ReactiveFormsModule, Button, Input, Select, FileUpload],
  templateUrl: './formulario.html',
  styleUrl: './formulario.css',
})
export class Formulario implements OnInit {
  readonly mode = input.required<'create' | 'edit'>();
  readonly initial = input<UsuarioResponse | null>(null);
  readonly roles = input<RolCatalogo[]>([]);
  readonly submitting = input(false);
  readonly currentUserId = input<string | null>(null);

  readonly submitForm = output<UsuarioFormSubmit>();
  readonly cancel = output<void>();

  protected readonly form = new FormGroup<UsuarioForm>({
    id_rol: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }),
    password: new FormControl('', { nonNullable: true }),
    nombres: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(100)] }),
    apellidos: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(100)],
    }),
  });

  protected readonly imagenControl = new FormControl<File | null>(null);
  protected eliminarImagenActual = false;
  protected urlImagenActual: string | null = null;

  ngOnInit(): void {
    if (this.mode() === 'create') {
      this.form.controls.password.addValidators([Validators.required, Validators.minLength(8)]);
    } else {
      this.form.controls.password.addValidators([Validators.minLength(8)]);
    }
    this.form.controls.password.updateValueAndValidity();

    const usuario = this.initial();
    if (this.mode() === 'edit' && usuario) {
      this.form.patchValue({
        id_rol: usuario.id_rol,
        email: usuario.email,
        nombres: usuario.nombres,
        apellidos: usuario.apellidos,
      });
      this.urlImagenActual = usuario.url_img;
    }
  }

  protected onEliminarImagenActual(): void {
    this.eliminarImagenActual = true;
  }

  protected get esUsuarioActual(): boolean {
    return this.mode() === 'edit' && !!this.initial() && this.initial()!.id_usuario === this.currentUserId();
  }

  protected get rolOptions(): SelectOption[] {
    return this.roles().map((rol) => ({ value: rol.id_rol, label: rol.nombre }));
  }

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.submitForm.emit({
      request: {
        id_rol: value.id_rol,
        email: value.email,
        password: value.password,
        nombres: value.nombres,
        apellidos: value.apellidos,
      },
      archivo: this.imagenControl.value,
      eliminarArchivo: this.eliminarImagenActual,
    });
  }
}
