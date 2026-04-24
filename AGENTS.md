# AGENTS.md

## Project

Android diploma project: mobile app for personal time management with gamification.

## Main goal

Build a beginner-friendly Android app that is:

* easy to understand,
* easy to run,
* suitable for diploma demonstration,
* based on Kotlin, Jetpack Compose, MVVM, Room, Coroutines/Flow.

## Constraints

* Prefer simple and robust solutions over clever ones.
* Avoid unnecessary abstraction.
* Avoid introducing backend unless explicitly requested.
* Keep architecture understandable for a student with near-zero Android experience.
* Every major change should preserve buildability.
* Do not leave the project in a broken state.

## Stack

* Kotlin
* Jetpack Compose
* Material 3
* MVVM
* Room
* Coroutines / Flow
* Navigation Compose

## Functional scope

Required:

* task list
* create/edit/delete task
* complete task
* XP and levels
* streak
* achievements
* dashboard
* statistics
* local persistence with Room

Optional:

* local notifications
* seeded demo data
* import/export later

## Work style

When asked to implement something:

1. First inspect current project structure.
2. Explain briefly what files will be changed.
3. Make minimal coherent changes.
4. Run build or relevant checks after meaningful steps.
5. Summarize what changed and any remaining issues.

## Code style

* Use clear naming.
* Keep functions relatively small.
* Prefer explicit code over magic.
* Add short comments only where logic is non-obvious.
* Keep UI code readable.
* Avoid deeply nested logic.

## Architecture guidance

Use packages:

* data
* domain
* presentation
* navigation
* ui/theme

Inside feature areas, keep files grouped logically.
Prefer feature-oriented readability over excessive layering.

## Gamification rules

Base XP:

* easy = 10
* medium = 20
* hard = 40

Bonuses:

* high priority = +10
* completed before due date = +5

Track:

* total XP
* current level
* streak days
* achievements
* daily progress

## Definition of done

A task is done only if:

* code compiles,
* navigation works,
* Room schema is coherent,
* new feature is connected to UI,
* project remains understandable to a beginner,
* README is updated when architecture or setup changes.

## README expectations

README should include:

* project purpose
* tech stack
* architecture overview
* how to run
* features
* gamification rules
* future improvements

## Important

This is a diploma-oriented educational app, not a production enterprise system.
Optimize for clarity, demonstration value, and completeness.
