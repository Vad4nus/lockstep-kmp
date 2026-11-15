# Changelog

Notable changes are recorded here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the versions follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html). The plugin and the libraries share one version.

## 0.1.0 - 2026-11-16

First version.

- `lockstep`: `UiStateFlow` and `UiFlow`, plain flows on Android that Swift reads as an observable `UiState` and an `AsyncSequence`.
- `lockstep-result`: `AppResult` and `AppError` for the data layer, with no dependency on ViewModels.
- `lockstep-mvvm`: `MvvmViewModel` with state, chrome (loaders, screen error, messages, dialog), one-shot actions and `request`, and `StackNavigator` with sheets and results.
- `lockstep-ktor`: `HttpClient.call`, which returns an `AppResult`, and retries limited to the methods that are safe to repeat.
- `lockstep-screens`: screens described as a tree of nodes, built with a DSL or decoded from JSON. Layouts that break a platform do not compile in the DSL and are repaired when decoded.
- `lockstep-compose`: `MvvmScreen`, `MvvmSheet`, `NavigatorDisplay` and `RenderNode` for Jetpack Compose.
- Gradle plugin `io.github.vad4nus.lockstep`: a SKIE sub-plugin that generates Swift structs, enums, flow accessors and function proxies, and the SwiftUI `MvvmScreen`, `NavigatorStack` and `NodeView`.
