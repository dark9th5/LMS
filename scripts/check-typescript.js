#!/usr/bin/env node
const fs = require("fs");
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
    try { ts = require(candidate); break; } catch { /* try next */ }
  }
}
if (!ts) {
  console.error("TypeScript is not installed. Run npm install in apps/web first.");
  process.exit(2);
}
const root = path.resolve(__dirname, "../apps/web");
const errors = [];
function walk(dir) {
  for (const name of fs.readdirSync(dir)) {
    const file = path.join(dir, name);
    const stat = fs.statSync(file);
    if (stat.isDirectory()) {
      if (!["node_modules", ".next"].includes(name)) walk(file);
    } else if (/\.(ts|tsx)$/.test(file) && !file.endsWith(".d.ts")) {
      const source = fs.readFileSync(file, "utf8");
      const result = ts.transpileModule(source, {
        fileName: file,
        reportDiagnostics: true,
        compilerOptions: {
          target: ts.ScriptTarget.ES2022,
          module: ts.ModuleKind.ESNext,
          jsx: ts.JsxEmit.ReactJSX,
        },
      });
      for (const diagnostic of result.diagnostics || []) {
        const message = ts.flattenDiagnosticMessageText(diagnostic.messageText, "\n");
        errors.push(`${path.relative(root, file)}: ${message}`);
      }
    }
  }
}
walk(root);
if (errors.length) {
  console.error(errors.join("\n"));
  process.exit(1);
}
console.log("OK: TypeScript/TSX syntax validated.");
