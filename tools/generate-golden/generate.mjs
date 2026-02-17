// Generates golden tokenization snapshots from the canonical vscode-textmate.
// Requires Node.js >= 21.2 (for import.meta.dirname).
// Usage: cd tools/generate-golden && npm install && npm run generate

import { readFileSync, writeFileSync, mkdirSync, readdirSync } from "fs";
import { createRequire } from "module";
import { join, relative } from "path";
const require = createRequire(import.meta.url);
const vsctm = require("vscode-textmate");
const oniguruma = require("vscode-oniguruma");
const wasmBin = readFileSync(
  require.resolve("vscode-oniguruma/release/onig.wasm"),
);
await oniguruma.loadWASM(wasmBin.buffer);

const onigLib = Promise.resolve({
  createOnigScanner: (patterns) => new oniguruma.OnigScanner(patterns),
  createOnigString: (s) => new oniguruma.OnigString(s),
});

const ROOT = join(import.meta.dirname, "..", "..");
const GRAMMARS_DIR = join(ROOT, "shared-assets", "grammars");
const CORPUS_DIR = join(import.meta.dirname, "corpus");
const OUTPUT_DIR = join(
  ROOT,
  "core",
  "src",
  "test",
  "resources",
  "conformance",
  "golden",
);

// Grammar config: directory name in corpus/ -> grammar file + scope name
const GRAMMARS = {
  json: { file: "JSON.tmLanguage.json", scope: "source.json" },
  kotlin: { file: "kotlin.tmLanguage.json", scope: "source.kotlin" },
  markdown: { file: "markdown.tmLanguage.json", scope: "text.html.markdown" },
};

async function tokenizeFile(grammar, sourceText) {
  const lines = sourceText.split("\n");
  let ruleStack = vsctm.INITIAL;
  const result = [];

  for (const line of lines) {
    const r = grammar.tokenizeLine(line, ruleStack);
    const tokens = r.tokens.map((t) => ({
      value: line.substring(t.startIndex, t.endIndex),
      scopes: t.scopes,
    }));
    result.push({ line, tokens });
    ruleStack = r.ruleStack;
  }
  return result;
}

mkdirSync(OUTPUT_DIR, { recursive: true });

for (const [lang, config] of Object.entries(GRAMMARS)) {
  const grammarPath = join(GRAMMARS_DIR, config.file);
  const grammarContent = readFileSync(grammarPath, "utf8");
  const rawGrammar = vsctm.parseRawGrammar(grammarContent, grammarPath);

  const registry = new vsctm.Registry({
    onigLib,
    loadGrammar: () => null,
  });

  const grammar = await registry.addGrammar(rawGrammar);
  const corpusDir = join(CORPUS_DIR, lang);
  const corpusFiles = readdirSync(corpusDir).filter((f) => !f.startsWith(".")).sort();

  const files = [];
  let totalLines = 0;
  let totalTokens = 0;

  for (const corpusFile of corpusFiles) {
    const sourceText = readFileSync(join(corpusDir, corpusFile), "utf8");
    const lines = await tokenizeFile(grammar, sourceText);
    files.push({ source: `${lang}/${corpusFile}`, lines });
    totalLines += lines.length;
    totalTokens += lines.reduce((sum, l) => sum + l.tokens.length, 0);
  }

  const version = JSON.parse(
    readFileSync(
      join(
        import.meta.dirname,
        "node_modules",
        "vscode-textmate",
        "package.json",
      ),
      "utf8",
    ),
  ).version;
  const snapshot = {
    grammar: `grammars/${config.file}`,
    generatedWith: `vscode-textmate@${version}`,
    files,
  };

  const outputPath = join(OUTPUT_DIR, `${lang}.snapshot.json`);
  writeFileSync(outputPath, JSON.stringify(snapshot, null, 2) + "\n");
  console.log(
    `${lang}: ${corpusFiles.length} files, ${totalLines} lines, ${totalTokens} tokens -> ${relative(ROOT, outputPath)}`,
  );
}

console.log(
  "\nDone. Review with: git diff core/src/test/resources/conformance/golden/",
);
