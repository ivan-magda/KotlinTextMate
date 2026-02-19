# Benchmark Results

Tracking issue: [#19](https://github.com/ivan-magda/kotlin-textmate/issues/19).
Implementation: [PR #24](https://github.com/ivan-magda/kotlin-textmate/pull/24).

## Setup

- **Framework:** kotlinx-benchmark 0.4.16 (JMH)
- **Configuration:** 3 forks, 5 warmup + 5 measurement iterations, 2s each
- **Machine:** macOS, JVM target 17
- **Scope:** `tokenizeLine()` loop only — grammar loading and pattern compilation excluded via `@Setup(Level.Trial)`

## Corpus Files

| Grammar    | Corpus           |  Lines |
| ---------- | ---------------- | -----: |
| Kotlin     | `large.kt.txt`   |  1,659 |
| JSON       | `large.json.txt` |  3,574 |
| Markdown   | `large.md.txt`   | 15,697 |
| JavaScript | `jquery.js.txt`  |  8,829 |

Corpus files live in `shared-assets/benchmark/`.

## Multi-Line Tokenization

```
Benchmark                         (grammar)  Mode  Cnt    Score    Error  Units
TokenizerBenchmark.tokenizeFile      kotlin  avgt    5   20.93 ±  0.09   ms/op
TokenizerBenchmark.tokenizeFile        json  avgt    5    7.81 ±  0.03   ms/op
TokenizerBenchmark.tokenizeFile    markdown  avgt    5  164.03 ±  1.21   ms/op
TokenizerBenchmark.tokenizeFile  javascript  avgt    5  857.43 ± 55.47   ms/op
```

### Throughput

| Grammar    |  ms/op |   Lines/sec | ms per 1000 lines |
| ---------- | -----: | ----------: | ----------------: |
| Kotlin     |  20.93 |  **79,300** |              12.6 |
| JSON       |   7.81 | **457,600** |               2.2 |
| Markdown   | 164.03 |  **95,700** |              10.4 |
| JavaScript | 857.43 |  **10,300** |              97.1 |

PoC target (<100 ms per 1000 lines) met for all grammars.

### Analysis

- **JSON is extremely fast** — tiny grammar (16 rules), repetitive structure.
- **Markdown** has the most lines (15.7k) but good per-line throughput (~0.01 ms/line) despite 169 rules + 72 while-rules.
- **JavaScript is the outlier** — largest grammar (233 rules, 310 includes) and dense source (jQuery). Unresolved `source.js.regexp` includes ([#16](https://github.com/ivan-magda/kotlin-textmate/issues/16)) cause regex literal patterns to fall through, adding wasted matching attempts.

## Single-Line Tokenization

Requested by [@RedCMD](https://github.com/ivan-magda/kotlin-textmate/issues/19#issuecomment-3930088133). Corpus: [`c.tmLanguage.json`](https://github.com/RedCMD/Lag-Syntax-Highlighter/blob/main/syntaxes/c.tmLanguage.json) — a ~420k character single-line JSON file.

| Grammar                                                                                                            |       ms/op | chars/sec |
| ------------------------------------------------------------------------------------------------------------------ | ----------: | --------: |
| JSON (VS Code built-in)                                                                                            | 9,058 ± 170 |      ~46k |
| JSON ([RedCMD's](https://github.com/RedCMD/TmLanguage-Syntax-Highlighter/blob/main/syntaxes/json.tmLanguage.json)) |  3,996 ± 50 |     ~105k |

RedCMD reports ~500k chars/sec in VS Code with the same grammar — about 4.7x faster. RedCMD's grammar is 2.3x faster than the VS Code built-in on the same file.

## Comparison with Other Engines

| Engine             | Language          | Lines/sec (jQuery) |
| ------------------ | ----------------- | ------------------ |
| **KotlinTextMate** | Kotlin/JVM        | **10,300**         |
| vscode-textmate    | TypeScript/Node   | ~5,600–18,300      |
| syntect            | Rust              | ~13,000            |
| Atom               | C++ (tree-sitter) | ~1,500             |

Sources: vscode-textmate numbers from its own benchmarks, syntect from `syncat` benches, Atom from GitHub blog posts.

## Running Benchmarks

```bash
# Full run (~8-10 min)
./gradlew :benchmark:benchmark

# Quick sanity check (~1 min)
./gradlew :benchmark:smokeBenchmark

# Allocation profiling (manual JAR)
./gradlew :benchmark:mainBenchmarkJar
java -jar benchmark/build/benchmarks/main/jars/main-benchmark.jar -prof gc -f 3 -wi 5 -i 5
```
