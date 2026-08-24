#!/usr/bin/env node
// patch-p-limit.js - ensure p-limit 3.x has limitFunction for asyncbox (Appium)
// Fixes: Cannot require() ES Module p-limit in cycle / Named export 'limitFunction' not found
import { readFileSync, writeFileSync, existsSync, rmSync } from 'node:fs';
import { join } from 'node:path';

const root = join(import.meta.dirname, '..');
const targets = [
  join(root, 'node_modules', 'p-limit', 'index.js'),
];

for (const p of targets) {
  if (!existsSync(p)) continue;
  let src = readFileSync(p, 'utf8');
  if (src.includes('limitFunction')) {
    console.log(`[patch-p-limit] already patched: ${p}`);
    continue;
  }
  // append limitFunction polyfill for p-limit 3.x
  const patch = `
module.exports.limitFunction = (fn, options) => {
  const concurrency = typeof options === 'object' && options !== null ? options.concurrency : options;
  const limit = module.exports(concurrency);
  return (...args) => limit(() => fn(...args));
};
`;
  writeFileSync(p, src.trimEnd() + '\n' + patch + '\n');
  console.log(`[patch-p-limit] patched: ${p}`);
}

// Remove nested p-limit 7.x that causes ESM cycle, fallback to root 3.x
const nested = join(root, 'node_modules', 'appium-uiautomator2-driver', 'node_modules', 'p-limit');
if (existsSync(nested)) {
  const pkg = JSON.parse(readFileSync(join(nested, 'package.json'), 'utf8'));
  if (pkg.version && pkg.version.startsWith('7.')) {
    rmSync(nested, { recursive: true, force: true });
    console.log(`[patch-p-limit] removed nested p-limit ${pkg.version} -> fallback to root 3.x`);
  }
}
