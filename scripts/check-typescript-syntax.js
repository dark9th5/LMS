#!/usr/bin/env node
const fs = require("fs");
const path = require("path");

let ts;
for (const candidate of [
  "typescript",
  path.resolve(__dirname, "../apps/web/node_modules/typescript"),
  "/usr/local/lib/node_modules/typescript",
  "/opt/nvm/versions/node/v22.16.0/lib/node_modules/typescript",
]) {
  try {
    ts = require(candidate);
    break;
  } catch {
    // Continue.
  }
}
if (!ts) {
  console.error("TypeScript is not installed. Run npm ci in apps/web first.");
  process.exit(2);
}

const root = path.resolve(__dirname, "../apps/web");
const files = [];
function walk(dir) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    if (["node_modules", ".next"].includes(entry.name)) continue;
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) walk(full);
    else if (/\.(?:ts|tsx)$/.test(entry.name) && !entry.name.endsWith(".d.ts")) files.push(full);
  }
}
walk(root);

const diagnostics = [];
for (const file of files) {
  const source = fs.readFileSync(file, "utf8");
  const output = ts.transpileModule(source, {
    fileName: file,
    reportDiagnostics: true,
    compilerOptions: {
      target: ts.ScriptTarget.ES2022,
      module: ts.ModuleKind.ESNext,
      jsx: ts.JsxEmit.ReactJSX,
      isolatedModules: true,
    },
  });
  for (const diagnostic of output.diagnostics || []) diagnostics.push(diagnostic);
}
if (diagnostics.length) {
  console.error(ts.formatDiagnosticsWithColorAndContext(diagnostics, {
    getCanonicalFileName: (name) => name,
    getCurrentDirectory: () => root,
    getNewLine: () => ts.sys.newLine,
  }));
  process.exit(1);
}
console.log(`OK: ${files.length} TypeScript/TSX files passed syntax transpilation.`);
