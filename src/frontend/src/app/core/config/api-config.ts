import { InjectionToken } from '@angular/core';

/**
 * URL base de la API del backend SGT. Se provee en app.config.ts en vez de
 * importar un environment.ts directamente en los servicios, para que la
 * configuración sea reemplazable sin tocar el código que la consume.
 */
export const API_BASE_URL = new InjectionToken<string>('API_BASE_URL');
