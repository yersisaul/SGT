import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  {
    // El área autenticada depende de localStorage/JWT (no existen en build time)
    // y de authGuard, que siempre redirigiría a /login durante el prerender.
    path: 'app/**',
    renderMode: RenderMode.Client
  },
  {
    path: '**',
    renderMode: RenderMode.Prerender
  }
];
