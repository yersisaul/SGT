import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Observable, of, switchMap } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import {
  ESTADO_NOVEDAD_LABEL,
  EstadoNovedad,
  WebCategoriaNovedadResponse,
  WebNovedadRequest,
  WebNovedadResponse,
} from '../../../../core/models/web.model';
import { WebContenidoService } from '../../../../core/services/web-contenido.service';
import { Button } from '../../../../shared/components/button/button';
import { Dialog } from '../../../../shared/components/dialog/dialog';
import { FileUpload } from '../../../../shared/components/file-upload/file-upload';
import { Spinner } from '../../../../shared/components/spinner/spinner';
import {
  LucideChevronLeft,
  LucideChevronRight,
  LucideCircleAlert,
  LucidePencil,
  LucidePlus,
  LucideSearch,
  LucideStar,
  LucideTag,
  LucideTrash,
} from '../../../../shared/icons/lucide-icons';
import { NotificationService } from '../../../../shared/services/notification.service';
import { extractApiErrorMessage } from '../../../../shared/utils/api-error.util';
import { WebMiniatura } from '../components/web-miniatura';

const TAMANO_PAGINA = 20;

type Pestana = 'TODAS' | EstadoNovedad;
/** Estado visible: una publicada con fecha futura está "programada". */
type EstadoVisible = 'BORRADOR' | 'PUBLICADO' | 'PROGRAMADO' | 'ARCHIVADO';

/**
 * Novedades del sitio (/api/web/novedades): nacen en borrador y se publican,
 * programan o archivan con web_novedad.publish. Paginadas en el backend.
 */
