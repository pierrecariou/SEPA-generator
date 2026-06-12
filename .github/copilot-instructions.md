# Copilot instructions for SEPA Generator

## Project context

SEPA Generator is a Java desktop application for generating SEPA payment XML files from spreadsheet inputs.

The project is moving toward a polished final Community Edition. Treat the application as a real product, not a prototype. Code quality, UI consistency, validation correctness, and maintainability are important.

The application handles financial/payment data, so correctness, clarity, and careful validation are essential.

The app processes files locally. Do not add cloud processing, telemetry, tracking, or external API calls unless explicitly requested.

## Architecture

Respect the existing package structure and responsibilities:

* `view`: Swing UI, panels, dialogs, rendering, user interaction.
* `service`: application logic, validation orchestration, persistence of local preferences, coordination between UI and generation logic.
* `generator`: SEPA XML generation and format-specific output logic.
* `model`: domain/data objects representing configuration, transactions, payment information, and generated data.

Do not put business logic directly into Swing components if it can be moved into a service.

Do not put UI concerns into model or generator classes.

Create new classes when it improves clarity, separation of concerns, testability, or maintainability.

## Clean code principles

Prioritize clean, readable, maintainable Java code.

Follow SOLID principles where practical:

* Single Responsibility: one class should have one clear reason to change.
* Open/Closed: prefer extension over modifying fragile existing logic.
* Liskov Substitution: keep inheritance safe and predictable.
* Interface Segregation: avoid large unfocused interfaces.
* Dependency Inversion: high-level logic should not depend unnecessarily on low-level details.

Prefer:

* small cohesive classes
* explicit names
* simple methods
* clear responsibilities
* immutable data where practical
* meaningful validation errors
* readable control flow
* predictable side effects

Avoid:

* large god classes
* duplicated logic
* hidden coupling between UI and generation logic
* clever abstractions without real value
* broad unrelated formatting changes
* mixing refactoring and feature work in the same change when avoidable

## Dependencies and libraries

Prefer existing project dependencies and standard Java APIs when they are sufficient.

If a well-known library would clearly solve a problem better than custom code, suggest it, but ask before adding a new dependency.

Do not add dependencies silently.

Do not reimplement complex standard behavior from scratch if a reliable existing library or Java API is appropriate.

## UI and FlatLaf design

The UI is a Java Swing desktop application using FlatLaf.

Preserve and improve the current professional visual identity:

* clean layout
* grey-first palette
* teal/accent highlights
* light and dark theme support
* simple spacing
* readable typography
* rounded components where appropriate
* subtle borders
* no clutter
* no annoying popups

FlatLaf consistency matters. When adding or changing UI components:

* use existing FlatLaf styling patterns where possible
* keep buttons, panels, cards, inputs, borders, and spacing visually consistent
* preserve both light and dark theme quality
* avoid hard-coded colors unless they follow the existing theme palette
* prefer `UIManager` theme values or existing theme constants when available
* keep hover, focus, disabled, and selection states readable
* make dialogs feel consistent with the rest of the app
* avoid introducing UI elements that look like default unstyled Swing components

For UI changes:

* keep components aligned and visually balanced
* avoid overloading screens with too many controls
* prefer discreet actions for secondary links
* keep dialogs clear and professional
* preserve usability for non-technical business users

The app should feel like a finished professional desktop product, not an internal tool.

## Validation and error handling

Validation should be clear, actionable, and easy to understand.

When possible, validation errors should explain:

* the field involved
* the invalid value or missing value
* the reason it is invalid
* what the user should fix

Do not overclaim bank acceptance. Keep wording precise and honest.

Financial/payment file generation should fail safely when required data is invalid.

## Testing

Add or update tests whenever the change is meaningful and testable.

Tests are especially important for:

* validation logic
* XML generation
* file parsing
* date handling
* amount handling
* IBAN/BIC/SIRET checks
* edge cases and invalid input
* regression-prone behavior

Prefer small focused tests that clearly document expected behavior.

Do not skip tests for core generation or validation changes unless there is a clear reason.

## Local preferences and configuration

For small local desktop preferences, prefer simple local persistence such as `java.util.prefs.Preferences`.

Keep local settings simple, transparent, and easy to reset.

Do not store sensitive payment files or payment data unless explicitly requested.

## Release and documentation

Keep version numbers consistent when release-related changes are requested.

README, release notes, and UI wording must match the real implemented behavior.

Avoid exaggerated claims.

Maintain Apache-2.0 license notices and attribution requirements.

## When to ask before changing

Ask for confirmation before:

* changing project architecture significantly
* adding a new dependency
* adding networking behavior
* changing generated XML structure
* changing licensing or attribution
* introducing paid/pro edition separation
* removing existing features
* making broad refactors unrelated to the task

## General behavior

Implement requested changes cleanly and completely.

When the task requires several changes, prefer a structured approach:

1. identify affected classes
2. make the smallest coherent design change
3. add or update tests where meaningful
4. verify build/tests
5. summarize what changed

Do not only patch the symptom if the underlying design problem is small and safe to improve.
