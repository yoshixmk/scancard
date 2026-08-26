import { config as base } from './wdio.conf.js';

// @slow tests: manual-verification equivalents that are slow (e.g., multi-minute LLM dummy with shade checks).
// Default wdio.conf.js excludes `@slow` via grep/invert; this config includes only them.
const slowConfig = { ...base };
slowConfig.mochaOpts = { ...(base.mochaOpts ?? {}) };
delete slowConfig.mochaOpts.grep;
delete slowConfig.mochaOpts.invert;

export const config = slowConfig;
