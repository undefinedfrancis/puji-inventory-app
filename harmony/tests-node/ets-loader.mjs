import { existsSync } from 'node:fs';
import { readFile } from 'node:fs/promises';
import { extname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { stripTypeScriptTypes } from 'node:module';

export async function resolve(specifier, context, nextResolve) {
  if (specifier.startsWith('.') && extname(specifier) === '') {
    const etsUrl = new URL(`${specifier}.ets`, context.parentURL);
    if (existsSync(fileURLToPath(etsUrl))) {
      return { url: etsUrl.href, shortCircuit: true };
    }
  }
  return nextResolve(specifier, context);
}

export async function load(url, context, nextLoad) {
  if (url.endsWith('.ets')) {
    const source = await readFile(fileURLToPath(url), 'utf8');
    return {
      format: 'module',
      shortCircuit: true,
      source: stripTypeScriptTypes(source, { mode: 'transform', sourceMap: false })
    };
  }
  return nextLoad(url, context);
}
