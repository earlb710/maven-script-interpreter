# Copilot Space Rules for maven-script-interpreter

Purpose
- Define consistent guidance for this Copilot Space grounded in the repository's documentation and code.
- Ensure responses align with the project's architecture, language semantics, UI system, and workflows.

Primary Sources of Truth (in order of precedence)
- Architecture: ARCHITECTURE.md
  - https://github.com/earlb710/maven-script-interpreter/blob/master/ARCHITECTURE.md
- Project overview and getting started: README.md
  - https://github.com/earlb710/maven-script-interpreter/blob/master/README.md
- UI areas, items, metadata: AREA_DEFINITION.md
  - https://github.com/earlb710/maven-script-interpreter/blob/master/AREA_DEFINITION.md
- Test scripts and UI feature behaviors: ScriptInterpreter/scripts/README.md
  - https://github.com/earlb710/maven-script-interpreter/blob/master/ScriptInterpreter/scripts/README.md
- JSON schemas and parsing conventions: ScriptInterpreter/src/main/resources/json/README.md
  - https://github.com/earlb710/maven-script-interpreter/blob/master/ScriptInterpreter/src/main/resources/json/README.md
- EBS Script syntax : EBS_SCRIPT_SYNTAX.md
  - https://github.com/earlb710/maven-script-interpreter/blob/master/EBS_SCRIPT_SYNTAX.md

General Response Policy
- Cite the relevant source file(s) above when giving guidance or examples.
- If a conflict exists, prefer ARCHITECTURE.md, then README.md, then other docs.
- Keep examples valid for Java 21, Maven 3.x, and JavaFX 21 (see README.md).

Language and Console Rules
- EBS syntax must follow the EBNF at ScriptInterpreter/src/main/java/com/eb/script/syntax_ebnf.txt (linked from README.md).
- Interactive console:
  - Use `mvn javafx:run` to start; `Ctrl+Enter` executes the editor script.
  - Support documented console commands (`/open`, `/save`, `/help`, `/clear`, `/echo on|off`, `/ai setup`, `/safe-dirs`).
- CLI execution:
  - `java -cp target/classes com.eb.script.Run <script.ebs>`

Screens and Variables
- Screen windows are NOT shown automatically; require explicit: `screen <name> show;` and support hide/show cycles.
- Each screen runs in its own thread; variables are thread-safe and accessed via `screenName.varName`.
- Prefer variable-level DisplayMetadata; AreaItem display can override. If AreaItem omits display, it falls back to the var's metadata.

JSON and Schema Validation
- For screen definitions:
  - Keys are case-insensitive at parse time: the JSON parser normalizes keys to lowercase.
  - Accept camelCase and snake_case; examples and lookups should assume lowercase normalization (`varref`, `prompttext`, `onclick`).
- Encourage schema usage:
  - `screen-definition.json` for full screen docs
  - `area-definition.json` for container areas
  - `display-metadata.json` for control metadata
- Recommend adding `$schema` in JSON files for IDE validation.

UI Layout and Styling
- Use the canonical AreaType (containers) and ItemType (controls) enumerations.
- Respect layout and sizing properties:
  - Ordering: `sequence`
  - Positioning: `layoutPos` (GridPane row,col or BorderPane region), `alignment`
  - Growth: `hgrow`, `vgrow`
  - Spacing: `margin`, `padding`
  - Size: `prefWidth`/`prefHeight`, `minWidth`/`minHeight`, `maxWidth`/`maxHeight`
- CSS conventions:
  - Areas: `screen-area-*` (screen-areas.css)
  - Items: `screen-item-*` (screen-items.css)
- Scripts README features:
  - Support `promptAlignment` for AreaItem
  - Support reduced padding for text/textarea controls demonstrated in tests

Built-ins, Arrays, and Features
- Use built-in functions as documented in README.md (type conversion, strings, arrays, math, file I/O, JSON, date/time).
- Arrays:
  - Respect types and bounds; use `.length`; support fixed/dynamic implementations.
- JSON operations:
  - `parseJson` / `stringifyJson` preserve key case for general JSON; only screen-definition parsing normalizes keys.

Database Usage
- Default adapter: `OracleDbAdapter`.
- Patterns to follow:
  - Connection lifecycle: connect → use → close
  - Cursor lifecycle: declare → open → iterate (`hasNext` / `next`) → close
  - Always close cursors and connections.

Build, Run, and Tooling
- Build: `mvn clean compile` under `ScriptInterpreter`
- Run UI: `mvn javafx:run`
- Run CLI script: `java -cp target/classes com.eb.script.Run <script.ebs>`
- Class tree analysis: `java -cp target/classes com.eb.util.ClassTreeLister [srcDir]`

Security and Safety
- Honor sandbox root and Safe Directories configuration; avoid file I/O outside trusted dirs.
- Emphasize type checking, bounds checking, and error isolation (no full app crashes).

Performance and Quality
- Reuse `RuntimeContext` when appropriate for repeated executions.
- UI performance: avoid excessive container nesting; minimize inline style overrides; choose appropriate containers.
- Provide runnable examples and encourage use of test scripts.

Testing and Scripts
- Use and extend test scripts in `ScriptInterpreter/scripts`.
- Naming convention: `test_screen_*.ebs`; update the scripts README when adding new tests.
- Validate cross-screen operations, boundary conditions, and thread-safe variable access.
- **Before creating EBS scripts**: Always verify correct syntax in `EBS_SCRIPT_SYNTAX.md` and available builtins in `Builtins.java`
  - Function syntax: `functionName(params) return returnType { ... }` (no `function` keyword)
  - Builtin functions: Check `Builtins.java` for available functions (e.g., `string.toUpper`, not `upper`)
  - Test scripts with parser validation before committing

Documentation and Maintenance
- When changes affect architecture, screen parsing, built-ins, or keywords:
  - Update `ARCHITECTURE.md`.
  - Update any keyword/builtin lookup resources referenced there.
- Keep JSON schema docs and examples aligned with parser behavior.
- Prefer absolute GitHub links to canonical docs in responses.
- Put new documents generated in the docs folder

Interaction Conventions for This Space
- When producing screen JSON, ensure it validates against the schemas and uses lowercase key lookups (while accepting camel/snake inputs).
- When asked about UI behavior, default to:
  - Explicit show/hide
  - `screenName.varName` threaded access
  - DisplayMetadata fallback logic
- When proposing new controls/areas, reference AreaType/ItemType and note schema impacts.

Appendix: Quick Links
- README.md: https://github.com/earlb710/maven-script-interpreter/blob/master/README.md
- ARCHITECTURE.md: https://github.com/earlb710/maven-script-interpreter/blob/master/ARCHITECTURE.md
- AREA_DEFINITION.md: https://github.com/earlb710/maven-script-interpreter/blob/master/AREA_DEFINITION.md
- Scripts README (tests): https://github.com/earlb710/maven-script-interpreter/blob/master/ScriptInterpreter/scripts/README.md
- JSON README (schemas & parsing): https://github.com/earlb710/maven-script-interpreter/blob/master/ScriptInterpreter/src/main/resources/json/README.md
- Class Tree Lister: https://github.com/earlb710/maven-script-interpreter/blob/master/CLASS_TREE_LISTER.md

Meta
- Owner: earlb710
- Last updated: 2025-11-17
