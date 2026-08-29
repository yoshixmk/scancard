/**
 * virtualScene.js — Automated Virtual Scene image injection for emulator.
 * Uses emulator console `virtualscene-image` via `adb emu` (host-side).
 * Does NOT use `mobile: shell` (device-side) because console is host-side.
 * Tested: `adb -s emulator-5554 emu help virtualscene-image` -> Usage: virtualscene-image <wall|table> [path]
 */
import { execFileSync } from 'node:child_process';
import { existsSync, readdirSync } from 'node:fs';
import { join, resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const ADB = process.env.ANDROID_HOME
    ? join(process.env.ANDROID_HOME, 'platform-tools', process.platform === 'win32' ? 'adb.exe' : 'adb')
    : process.platform === 'win32'
        ? join(process.env.LOCALAPPDATA || '', 'Android', 'Sdk', 'platform-tools', 'adb.exe')
        : 'adb';

function getAdbPath() {
    if (existsSync(ADB)) return ADB;
    return 'adb';
}

function getEmulatorSerial() {
    // wdio.conf.js default; allow override
    return process.env.ANDROID_SERIAL || process.env.ANDROID_DEVICE || 'emulator-5554';
}

/**
 * Execute `adb -s <serial> emu virtualscene-image <target> [absPath]`
 * @param {'wall'|'table'} target
 * @param {string|null} absPath - absolute host path to PNG/JPEG, or null/undefined to restore default
 * @returns {boolean} true if OK
 */
export function setVirtualSceneImage(target, absPath) {
    const serial = getEmulatorSerial();
    const adb = getAdbPath();
    const targetNorm = target === 'table' ? 'table' : 'wall';
    const args = ['-s', serial, 'emu', 'virtualscene-image', targetNorm];
    if (absPath) {
        const resolved = resolve(absPath);
        if (!existsSync(resolved)) {
            console.warn(`[virtualScene] image not found: ${resolved}`);
            return false;
        }
        args.push(resolved);
    }
    try {
        const out = execFileSync(adb, args, { encoding: 'utf8', timeout: 8000 });
        const ok = out.includes('OK') || out.trim() === '';
        console.log(`[virtualScene] ${targetNorm} ${absPath ? absPath : '(default)'} -> ${out.trim() || 'OK'} (serial=${serial})`);
        return ok || out.trim() === 'OK' || out.trim() === '';
    } catch (e) {
        console.warn(`[virtualScene] failed ${targetNorm} ${absPath}: ${e.message}`);
        if (e.stdout) console.warn(String(e.stdout));
        if (e.stderr) console.warn(String(e.stderr));
        return false;
    }
}

/**
 * Restore both wall and table to defaults.
 */
export function resetVirtualSceneImages() {
    let ok = true;
    ok = setVirtualSceneImage('wall', null) && ok;
    ok = setVirtualSceneImage('table', null) && ok;
    return ok;
}

/**
 * Auto-setup: push all images in appium/images to emulator virtual scene.
 * By convention: files named *wall* -> wall, *table* -> table, otherwise wall.
 * Default: appium/images/sample.jpg -> wall (as requested).
 * @param {string} imagesDir - absolute path to images dir
 */
export function setupVirtualSceneFromDir(imagesDir) {
    const dir = resolve(imagesDir);
    if (!existsSync(dir)) {
        console.log(`[virtualScene] dir not found, skip: ${dir}`);
        return false;
    }
    const files = readdirSync(dir).filter(f => /\.(png|jpe?g)$/i.test(f));
    if (files.length === 0) {
        console.log(`[virtualScene] no images in ${dir}, skip`);
        return false;
    }
    let ok = true;
    for (const f of files) {
        const full = join(dir, f);
        const lower = f.toLowerCase();
        const target = lower.includes('table') ? 'table' : 'wall';
        const res = setVirtualSceneImage(target, full);
        ok = res && ok;
    }
    return ok;
}

// Resolve default images dir relative to this file: appium/helpers -> ../images
const __dirname = dirname(fileURLToPath(import.meta.url));
export const DEFAULT_IMAGES_DIR = resolve(__dirname, '..', 'images');
