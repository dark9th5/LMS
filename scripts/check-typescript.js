#!/usr/bin/env node
const path = require("path");

let ts;
try {
  ts = require("typescript");
} catch {
  const candidates = [
    path.resolve(__dirname, "../apps/web/node_modules/typescript"),
    "/usr/local/lib/node_modules/typescript",
    "/opt/nvm/versions/node/v22.16.0/lib/node_modules/typescript",
  ];
  for (const candidate of candidates) {
    try {
      ts = require(candidate);
      break;
    } catch {
      // Try the next well-known installation location.
    }
  }
}

if (!ts) {
  console.error("TypeScript is not installed. Run npm ci in apps/web first.");
  process.exit(2);
}

const root = path.resolve(__dirname, "../apps/web");
const configPath = path.join(root, "tsconfig.json");
const configFile = ts.readConfigFile(configPath, ts.sys.readFile);

if (configFile.error) {
  console.error(ts.formatDiagnosticsWithColorAndContext([configFile.error], formatHost()));
  process.exit(1);
}

const parsed = ts.parseJsonConfigFileContent(
  configFile.config,
  ts.sys,
  root,
  { noEmit: true, incremental: false },
  configPath,
);

if (parsed.errors.length) {
  console.error(ts.formatDiagnosticsWithColorAndContext(parsed.errors, formatHost()));
  process.exit(1);
}

const program = ts.createProgram({
  rootNames: parsed.fileNames,
  options: parsed.options,
  projectReferences: parsed.projectReferences,
});
const diagnostics = ts.getPreEmitDiagnostics(program);

if (diagnostics.length) {
  console.error(ts.formatDiagnosticsWithColorAndContext(diagnostics, formatHost()));
  process.exit(1);
}

console.log(`OK: ${parsed.fileNames.length} TypeScript/TSX files passed semantic type checking.`);

function formatHost() {
  return {
    getCanonicalFileName: (fileName) => fileName,
    getCurrentDirectory: () => root,
    getNewLine: () => ts.sys.newLine,
  };
}
