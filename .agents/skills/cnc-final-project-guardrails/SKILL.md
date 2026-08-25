---
name: cnc-final-project-guardrails
description: Plan, implement, or review changes in this CNC Java/JavaFX final-project repository while enforcing its architecture, scope, testing claims, and development-note requirements. Use for code, configuration, SQL, tests, UI, geometry, layout, G-code, persistence, and related implementation documentation in this repository.
---

# CNC Final Project Guardrails

Use this skill only in the repository that contains the matching root `AGENTS.md`.
Read `AGENTS.md` before planning or changing anything. Treat it as the source of
confirmed project decisions; this skill defines the recurring verification workflow.

## Before implementation

1. Inspect the current repository state and identify what is actually implemented.
2. Check the requested work against `AGENTS.md`.
3. Flag any unconfirmed dependency, value, machine setting, or scope expansion.
4. Keep the approved step to one small, independently verifiable unit.
5. State how the result will be tested and which files are expected to change.

Do not invent missing decisions. Do not add future-development features or adjacent
architecture merely because they may be useful later.

## Architecture review

For each relevant change, verify:

- UI controllers coordinate input and output but contain no SQL, geometry
  calculations, layout algorithm, or G-code generation logic.
- Business orchestration belongs in the service layer.
- Input and domain validation remain outside controllers where reusable.
- Geometry and ToolPath code produce geometric/path data, not G-code text.
- Layout code produces placements and does not generate G-code.
- G-code code consumes prepared geometry or ToolPaths and does not calculate layout.
- Persistence code owns JDBC and SQL.
- JDBC statements containing values use `PreparedStatement`; do not construct SQL
  by concatenating user or domain values.
- Domain classes do not depend on JavaFX or JDBC.
- No `Part` abstraction or other unapproved scope is introduced.

Prefer code whose responsibility and control flow can be explained clearly by the
student. When reviewing a design, identify unnecessary indirection, hidden behavior,
or abstractions that make the implementation harder to defend without providing a
concrete project benefit.

## Claims and CNC-specific checks

- Describe layout output only as the result of the implemented algorithm.
- Never call a layout mathematically, globally, or provably optimal unless a later
  confirmed requirement and proof establish that claim.
- Distinguish software status from physical-machine evidence:
  - `IMPLEMENTIRANO` means the behavior exists in the current code.
  - `TESTIRANO` means the stated test was actually executed and its result recorded.
- Do not describe RichAuto A11 compatibility as physically tested before a recorded
  test on the target ZK-1325 / RichAuto A11 setup.
- Do not invent JDK versions, `ToolType` values, machining parameters, axis
  orientation, work zero, controller behavior, or other physical settings.

## Verification after changes

1. Inspect the final diff and summarize every changed file.
2. Run the smallest relevant build, unit test, integration test, or static check.
3. Record the exact command and result.
4. State separately what was implemented, what was tested, and what remains untested.
5. Do not claim success for checks that were not run or could not complete.

## Development notes

After a task changes code, configuration, SQL, tests, or UI:

1. Verify that a prompt-specific note was created or updated under
   `Dokumentacija/biljeske/`.
2. Verify that `Dokumentacija/biljeske/00_indeks.md` was updated.
3. If the task introduced or confirmed an important architectural or technological
   decision, verify that `Dokumentacija/biljeske/00_odluke.md` was updated.
4. Verify that the note follows `Dokumentacija/biljeske/00_predlozak_biljeske.md`
   and retains all required sections.
5. Ensure the note matches the current code and actual test evidence.
6. Verify that visual candidates and a Git commit/hash are recorded only when they
   actually exist.
7. If the documentation structure does not yet exist, report it as a blocking
   documentation prerequisite instead of silently treating documentation as complete.

A note must distinguish `IMPLEMENTIRANO` from `TESTIRANO` and must not contain
secrets, passwords, tokens, personal data, or personal local paths.

## Code-snippet candidates

Select at most a few candidates that materially explain architecture, an algorithm,
validation, persistence, ToolPath generation, G-code generation, UI coordination,
or error handling.

For each candidate include:

- current repository-relative file path;
- class and method;
- why the code matters;
- a possible final-thesis chapter;
- only the relevant excerpt from the current code.

Do not select getters, setters, imports, trivial constructors, generated boilerplate,
whole classes without need, duplicate examples, or unused experiments. If the task
contains no meaningful candidate, explicitly record that none exists.
