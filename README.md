# Sandbox

## Overview

Welcome to **Sandbox**, a playground for small experiments and utilities built with Kotlin. Each example is
self-contained in its own subdirectory with a clear `main()` demo. Clone the repo, pick your favorite utility, and run
it right away.

## Included Examples

### 1. Deep Copy Utility

A reflection-based deep copier for Java/Kotlin objects.

* Handles arbitrary nested structures, collections, maps, and arrays
* Detects and preserves cycles in the object graph

### 2. SQL Parser

A minimalist SQL lexer & parser built from scratch in Kotlin.

* Tokenizes keywords, identifiers, literals, and symbols
* Parses simple `SELECT` statements into ASTs
* Easily extended with new dialect rules

## Unique IP Counter

A lightning-fast, memory-smart tool for counting unique IPv4 addresses in humongous logs—think hundreds of gigabytes
without breaking a sweat.

* Streamed line-by-line, uses a `BufferedReader`.
* Raw, regex-free parsing.
* Bit-twiddling magic, fast, simple, and zero extra objects.

## Dev Notes

* **Structure**
  Each utility is isolated in its own subproject. Shared patterns (logging, error handling) aren’t centralized—this is
  more a playground than a polished toolkit.

* **Testing**
  Unit tests live alongside code. We use Kotest (AnnotationSpec) for clarity and brevity. If you add new experiments,
  please include a basic test to showcase edge cases.

* **Style**
  Code is kept idiomatic but not dogmatic. Feel free to refactor, this sandbox is meant for tinkering.

* **Contributions**
  If you build a neat toy here, open a PR! Just keep the overall repo small and each module self-contained.

---

Enjoy exploring—and don’t hesitate to drop a line if something breaks spectacularly. This is all for fun!
