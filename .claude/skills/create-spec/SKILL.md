---
name: create-spec
description: Create a feature branch and implementation spec for the next Tiffin-Ghar development step. Use when the user wants to start a new feature, begin a roadmap step, or create a feature specification.
argument-hint: '[number] [feature | bug]'
---

# Create Spec and Feature Branch

You are a senior developer working on the Tiffin-Ghar app.

Follow the project guidelines and execute the workflow below in order. Do not skip steps or continue past a failed prerequisite.

## 0. Verify Arguments (Mandatory)

The user MUST provide the required arguments when calling this skill: a `step_number` and a `feature_title` (or bug description).

If these arguments are missing, incomplete, or ambiguous, **STOP IMMEDIATELY**. Do not proceed to verify the working tree, and do not make any repository changes. Ask the user to provide the missing arguments.

> **Crucial User Interaction Rule**: Whenever a decision depends on the user (such as missing arguments or ambiguous values), **always provide recommended options** that they can directly choose from or manually type, instead of just ending the response with an open-ended question.

## 1. Verify the Working Tree

Run:

```bash
git status --short
```

The working tree must be completely clean.

If the command returns any output, **stop immediately** and ask the user to commit or stash their changes.

Do not modify, stash, commit, or discard the user's changes.

## 2. Determine Feature Metadata

Extract the following from the user's provided arguments:

| Field           | Requirement                                                             |
| --------------- | ----------------------------------------------------------------------- |
| `step_number`   | Zero-padded to 2 digits (`2` → `02`, `11` → `11`)                       |
| `feature_title` | Human-readable Title Case                                               |
| `feature_slug`  | Lowercase kebab-case, only `a-z`, `0-9`, and `-`, maximum 40 characters |
| `branch_name`   | `features/<feature_slug>`                                               |

As stated in Step 0, if any required value cannot be determined confidently, stop and ask the user before proceeding (remembering to provide recommended options).

### Slug rules

The slug must:

- be lowercase
- use hyphens instead of spaces
- contain only `a-z`, `0-9`, and `-`
- be no longer than 40 characters
- describe the feature clearly
- avoid unnecessary words

Example:

```text
Feature title: Search Functionality
Feature slug: search-functionality
Branch: features/search-functionality
Spec: .claude/specs/02-search-functionality.md
```

## 3. Check for an Existing Branch

Run:

```bash
git branch --list "features/*"
```

If `features/<feature_slug>` already exists, do not overwrite or reuse it.

Create a unique branch name by appending an incrementing numeric suffix:

```text
features/search-functionality
features/search-functionality-01
features/search-functionality-02
```

Use the first available suffix.

## 4. Update Main

Switch to `main` and pull the latest changes:

```bash
git switch main
git pull origin main
```

If either command fails, stop and report the error.

Do not continue to branch creation or spec generation.

## 5. Create the Feature Branch

Create and switch to the selected branch:

```bash
git switch -c <branch_name>
```

Verify the current branch:

```bash
git branch --show-current
```

It must match `<branch_name>` before continuing.

## 6. Research the Codebase

Read the following before writing the spec.
_(Note: Assume your working directory is the repository root when resolving these paths.)_

### Project guidelines

```text
.claude/CLAUDE.md
```

Use this file as the source of truth for roadmap context, architecture,
conventions, schema, and project-specific rules.

### Existing application structure

Inspect:

```text
app/_layout.tsx
app/index.tsx
```

Use these to understand the current routing and application structure.

### Existing specifications

Inspect the contents of:

```text
.claude/specs/
```

Use existing filenames and specifications to:

- avoid duplicate feature names
- understand previous roadmap steps
- identify dependencies
- maintain consistency with existing specs

Do not modify existing specifications.

## 7. Create the Feature Specification

Create a spec using the following structure exactly.

```markdown
# Spec: <feature_title>

## Overview

One paragraph explaining what this feature does, why it is needed, and where it
fits in the Tiffin-Ghar roadmap.

## Depends on

List the previous roadmap steps or features that must already be complete.

If there are no dependencies:

No dependencies.

## Screens and Routing

List every new or modified route.

Format:

- `PATH` — description — access level (`public` / `logged-in`)

If no routes are required:

No new routes.

## State & Data (Zustand/Services)

Describe any new or modified Zustand stores, API services, persistence, or
data models.

If none are required:

No state/data changes.

## Components

### Create

List every new component, including its expected location.

### Modify

List every existing component that must be changed and explain why.

If a section has no changes:

None.

## Files to change

List every existing file that will be modified.

If none:

None.

## Files to create

List every new file that will be created.

If none:

None.

## New dependencies

List every new Bun/npm dependency and why it is required.

If none:

No new dependencies.

## Rules for implementation

Include the following rules:

- Use NativeWind for styling with `className`.
- Use `features/<feature-name>` for feature branches.
- Follow Conventional Commits (`feat:`, `fix:`, `chore:`, `ui:`, etc.).
- Use absolute imports such as `@/components/...`.
- Use direct `async/await` service calls from hooks/components.
- Do not introduce React Query, SWR, or another data-fetching library unless
  explicitly required by the project guidelines.
- Follow the existing project architecture and conventions documented in
  `.claude/CLAUDE.md`.

Add any feature-specific implementation constraints discovered during codebase
research.

## Definition of done

Provide a concrete, testable checklist.

Every item must be verifiable by running the application or inspecting the
resulting behavior.

Example:

- [ ] User can navigate to the new screen.
- [ ] The screen renders correctly for the required access level.
- [ ] User can complete the primary feature flow.
- [ ] Loading and error states are handled.
- [ ] Data is persisted or retrieved as specified.
- [ ] Existing functionality remains unaffected.
```

### Specification requirements

The spec must be implementation-ready.

Do not:

- invent files that are not justified by the feature
- introduce dependencies without a reason
- duplicate existing architecture
- prescribe implementation details that conflict with `.claude/CLAUDE.md`
- leave required behavior ambiguous

Prefer concrete file paths, route names, component names, and observable
behavior over vague descriptions.

## 8. Save the Specification

Ensure the directory exists before attempting to save the file:

```bash
mkdir -p .claude/specs
```

Save the spec as:

```text
.claude/specs/<step_number>-<feature_slug>.md
```

Example:

```text
.claude/specs/02-search-functionality.md
```

Then verify that the file exists:

```bash
test -f .claude/specs/<step_number>-<feature_slug>.md
```

If verification fails, stop and report the error.

## 9. Final Verification

Before reporting success, verify:

1. The current branch is `<branch_name>`.
2. The spec file exists at the expected path.
3. The spec contains the required sections.
4. No existing files were unintentionally modified.

Run:

```bash
git branch --show-current
git status --short
```

The expected result is:

- current branch = `<branch_name>`
- only the newly created spec should appear as an uncommitted change (note: if `.claude/specs` was newly created, git tracks the file, not the directory itself).

## 10. Report

Report the result using exactly this format:

```text
Branch:    <branch_name>
Spec file: .claude/specs/<step_number>-<feature_slug>.md
Title:     <feature_title>
```

Then say:

> Review the spec at `.claude/specs/<step_number>-<feature_slug>.md` and let me know if you want to proceed with implementation.
