import { HttpClient } from '@angular/common/http';
import { Injectable, NgZone, OnDestroy, PLATFORM_ID, effect, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { AuthService } from '../auth/auth.service';
import { API_BASE_URL } from '../config/api-config';
import { EstadoCatalogo } from '../models/catalogo.model';
import { OrdenResponse } from '../models/orden.model';
import { NotificationService } from '../../shared/services/notification.service';

/** Aviso recibido por el stream (backend: NotificacionResponse). */
export interface NotificacionOrden {
  tipo: 'orden.encolada' | 'orden.asignada' | 'orden.devuelta' | 'orden.escalada';
  id_orden: string;
  numero_orden: string;
  id_especialidad: string;
}

const RECONEXION_INICIAL_MS = 1_000;
const RECONEXION_MAXIMA_MS = 30_000;
const RESINCRONIZACION_MS = 60_000;

const MENSAJES: Record<NotificacionOrden['tipo'], (numero: string) => string> = {
  'orden.encolada': (numero) => `Nueva orden ${numero} en la cola de tu especialidad.`,
  'orden.asignada': (numero) => `Te asignaron la orden ${numero}.`,
  'orden.devuelta': (numero) => `La orden ${numero} fue devuelta: requiere tu decisión.`,
  'orden.escalada': (numero) => `La orden ${numero} superó el tiempo de espera en cola.`,
};

/**
 * Avisos en tiempo real (PRD E5). Usa fetch + ReadableStream en lugar de
 * EventSource para enviar el JWT en el header Authorization: el token nunca
 * viaja en la URL (NFR-006). Reconecta con backoff exponencial y jitter, y
 * resincroniza el contador con la API al conectar y cada 60 s (FR-029).
 */
@Injectable({ providedIn: 'root' })
export class NotificacionStreamService implements OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);
  private readonly apiBaseUrl = inject(API_BASE_URL);
  private readonly zone = inject(NgZone);
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));

  /** Último aviso recibido; las vistas lo observan para refrescarse. */
  readonly ultimoEvento = signal<NotificacionOrden | null>(null);
  /** OT pendientes de acción del usuario: en su cola + asignadas sin confirmar (badge del menú). */
  readonly pendientes = signal(0);

  private controlador: AbortController | null = null;
  private esperaReconexion = RECONEXION_INICIAL_MS;
  private temporizadorReconexion: ReturnType<typeof setTimeout> | null = null;
  private temporizadorResincronizacion: ReturnType<typeof setInterval> | null = null;

  constructor() {
    effect(() => {
      const usuario = this.auth.user();
      if (usuario && this.auth.hasPermission('orden.read')) {
        this.iniciar();
      } else {
        this.detener();
      }
    });
  }

  ngOnDestroy(): void {
    this.detener();
  }

  /** Vuelve a calcular el contador desde la API (fuente de verdad). */
  resincronizar(): void {
    if (!this.auth.hasPermission('orden.tomar')) {
      this.pendientes.set(0);
      return;
    }
    const yo = this.auth.user()?.id;
    forkJoin({
      cola: this.http.get<OrdenResponse[]>(`${this.apiBaseUrl}/ordenes/cola`).pipe(catchError(() => of([]))),
      ordenes: this.http.get<OrdenResponse[]>(`${this.apiBaseUrl}/ordenes`).pipe(catchError(() => of([]))),
      estados: this.http.get<EstadoCatalogo[]>(`${this.apiBaseUrl}/estados`).pipe(catchError(() => of([]))),
    }).subscribe(({ cola, ordenes, estados }) => {
      const idAsignada = estados.find((e) => e.nombre.trim().toLowerCase() === 'asignada')?.id_estado;
      const asignadasSinConfirmar = ordenes.filter(
        (o) => o.id_usuario === yo && !o.fecha_cierre && o.id_estado === idAsignada,
      ).length;
      this.pendientes.set(cola.length + asignadasSinConfirmar);
    });
  }

  private iniciar(): void {
    if (!this.isBrowser || this.controlador) return;
    this.resincronizar();
    this.temporizadorResincronizacion = setInterval(() => this.resincronizar(), RESINCRONIZACION_MS);
    this.conectar();
  }

  private detener(): void {
    this.controlador?.abort();
    this.controlador = null;
    if (this.temporizadorReconexion) clearTimeout(this.temporizadorReconexion);
    if (this.temporizadorResincronizacion) clearInterval(this.temporizadorResincronizacion);
    this.temporizadorReconexion = null;
    this.temporizadorResincronizacion = null;
    this.pendientes.set(0);
  }

  private conectar(): void {
    const token = this.auth.getToken();
    if (!token) return;
    const controlador = new AbortController();
    this.controlador = controlador;

    // Fuera de la zona de Angular: un stream abierto no debe impedir la estabilidad de la app.
    this.zone.runOutsideAngular(() => {
      fetch(`${this.apiBaseUrl}/notificaciones/stream`, {
        headers: { Authorization: `Bearer ${token}`, Accept: 'text/event-stream' },
        signal: controlador.signal,
      })
        .then(async (respuesta) => {
          if (!respuesta.ok || !respuesta.body) {
            throw new Error(`HTTP ${respuesta.status}`);
          }
          this.esperaReconexion = RECONEXION_INICIAL_MS;
          await this.leer(respuesta.body.getReader());
          throw new Error('stream cerrado');
        })
        .catch(() => {
          if (controlador.signal.aborted) return;
          this.programarReconexion();
        });
    });
  }

  private async leer(lector: ReadableStreamDefaultReader<Uint8Array>): Promise<void> {
    const decodificador = new TextDecoder();
    let pendiente = '';
    for (;;) {
      const { value, done } = await lector.read();
      if (done) return;
      pendiente += decodificador.decode(value, { stream: true });
      const bloques = pendiente.split(/\r?\n\r?\n/);
      pendiente = bloques.pop() ?? '';
      bloques.forEach((bloque) => this.procesarBloque(bloque));
    }
  }

  /** Un bloque SSE: líneas "event:" y "data:"; las que empiezan con ":" son heartbeat. */
  private procesarBloque(bloque: string): void {
    const datos = bloque
      .split(/\r?\n/)
      .filter((linea) => linea.startsWith('data:'))
      .map((linea) => linea.slice('data:'.length).trim())
      .join('');
    if (!datos) return;
    try {
      const aviso = JSON.parse(datos) as NotificacionOrden;
      this.zone.run(() => {
        this.ultimoEvento.set(aviso);
        this.notifications.show(MENSAJES[aviso.tipo]?.(aviso.numero_orden) ?? 'Hay novedades en tus órdenes.', 'info');
        this.resincronizar();
      });
    } catch {
      // Bloque mal formado: se ignora sin cortar el stream.
    }
  }

  private programarReconexion(): void {
    this.controlador = null;
    if (!this.auth.user()) return;
    const jitter = Math.random() * this.esperaReconexion * 0.3;
    this.temporizadorReconexion = setTimeout(() => {
      this.resincronizar();
      this.conectar();
    }, this.esperaReconexion + jitter);
    this.esperaReconexion = Math.min(this.esperaReconexion * 2, RECONEXION_MAXIMA_MS);
  }
}
