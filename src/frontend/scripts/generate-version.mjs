/**
 * Genera src/app/core/config/app-version.ts con la versión de la aplicación.
 *
 * Origen (en orden): variable de entorno APP_VERSION (la usa el build de
 * Docker) → APP_VERSION del .env en la raíz del repositorio → '0.0.0-dev'.
 * Se ejecuta solo vía los hooks pre* de package.json; el archivo generado no
 * se versiona.
 */
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const frontendRoot = join(dirname(fileURLToPath(import.meta.url)), '..');
const envFile = join(frontendRoot, '..', '..', '.env');
const outFile = join(frontendRoot, 'src', 'app', 'core', 'config', 'app-version.ts');

function readFromEnvFile() {
  if (!existsSync(envFile)) return undefined;
  const line = readFileSync(envFile, 'utf8')
    .split(/\r?\n/)
    .find((l) => /^\s*APP_VERSION\s*=/.test(l));
  return line?.split('=').slice(1).join('=').trim().replace(/^["']|["']$/g, '');
}

const version = (process.env.APP_VERSION || readFromEnvFile() || '0.0.0-dev').trim();

// Solo SemVer: evita inyectar texto arbitrario en el bundle.
if (!/^\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?$/.test(version)) {
  console.error(`APP_VERSION inválida: "${version}". Usa SemVer, p. ej. 1.0.0`);
  process.exit(1);
}

const content = `// Archivo generado por scripts/generate-version.mjs: no editar a mano.
export const APP_VERSION = '${version}';
`;
if (!existsSync(outFile) || readFileSync(outFile, 'utf8') !== content) {
  writeFileSync(outFile, content);
}
console.log(`SGT v${version}`);
