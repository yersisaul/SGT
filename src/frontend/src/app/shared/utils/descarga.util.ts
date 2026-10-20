/** Guarda un Blob como archivo local (la descarga pasa por HttpClient para llevar el JWT). */
export function guardarBlob(blob: Blob, nombreArchivo: string): void {
  const url = URL.createObjectURL(blob);
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = nombreArchivo;
  enlace.click();
  URL.revokeObjectURL(url);
}

/** Nombre "<prefijo>-AAAA-MM-DD.csv" con la fecha local. */
export function nombreCsvDelDia(prefijo: string): string {
  const hoy = new Date();
  const dos = (n: number) => String(n).padStart(2, '0');
  return `${prefijo}-${hoy.getFullYear()}-${dos(hoy.getMonth() + 1)}-${dos(hoy.getDate())}.csv`;
}
