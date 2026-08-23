---
name: clean-code
description: Use this skill to clean files in the Spring Boot Java project by removing unused imports, dead code, unused autowired dependencies, and unused variables or methods.
arguments: [filename]
argument-hint: "[filename]"
context: fork
background: false
disable-model-invocation: true
---

## Instructions

**CRITICAL PREREQUISITE:** A target filename or directory is mandatory for this skill. If the user asks you to perform a cleanup but does not specify which file(s) to clean, **STOP** and ask the user for the target file(s). Do not proceed with any other steps until the target is provided.

When asked to clean up a file in this Spring Boot project, follow these steps meticulously:

1. **Identify Unused Code and Imports**
   - Carefully analyze the target Java file.
   - Identify `import` statements for classes that are never referenced in the code.
   - Look for private fields, private methods, and local variables that are declared but never read or invoked.

2. **Remove Unused Imports, Variables, and Methods**
   - Remove the unused `import` statements.
   - Remove unused local variables and dead assignments.
   - Remove unused private methods.

3. **Handle Unused Spring Components and Autowired Dependencies**
   - Check injected fields, especially those injected via constructor (e.g., using Lombok's `@RequiredArgsConstructor`). If a dependency (like a Service or Repository) is injected but never used within the class, remove it.
   - Be cautious with `@Value` fields; only remove them if you are absolutely sure they aren't used anywhere in the class.

4. **Exercise Caution with DTOs, Entities, and Projections**
   - **Do NOT** blindly remove fields in DTOs (Request/Response) or JPA Entities (`@Entity`). They might be mapped automatically by Jackson (JSON serialization) or Hibernate, even if no explicit `get()` or `set()` is called in the Java code.
   - **Do NOT** alter or remove aliases in native SQL interface projections (`projection/`) unless you are fixing a specific issue, as these are load-bearing for native queries (as noted in `CLAUDE.md`).
   - Leave MapStruct mappers (`mapper/`) alone unless an underlying DTO/Entity field was deliberately removed.

5. **Clean up Comments and Debug Statements**
   - Clean up any commented-out code blocks that are no longer needed.
   - Remove unnecessary `System.out.println()` or redundant `log.debug()`/`log.info()` statements if they were just for temporary debugging.

6. **Iterate and Verify**
   - After making removals, review the file again to ensure no new unused code was created (e.g., removing a method might make a class-level variable unused).
   - Use Maven to verify the project builds successfully and nothing was broken during the cleanup process.
   - Run: `./mvnw clean compile` to ensure compilation succeeds.
   - Run: `./mvnw test` to ensure tests (like context loads) still pass(Only IF Present).

7. **Review Project Gotchas**
   - Remember the specific architectural details from `CLAUDE.md`: auth is handled via Interceptors, location data uses PostGIS/Redis GEO, and notifications use Postgres LISTEN/NOTIFY. Ensure your cleanup does not accidentally remove these critical flows.