@Component({
  selector: 'app-web-novedades',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    Button,
    Dialog,
    FileUpload,
    Spinner,
    WebMiniatura,
    LucideChevronLeft,
    LucideChevronRight,
    LucideCircleAlert,
    LucidePencil,
    LucidePlus,
    LucideSearch,
    LucideStar,
    LucideTag,
    LucideTrash,
  ],
  templateUrl: './novedades.html',
  styleUrls: ['../../administracion/administracion.css', '../components/web-controles.css', '../web.css', './novedades.css'],
})
export class Novedades implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly web = inject(WebContenidoService);
  private readonly notifications = inject(NotificationService);

  protected readonly canCreate = this.authService.hasPermission('web_novedad.create');
  protected readonly canUpdate = this.authService.hasPermission('web_novedad.update');
  protected readonly canDelete = this.authService.hasPermission('web_novedad.delete');
  protected readonly canPublish = this.authService.hasPermission('web_novedad.publish');
  protected readonly estadoLabel = ESTADO_NOVEDAD_LABEL;

  protected readonly pestanas: { id: Pestana; etiqueta: string }[] = [
    { id: 'TODAS', etiqueta: 'Todas' },
    { id: 'BORRADOR', etiqueta: 'Borradores' },
    { id: 'PUBLICADO', etiqueta: 'Publicadas' },
    { id: 'ARCHIVADO', etiqueta: 'Archivadas' },
  ];

  protected readonly pestana = signal<Pestana>('TODAS');
  protected readonly pagina = signal(0);
  protected readonly totalPaginas = signal(0);
  protected readonly totalElementos = signal(0);
  protected readonly novedades = signal<WebNovedadResponse[]>([]);
  protected readonly categorias = signal<WebCategoriaNovedadResponse[]>([]);
  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  protected readonly busqueda = signal('');

  protected readonly filtradas = computed(() => {
    const termino = this.busqueda().trim().toLowerCase();
    return this.novedades().filter(
      (n) => !termino || `${n.titulo} ${n.nombre_categoria ?? ''} ${n.nombre_autor ?? ''}`.toLowerCase().includes(termino),
    );
  });

  // ---------- Editor ----------
  protected readonly editor = signal<'crear' | 'editar' | null>(null);
  protected readonly seleccionada = signal<WebNovedadResponse | null>(null);
  protected readonly guardando = signal(false);
  protected readonly errorFormulario = signal<string | null>(null);
  protected readonly portadaControl = new FormControl<File | null>(null);
  private quitarPortada = false;

  protected readonly form = new FormGroup({
    titulo: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(200)] }),
    slug: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(120)] }),
    id_categoria_novedad: new FormControl('', { nonNullable: true }),
    resumen: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(500)] }),
    contenido: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(50000)] }),
    destacado: new FormControl(false, { nonNullable: true }),
  });

  /** Fecha para programar (datetime-local, hora local del navegador). */
  protected readonly fechaProgramada = signal('');
  protected readonly cambiandoEstado = signal(false);

  // ---------- Eliminar ----------
  protected readonly aEliminar = signal<WebNovedadResponse | null>(null);
  protected readonly eliminando = signal(false);
  protected readonly errorEliminar = signal<string | null>(null);

  // ---------- Categorías ----------
  protected readonly categoriasAbiertas = signal(false);
  protected readonly categoriaEditando = signal<string | null>(null);
  protected readonly categoriaGuardando = signal(false);
  protected readonly categoriaError = signal<string | null>(null);
  protected readonly categoriaForm = new FormGroup({
    nombre: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(100)] }),
    slug: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(120)] }),
    orden: new FormControl(0, { nonNullable: true, validators: [Validators.min(0), Validators.max(9999)] }),
  });

  ngOnInit(): void {
    this.cargar();
    this.cargarCategorias();
  }

  protected estadoVisible(novedad: WebNovedadResponse): EstadoVisible {
    return novedad.estado === 'PUBLICADO' && !novedad.visible_en_sitio ? 'PROGRAMADO' : novedad.estado;
  }

  protected cambiarPestana(pestana: Pestana): void {
    this.pestana.set(pestana);
    this.pagina.set(0);
    this.cargar();
  }

  protected irAPagina(pagina: number): void {
    if (pagina < 0 || pagina >= this.totalPaginas()) return;
    this.pagina.set(pagina);
    this.cargar();
  }

  protected cargar(): void {
    this.loading.set(true);
    this.loadError.set(null);
    const estado = this.pestana() === 'TODAS' ? null : (this.pestana() as EstadoNovedad);
    this.web.listarNovedades(estado, this.pagina(), TAMANO_PAGINA).subscribe({
      next: (pagina) => {
        this.novedades.set(pagina.contenido);
        this.totalPaginas.set(pagina.total_paginas);
        this.totalElementos.set(pagina.total_elementos);
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set('No se pudieron cargar las novedades.');
        this.loading.set(false);
      },
    });
  }

  // ---------- Editor ----------

  protected abrirCrear(): void {
    this.prepararEditor(null);
    this.editor.set('crear');
  }

  protected abrirEditar(novedad: WebNovedadResponse): void {
    this.prepararEditor(novedad);
    this.editor.set('editar');
  }

  protected cerrarEditor(): void {
    if (this.guardando() || this.cambiandoEstado()) return;
    this.editor.set(null);
    this.seleccionada.set(null);
  }

  protected marcarQuitarPortada(): void {
    this.quitarPortada = true;
  }

  /** Guarda los datos (y la portada). Con `publicar`, además la publica de inmediato. */
  protected guardar(publicar = false): void {
    if (this.form.invalid || this.guardando()) {
      this.form.markAllAsTouched();
      return;
    }
    const valor = this.form.getRawValue();
    const request: WebNovedadRequest = {
      titulo: valor.titulo.trim(),
      slug: this.texto(valor.slug),
      id_categoria_novedad: valor.id_categoria_novedad || null,
      resumen: this.texto(valor.resumen),
      contenido: this.texto(valor.contenido),
      destacado: valor.destacado,
    };
    const actual = this.seleccionada();
    const portada = this.portadaControl.value;
    this.guardando.set(true);
    this.errorFormulario.set(null);

    (actual ? this.web.editarNovedad(actual.id_novedad, request) : this.web.crearNovedad(request))
      .pipe(switchMap((guardada) => this.gestionarPortada(guardada, portada)))
      .pipe(
        switchMap((guardada) =>
          publicar ? this.web.cambiarEstadoNovedad(guardada.id_novedad, { estado: 'PUBLICADO' }) : of(guardada),
        ),
      )
      .subscribe({
        next: (final) => {
          this.guardando.set(false);
          this.editor.set(null);
          this.seleccionada.set(null);
          this.notifications.success(
            publicar ? 'Novedad publicada.' : actual ? 'Novedad actualizada.' : 'Borrador guardado.',
          );
          this.reemplazarOCargar(final, !actual);
        },
        error: (error: unknown) => {
          this.guardando.set(false);
          this.errorFormulario.set(extractApiErrorMessage(error));
          this.cargar();
        },
      });
  }

  protected publicarAhora(): void {
    // Sin fecha el backend conserva la anterior: una programada seguiría esperando.
    const actual = this.seleccionada();
    const fecha = actual && this.estadoVisible(actual) === 'PROGRAMADO' ? new Date().toISOString() : null;
    this.cambiarEstado('PUBLICADO', fecha, 'Novedad publicada.');
  }

  protected programar(): void {
    const local = this.fechaProgramada();
    if (!local) return;
    const fecha = new Date(local);
    if (Number.isNaN(fecha.getTime()) || fecha.getTime() <= Date.now()) {
      this.errorFormulario.set('Elige una fecha y hora futuras para programar la publicación.');
      return;
    }
    this.cambiarEstado('PUBLICADO', fecha.toISOString(), 'Publicación programada.');
  }

  protected archivar(): void {
    this.cambiarEstado('ARCHIVADO', null, 'Novedad archivada: ya no se ve en el sitio.');
  }

  protected volverABorrador(): void {
    this.cambiarEstado('BORRADOR', null, 'La novedad volvió a borrador.');
  }

  // ---------- Eliminar ----------

  protected pedirEliminar(novedad: WebNovedadResponse): void {
    this.errorEliminar.set(null);
    this.aEliminar.set(novedad);
  }

  protected cancelarEliminar(): void {
    if (this.eliminando()) return;
    this.aEliminar.set(null);
  }

  protected confirmarEliminar(): void {
    const novedad = this.aEliminar();
    if (!novedad) return;
    this.eliminando.set(true);
    this.web.eliminarNovedad(novedad.id_novedad).subscribe({
      next: () => {
        this.eliminando.set(false);
        this.aEliminar.set(null);
        if (this.seleccionada()?.id_novedad === novedad.id_novedad) {
          this.editor.set(null);
          this.seleccionada.set(null);
        }
        this.notifications.success('Novedad eliminada.');
        this.cargar();
      },
      error: (error: unknown) => {
        this.eliminando.set(false);
        this.errorEliminar.set(extractApiErrorMessage(error));
      },
    });
  }

  // ---------- Categorías ----------

  protected abrirCategorias(): void {
    this.nuevaCategoria();
    this.categoriasAbiertas.set(true);
  }

  protected cerrarCategorias(): void {
    if (this.categoriaGuardando()) return;
    this.categoriasAbiertas.set(false);
  }

  protected nuevaCategoria(): void {
    this.categoriaEditando.set(null);
    this.categoriaError.set(null);
    this.categoriaForm.reset({ nombre: '', slug: '', orden: this.categorias().length });
  }

  protected editarCategoria(categoria: WebCategoriaNovedadResponse): void {
    this.categoriaEditando.set(categoria.id_categoria_novedad);
    this.categoriaError.set(null);
    this.categoriaForm.reset({ nombre: categoria.nombre, slug: categoria.slug, orden: categoria.orden });
  }

  protected guardarCategoria(): void {
    if (this.categoriaForm.invalid || this.categoriaGuardando()) {
      this.categoriaForm.markAllAsTouched();
      return;
    }
    const valor = this.categoriaForm.getRawValue();
    const request = { nombre: valor.nombre.trim(), slug: this.texto(valor.slug), orden: valor.orden };
    const id = this.categoriaEditando();
    this.categoriaGuardando.set(true);
    this.categoriaError.set(null);
    (id ? this.web.editarCategoria(id, request) : this.web.crearCategoria(request)).subscribe({
      next: () => {
        this.categoriaGuardando.set(false);
        this.notifications.success(id ? 'Categoría actualizada.' : 'Categoría creada.');
        this.nuevaCategoria();
        this.cargarCategorias();
      },
      error: (error: unknown) => {
        this.categoriaGuardando.set(false);
        this.categoriaError.set(extractApiErrorMessage(error));
      },
    });
  }

  protected eliminarCategoria(categoria: WebCategoriaNovedadResponse): void {
    this.categoriaGuardando.set(true);
    this.categoriaError.set(null);
    this.web.eliminarCategoria(categoria.id_categoria_novedad).subscribe({
      next: () => {
        this.categoriaGuardando.set(false);
        this.notifications.success(`Categoría "${categoria.nombre}" eliminada.`);
        if (this.categoriaEditando() === categoria.id_categoria_novedad) this.nuevaCategoria();
        this.cargarCategorias();
      },
      error: (error: unknown) => {
        this.categoriaGuardando.set(false);
        this.categoriaError.set(extractApiErrorMessage(error));
      },
    });
  }

  // ---------- Internos ----------

  private cargarCategorias(): void {
    this.web.listarCategorias().subscribe({ next: (lista) => this.categorias.set(lista), error: () => undefined });
  }

  private prepararEditor(novedad: WebNovedadResponse | null): void {
    this.seleccionada.set(novedad);
    this.errorFormulario.set(null);
    this.portadaControl.setValue(null);
    this.quitarPortada = false;
    this.fechaProgramada.set('');
    this.form.reset({
      titulo: novedad?.titulo ?? '',
      slug: novedad?.slug ?? '',
      id_categoria_novedad: novedad?.id_categoria_novedad ?? '',
      resumen: novedad?.resumen ?? '',
      contenido: novedad?.contenido ?? '',
      destacado: novedad?.destacado ?? false,
    });
  }

  private gestionarPortada(novedad: WebNovedadResponse, portada: File | null): Observable<WebNovedadResponse> {
    if (portada) return this.web.subirPortadaNovedad(novedad.id_novedad, portada);
    if (this.quitarPortada) return this.web.eliminarPortadaNovedad(novedad.id_novedad);
    return of(novedad);
  }

  private cambiarEstado(estado: EstadoNovedad, fecha: string | null, mensaje: string): void {
    const actual = this.seleccionada();
    if (!actual || this.cambiandoEstado()) return;
    this.cambiandoEstado.set(true);
    this.errorFormulario.set(null);
    this.web.cambiarEstadoNovedad(actual.id_novedad, { estado, fecha_publicacion: fecha }).subscribe({
      next: (actualizada) => {
        this.cambiandoEstado.set(false);
        this.seleccionada.set(actualizada);
        this.fechaProgramada.set('');
        this.notifications.success(mensaje);
        this.reemplazarOCargar(actualizada, false);
      },
      error: (error: unknown) => {
        this.cambiandoEstado.set(false);
        this.errorFormulario.set(extractApiErrorMessage(error));
      },
    });
  }

  /** Si la novedad sigue cumpliendo el filtro se reemplaza en su lugar; si no, se recarga la página. */
  private reemplazarOCargar(novedad: WebNovedadResponse, nueva: boolean): void {
    const pestana = this.pestana();
    const sigueEnPestana = pestana === 'TODAS' || pestana === novedad.estado;
    if (nueva || !sigueEnPestana) {
      this.cargar();
      return;
    }
    this.novedades.update((lista) => lista.map((n) => (n.id_novedad === novedad.id_novedad ? novedad : n)));
  }

  private texto(valor: string | null | undefined): string | null {
    const limpio = (valor ?? '').trim();
    return limpio === '' ? null : limpio;
  }
}
