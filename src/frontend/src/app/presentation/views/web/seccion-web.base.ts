import { Directive, OnInit, computed, inject, signal } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { Observable, of, switchMap } from 'rxjs';

import { AuthService } from '../../../core/auth/auth.service';
import { RecursoWeb, WebContenidoService } from '../../../core/services/web-contenido.service';
import { NotificationService } from '../../../shared/services/notification.service';
import { extractApiErrorMessage } from '../../../shared/utils/api-error.util';

export type FiltroPublicacion = 'todos' | 'publicados' | 'ocultos';

interface ItemPublicable {
  publicado: boolean;
}

/**
 * Comportamiento común de las secciones del panel del sitio web con CRUD,
 * publicación y un archivo (productos, servicios, partners, clientes, casos
 * de éxito y documentos): listado con búsqueda y filtro, editor en drawer,
 * guardado en dos pasos (datos y luego archivo, porque el archivo necesita
 * el id) y confirmación de borrado. Cada sección define su formulario.
 */
@Directive()
export abstract class SeccionWebBase<T extends ItemPublicable, Req> implements OnInit {
  protected readonly authService = inject(AuthService);
  protected readonly web = inject(WebContenidoService);
  protected readonly notifications = inject(NotificationService);

  protected abstract readonly recurso: RecursoWeb<T, Req>;
  /** Grupo de permisos: "web_catalogo", "web_comercial" o "web_documento". */
  protected abstract readonly grupoPermiso: string;
  /** "producto", "servicio"…: para los mensajes. */
  protected abstract readonly entidad: string;
  protected abstract readonly form: FormGroup;

  protected abstract id(item: T): string;
  protected abstract nombre(item: T): string;
  protected abstract textoBusqueda(item: T): string;
  /** Carga el formulario para crear (null) o editar. */
  protected abstract cargarFormulario(item: T | null): void;
  protected abstract construirRequest(): Req;

  protected readonly items = signal<T[]>([]);
  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  protected readonly busqueda = signal('');
  protected readonly filtro = signal<FiltroPublicacion>('todos');

  protected readonly editor = signal<'crear' | 'editar' | null>(null);
  protected readonly seleccionado = signal<T | null>(null);
  protected readonly guardando = signal(false);
  protected readonly errorFormulario = signal<string | null>(null);
  protected readonly archivoControl = new FormControl<File | null>(null);
  protected quitarArchivoActual = false;

  protected readonly publicando = signal<string | null>(null);
  protected readonly aEliminar = signal<T | null>(null);
  protected readonly eliminando = signal(false);
  protected readonly errorEliminar = signal<string | null>(null);

  protected readonly conteo = computed(() => {
    const lista = this.items();
    const publicados = lista.filter((item) => item.publicado).length;
    return { todos: lista.length, publicados, ocultos: lista.length - publicados };
  });

  protected readonly filtrados = computed(() => {
    const termino = this.busqueda().trim().toLowerCase();
    const filtro = this.filtro();
    return this.items().filter(
      (item) =>
        (filtro === 'todos' || (filtro === 'publicados') === item.publicado) &&
        (!termino || this.textoBusqueda(item).toLowerCase().includes(termino)),
    );
  });

  protected get canCreate(): boolean {
    return this.authService.hasPermission(`${this.grupoPermiso}.create`);
  }

  protected get canUpdate(): boolean {
    return this.authService.hasPermission(`${this.grupoPermiso}.update`);
  }

  protected get canDelete(): boolean {
    return this.authService.hasPermission(`${this.grupoPermiso}.delete`);
  }

  ngOnInit(): void {
    this.cargar();
  }

  protected cargar(): void {
    this.loading.set(true);
    this.loadError.set(null);
    this.web.listar(this.recurso).subscribe({
      next: (lista) => {
        this.items.set(lista);
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set(`No se pudo cargar la lista de ${this.entidad}s.`);
        this.loading.set(false);
      },
    });
  }

  protected abrirCrear(): void {
    this.prepararEditor(null);
    this.editor.set('crear');
  }

  protected abrirEditar(item: T): void {
    this.prepararEditor(item);
    this.editor.set('editar');
  }

  protected cerrarEditor(): void {
    if (this.guardando()) return;
    this.editor.set(null);
    this.seleccionado.set(null);
  }

  protected marcarQuitarArchivo(): void {
    this.quitarArchivoActual = true;
  }

