import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Observable, catchError, forkJoin, map, of } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { UsuarioService } from '../../../../core/services/usuario.service';
import {
  CASOS_EXITO,
  CLIENTES,
  DOCUMENTOS,
  PARTNERS,
  PRODUCTOS,
  RecursoWeb,
  SERVICIOS,
  WebContenidoService,
} from '../../../../core/services/web-contenido.service';
import { Card } from '../../../../shared/components/card/card';
import { Spinner } from '../../../../shared/components/spinner/spinner';
import { LucideChevronRight, LucideCircleAlert } from '../../../../shared/icons/lucide-icons';

interface Tarjeta {
  etiqueta: string;
  ruta: string;
  publicados: number;
  total: number;
}

interface Pendiente {
  texto: string;
  ruta: string;
  tono: 'alerta' | 'info';
}

interface Conteo {
  publicados: number;
  total: number;
  sinArchivo: number;
}

interface ConArchivo {
  publicado: boolean;
  url_img?: string | null;
  url_logo?: string | null;
  url_archivo?: string | null;
}

/**
 * Estado del sitio de un vistazo: cuánto contenido está publicado por
 * sección y qué falta completar (imágenes, PDF, borradores, registros).
 * Cada bloque se carga solo si el usuario tiene el permiso de esa sección.
 */
@Component({
  selector: 'app-web-resumen',
  imports: [RouterLink, Card, Spinner, LucideChevronRight, LucideCircleAlert],
  templateUrl: './resumen.html',
  styleUrls: ['../../administracion/administracion.css', '../components/web-controles.css', '../web.css', './resumen.css'],
})
export class Resumen implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly web = inject(WebContenidoService);
  private readonly usuarioService = inject(UsuarioService);

  protected readonly nombre = this.authService.user()?.nombres ?? '';
  protected readonly loading = signal(true);
  protected readonly tarjetas = signal<Tarjeta[]>([]);
  protected readonly pendientes = signal<Pendiente[]>([]);

  ngOnInit(): void {
    const puede = (permiso: string) => this.authService.hasPermission(permiso);
    const catalogo = puede('web_catalogo.read');
    const comercial = puede('web_comercial.read');

    forkJoin({
      productos: catalogo ? this.contar(PRODUCTOS) : of(null),
      servicios: catalogo ? this.contar(SERVICIOS) : of(null),
      casos: comercial ? this.contar(CASOS_EXITO) : of(null),
      clientes: comercial ? this.contar(CLIENTES) : of(null),
      partners: comercial ? this.contar(PARTNERS) : of(null),
      documentos: puede('web_documento.read') ? this.contar(DOCUMENTOS) : of(null),
      borradores: puede('web_novedad.read') ? this.totalNovedades('BORRADOR') : of(null),
      publicadas: puede('web_novedad.read') ? this.totalNovedades('PUBLICADO') : of(null),
      registros: puede('usuario.update')
        ? this.usuarioService.listarPorHabilitado(false).pipe(
            map((usuarios) => usuarios.filter((u) => u.nombre_rol?.toLowerCase() === 'cliente').length),
            catchError(() => of(null)),
          )
        : of(null),
    }).subscribe((r) => {
      const tarjetas: Tarjeta[] = [];
      const agregar = (etiqueta: string, ruta: string, conteo: Conteo | null) => {
        if (conteo) tarjetas.push({ etiqueta, ruta, publicados: conteo.publicados, total: conteo.total });
      };
      agregar('Productos', '/web/productos', r.productos);
      agregar('Servicios', '/web/servicios', r.servicios);
      agregar('Casos de éxito', '/web/casos-exito', r.casos);
      agregar('Clientes', '/web/clientes', r.clientes);
      agregar('Partners', '/web/partners', r.partners);
      agregar('Brochures', '/web/documentos', r.documentos);
      if (r.publicadas !== null && r.borradores !== null) {
        tarjetas.push({
          etiqueta: 'Novedades',
          ruta: '/web/novedades',
          publicados: r.publicadas,
          total: r.publicadas + r.borradores,
        });
      }
      this.tarjetas.set(tarjetas);

      const pendientes: Pendiente[] = [];
      if (r.registros) {
        pendientes.push({
          texto: `${r.registros} ${r.registros === 1 ? 'cliente espera' : 'clientes esperan'} que habilites su cuenta`,
          ruta: '/web/registros',
          tono: 'alerta',
        });
      }
      if (r.borradores) {
        pendientes.push({
          texto: `${r.borradores} ${r.borradores === 1 ? 'novedad en borrador' : 'novedades en borrador'}`,
          ruta: '/web/novedades',
          tono: 'info',
        });
      }
      const sinArchivo = (conteo: Conteo | null, singular: string, plural: string, ruta: string) => {
        if (conteo?.sinArchivo) {
          pendientes.push({ texto: `${conteo.sinArchivo} ${conteo.sinArchivo === 1 ? singular : plural}`, ruta, tono: 'info' });
        }
      };
      sinArchivo(r.documentos, 'documento sin PDF', 'documentos sin PDF', '/web/documentos');
      sinArchivo(r.productos, 'producto sin imagen', 'productos sin imagen', '/web/productos');
      sinArchivo(r.clientes, 'cliente sin logo', 'clientes sin logo', '/web/clientes');
      sinArchivo(r.partners, 'partner sin logo', 'partners sin logo', '/web/partners');
      this.pendientes.set(pendientes);
      this.loading.set(false);
    });
  }

  protected porcentaje(tarjeta: Tarjeta): number {
    return tarjeta.total === 0 ? 0 : Math.round((tarjeta.publicados / tarjeta.total) * 100);
  }

  private contar<T extends ConArchivo, Req>(recurso: RecursoWeb<T, Req>): Observable<Conteo | null> {
    return this.web.listar(recurso).pipe(
      map((lista) => ({
        publicados: lista.filter((item) => item.publicado).length,
        total: lista.length,
        sinArchivo: lista.filter((item) => !(item.url_img ?? item.url_logo ?? item.url_archivo)).length,
      })),
      catchError(() => of(null)),
    );
  }

  private totalNovedades(estado: 'BORRADOR' | 'PUBLICADO'): Observable<number | null> {
    return this.web.listarNovedades(estado, 0, 1).pipe(
      map((pagina) => pagina.total_elementos),
      catchError(() => of(null)),
    );
  }
}