  protected enviar(): void {
    if (this.form.invalid || this.guardando()) {
      this.form.markAllAsTouched();
      return;
    }
    const request = this.construirRequest();
    const actual = this.seleccionado();
    const archivo = this.archivoControl.value;
    this.guardando.set(true);
    this.errorFormulario.set(null);

    const guardado$ = actual
      ? this.web.editar(this.recurso, this.id(actual), request)
      : this.web.crear(this.recurso, request);
    guardado$
      .pipe(switchMap((guardado) => this.gestionarArchivo(guardado, archivo)))
      .pipe(switchMap((guardado) => this.trasGuardarArchivo(guardado, archivo !== null)))
      .subscribe({
        next: (final) => {
          this.reemplazarOAgregar(final);
          this.guardando.set(false);
          this.editor.set(null);
          this.seleccionado.set(null);
          this.notifications.success(
            actual ? `${this.capitalizar(this.entidad)} actualizado.` : `${this.capitalizar(this.entidad)} creado.`,
          );
        },
        error: (error: unknown) => {
          this.guardando.set(false);
          this.errorFormulario.set(extractApiErrorMessage(error));
          // Si se creó pero falló el archivo, la lista debe reflejarlo igual.
          this.cargar();
        },
      });
  }

  /** Paso extra tras subir el archivo (p. ej. documentos: publicar recién con el PDF). */
  protected trasGuardarArchivo(guardado: T, _subioArchivo: boolean): Observable<T> {
    return of(guardado);
  }

  protected alternarPublicacion(item: T, publicado: boolean): void {
    const id = this.id(item);
    this.publicando.set(id);
    this.web.cambiarPublicacion(this.recurso, id, publicado).subscribe({
      next: (actualizado) => {
        this.reemplazarOAgregar(actualizado);
        this.publicando.set(null);
        this.notifications.success(
          publicado ? `"${this.nombre(item)}" ya se ve en el sitio.` : `"${this.nombre(item)}" se ocultó del sitio.`,
        );
      },
      error: () => this.publicando.set(null),
    });
  }

  protected pedirEliminar(item: T): void {
    this.errorEliminar.set(null);
    this.aEliminar.set(item);
  }

  protected cancelarEliminar(): void {
    if (this.eliminando()) return;
    this.aEliminar.set(null);
  }

  protected confirmarEliminar(): void {
    const item = this.aEliminar();
    if (!item) return;
    this.eliminando.set(true);
    this.errorEliminar.set(null);
    this.web.eliminar(this.recurso, this.id(item)).subscribe({
      next: () => {
        this.items.update((lista) => lista.filter((otro) => this.id(otro) !== this.id(item)));
        this.eliminando.set(false);
        this.aEliminar.set(null);
        if (this.seleccionado() && this.id(this.seleccionado()!) === this.id(item)) {
          this.editor.set(null);
          this.seleccionado.set(null);
        }
        this.notifications.success(`${this.capitalizar(this.entidad)} eliminado.`);
      },
      error: (error: unknown) => {
        this.eliminando.set(false);
        this.errorEliminar.set(extractApiErrorMessage(error));
      },
    });
  }

  /** Texto opcional del formulario: vacío → null (el backend lo guarda sin valor). */
  protected texto(valor: string | null | undefined): string | null {
    const limpio = (valor ?? '').trim();
    return limpio === '' ? null : limpio;
  }

  /** Listas de texto: sin filas vacías. */
  protected limpiarLista(lista: string[]): string[] {
    return lista.map((item) => item.trim()).filter(Boolean);
  }

  private prepararEditor(item: T | null): void {
    this.seleccionado.set(item);
    this.errorFormulario.set(null);
    this.archivoControl.setValue(null);
    this.quitarArchivoActual = false;
    this.form.reset();
    this.cargarFormulario(item);
  }

  private gestionarArchivo(guardado: T, archivo: File | null): Observable<T> {
    const id = this.id(guardado);
    if (archivo) {
      return this.web.subirArchivo(this.recurso, id, archivo);
    }
    if (this.quitarArchivoActual && this.recurso.archivo !== 'archivo') {
      return this.web.eliminarArchivo(this.recurso, id);
    }
    return of(guardado);
  }

  private reemplazarOAgregar(item: T): void {
    const id = this.id(item);
    this.items.update((lista) =>
      lista.some((otro) => this.id(otro) === id)
        ? lista.map((otro) => (this.id(otro) === id ? item : otro))
        : [...lista, item],
    );
  }

  private capitalizar(texto: string): string {
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  }
}
