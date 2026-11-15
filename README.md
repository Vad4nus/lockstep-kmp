# Lockstep

Flows for Kotlin Multiplatform ViewModels with native Swift types on iOS.

`UiStateFlow<T>` and `UiFlow<T>` are plain `StateFlow<T>` / `Flow<T>` on Android. On iOS a SKIE sub-plugin generates, at framework link time, Swift structs for Kotlin data classes and Swift enums for sealed types, and compiles them into the same framework. Swift reads `viewModel.state.value.name` and `for await action in viewModel.actions`, with no handwritten mirror, mapping, unboxing or `onEnum(of:)`.

## Modules

| Module | What it is | Depends on |
| --- | --- | --- |
| `lockstep` | `UiStateFlow`, `UiFlow` | coroutines |
| `lockstep-result` | `AppResult`, `AppError`, `ErrorMapper` for the data layer | moko-resources |
| `lockstep-mvvm` | `MvvmViewModel`, `ScreenChrome`, `StackNavigator` | `lockstep`, `lockstep-result`, androidx ViewModel |
| `lockstep-ktor` | `HttpClient.call` that returns `AppResult` | `lockstep-result`, Ktor client |
| `lockstep-screens` | screens as a tree of nodes, a DSL and JSON | kotlinx-serialization |
| `lockstep-compose` | Android only: `MvvmScreen`, `MvvmSheet`, `NavigatorDisplay`, `RenderNode` | `lockstep-mvvm`, `lockstep-screens`, Material 3, Navigation 3 |
| Gradle plugin `io.github.vad4nus.lockstep` | the Swift generator, a SKIE sub-plugin | SKIE |

`lockstep-compose` is one module on purpose: splitting the screen host from the node renderer would save an app that uses only one of them a dependency on a small Kotlin-only module, at the price of a second artifact to version. Split it when one half gains a heavy dependency of its own.

## Gradle

In the module that builds the iOS framework, next to SKIE:

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.skie)
    id("io.github.vad4nus.lockstep")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api("io.github.vad4nus:lockstep-mvvm:0.1.0")
        }
    }
}
```

In the root `build.gradle.kts`, declare the plugins once so they share a classloader:

```kotlin
plugins {
    alias(libs.plugins.skie) apply false
    id("io.github.vad4nus.lockstep") version "0.1.0" apply false
}
```

The Android app adds `implementation("io.github.vad4nus:lockstep-compose:0.1.0")`.

- The plugin and the libraries share one version, and the generated Swift calls the libraries. The plugin fails dependency resolution when a `lockstep` library of another version is requested, directly or transitively.
- The plugin and the libraries are on Maven Central, not on the Gradle Plugin Portal: `mavenCentral()` has to be in `pluginManagement.repositories` as well as in `dependencyResolutionManagement.repositories`.
- To try a local build, `./gradlew publishToMavenLocal` and `./gradlew -p lockstep-plugin publishToMavenLocal` put the artifacts into `~/.m2`; add `mavenLocal()` to the same two blocks.
- `sample/` uses the sources instead: `includeBuild("lockstep-plugin")` in `settings.gradle.kts` and `api(project(":lockstep"))`.

## Kotlin

```kotlin
import io.github.vad4nus.lockstep.asUiFlow
import io.github.vad4nus.lockstep.asUiStateFlow

abstract class BaseViewModel<S : Any, A : Any>(initial: S) {
    private val stateCell = MutableStateFlow(initial)
    private val actionChannel = Channel<A>(Channel.BUFFERED)

    val state: UiStateFlow<S> = stateCell.asUiStateFlow()
    val actions: UiFlow<A> = actionChannel.receiveAsFlow().asUiFlow()
}
```

`subscribe` delivers on `Dispatchers.Main.immediate`, and reports completion when the flow ends, fails or its source is cancelled. Android needs `kotlinx-coroutines-android`, which the library already brings in. `lockstep-mvvm` has this base class ready, see [Base layer](#base-layer).

## Swift

```swift
@State private var state: UiState<ProfileState>

init(viewModel: ProfileViewModel) {
    _state = State(wrappedValue: viewModel.state)
}

Text(state.value.address?.city ?? "-")

switch state.value.screen {
case .idle: Text("Idle")
case .loading: ProgressView()
case .error(let message, let code): Text("\(message) (\(code.map { String($0) } ?? "-"))")
}

.task {
    for await action in viewModel.actions {
        switch action {
        case .navigateBack: ...
        case .showToast(let text): ...
        }
    }
}
```

- `state` is a `UiState<Value>` (an `Observable` class with one observed property, `value`), cached per ViewModel instance. An emission is mapped to Swift once, on the main thread, and notifies observers; an emission that is the very same Kotlin object as the previous one is skipped. The whole value is mapped each time, so keep a large part that changes rarely, such as a screen tree, in a flow of its own.
- `actions` is an `AsyncSequence`. Each iteration subscribes anew, and the sequence finishes when the Kotlin flow completes, fails or is cancelled. A flow backed by a channel hands each event to one subscriber, so iterate it in one place.
- The original Kotlin properties stay available as `__state` and `__actions`.
- Generated structs also get a memberwise initializer, handy for previews and tests.
- Every generated struct keeps the Kotlin object it was mapped from in `kotlin` (`__ProfileState?`, nil when the struct is built in Swift). `==` and `hash` ignore it. A data class property named `kotlin` fails the build.
- A struct that can be turned back into Kotlin has `kotlinValue`: the stored Kotlin object, or a new one built from the fields. See [Lists, StringDesc and routes in Swift](#lists-stringdesc-and-routes-in-swift) for which types qualify.

## Functions

Functions of a data class (members and extensions on the class) are available on the struct as proxies that call the Kotlin object:

```swift
state.value.address?.label()
state.value.address?.isNear(other: other)
```

- On a struct with `kotlinValue` a proxy always runs: it calls the stored Kotlin object or one built from the fields, and returns the plain result type. This holds when its struct parameters have `kotlinValue` too.
- On any other struct a proxy returns an optional, `nil` when the struct was built in Swift; `Unit` functions do nothing in that case.
- Parameters and results of generated types are converted. A `toString()` you declare on a data class with `kotlinValue` becomes `description` (`CustomStringConvertible`).
- Proxies live in `LockstepProxies.swift`, next to `LockstepGenerated.swift`.
- Not proxied, with a build warning, and still reachable through `kotlin`: `suspend`, generic and throwing functions; nullable generated-struct parameters, generated-enum parameters or nullable-primitive parameters; functions whose name clashes with a field or with `kotlin`, or whose parameter name starts with `lockstep`; functions SKIE hides or replaces; a function whose Swift signature another proxy already has.
- Silently left out: functions the Kotlin compiler generates for a data class (`copy`, `equals`, `hashCode`, `componentN`, and `toString` unless you declare it), property accessors, inherited (fake override) functions, and static or top-level functions. Generated enums have no proxies.

## Type mapping

| Kotlin | Swift |
| --- | --- |
| data class | `struct`, `Equatable` when every field is |
| sealed class or interface | `enum`, cases are lowerCamel subclass names, associated values are labeled by subclass properties |
| `data object` / `object` subclass | case without payload |
| `Int?`, `Long?`, `Boolean?`, unsigned too | `Int32?`, `Int64?`, `Bool?`, `UInt32?` |
| nested data class or sealed type | generated type |
| `List<DataClass>`, `List<DataClass>?` | `[Struct]`, `[Struct]?` |
| moko `StringDesc` | `String` (read with `.localized()`) |
| everything else | passed through with the type SKIE already exposes: `Set`, `Map`, `enum class`, `Char` (as `UInt16`), non-data classes, generic data classes, third-party types, and fields that close a reference cycle made only of data classes (build warning) |

- Lists of generated types are converted element by element; `Set` and `Map` are passed through. A sealed type with unexposed or sealed-in-sealed subclasses is passed through with a build warning.
- Every property of a data class becomes a stored field of the struct, a computed one included: it is read once per emission and is a parameter of the Swift memberwise initializer, but `kotlinValue` passes only constructor parameters to Kotlin. Keep expensive work out of computed properties of state classes; a function is called on demand through its proxy.

## Flow accessors

- An accessor replaces a `UiStateFlow` or `UiFlow` property of a class or an `object`. It is generated once, on the first class in the hierarchy where the flow's type argument is concrete (a generic base class keeps `__state`), and a subclass does not repeat it. Sibling subclasses get their own accessor.
- The value may be a generated type, a list of generated types, a number or boolean (`UiState<Int32>`, `UiState<Bool>`), a `StringDesc` (a localized `String`), or anything SKIE bridges: `String`, an `enum class`, a collection, another class. A nullable value becomes an optional.
- No accessor, with a build warning, and the property stays a Kotlin flow you can `subscribe` to: a top-level property, a member of an interface or of an enum, a nullable flow, a flow whose values are not objects of a class.

## Naming

- A generated type takes the Swift name SKIE gives the Kotlin class. The Kotlin class itself is renamed to `__Name` (SKIE's replaced form), so `__ProfileState` is the Kotlin object and `ProfileState` is the struct.
- Kotlin already disambiguates same-named classes in a module (`Address`, `Address_`), and generated types follow those names.
- Swift keywords are backticked.

## Build errors and warnings

The plugin reports through the Kotlin compiler, so everything below shows up in the Gradle output of the framework link task, prefixed with `lockstep:`.

Errors:

- An exported class has a Swift name the emitted runtime uses: `UiState`, `UiEvents`; with `lockstep-mvvm` also `ViewModelOwner`, `MvvmScreenStyle`, `MvvmScreenModel`, `MvvmScreen`, `NavigationRoute`, `NavigatorStack`; with `lockstep-screens` also `NodeTheme`, `NodeView`. Rename the class.
- A data class has a property named `kotlin`.
- Two subclasses of a sealed type get the same case name.

Warnings:

- A flow gets no accessor (see above), with the reason.
- A function gets no proxy, with the reason.
- A sealed type or a field is passed through instead of being generated.
- A route type is not `Hashable` in Swift.
- An `MvvmViewModel` is exported without the types the SwiftUI runtime needs (`ViewModel`, `ViewModelStore`, `StackNavigator`, `Route`, the chrome types): the runtime is not emitted. Export `lockstep-mvvm` and `androidx.lifecycle.viewmodel`.
- `NodeView and NodeTheme are not generated`, with the reason, see [Screens described in KMP](#screens-described-in-kmp).

## Base layer

`lockstep-mvvm` is the shared base: an androidx `ViewModel` with state, chrome, actions and a request helper, plus a navigator. `AppResult` and `AppError` live in `lockstep-result`, which `lockstep-mvvm` re-exports, so a data-layer module can depend on the result type alone.

```kotlin
class OrdersViewModel(
    private val repo: OrdersRepository,
    private val navigator: StackNavigator,
) : MvvmViewModel<OrdersState, OrdersAction>(OrdersState()) {

    override fun onFirstAppear() = load()

    fun load() {
        request(call = { repo.orders() }, key = "load", errors = ErrorDisplay.Screen) {
            updateState { copy(orders = it) }
        }
    }

    fun refresh() {
        request(call = { repo.orders() }, loading = LoadingKind.Refresh, key = "load") {
            updateState { copy(orders = it) }
        }
    }

    fun onOrderClicked(id: Int) = navigator.push(OrdersRoute.Details(id))
}
```

- `state: UiStateFlow<S>` is the screen data. `chrome: UiStateFlow<ScreenChrome>` is everything the base screen draws around it, in separate fields: `loading` (`Blocking` or `Refresh`, null when idle), `backgroundLoading`, `error`, `messages` and `dialog`. A loader and a screen error do not overwrite each other. `actions: UiFlow<A>` carries screen effects only (scroll, focus) and queues them without a limit until the screen collects them. Messages, errors and dialogs are state, not actions, so none is lost while the screen is not collected.
- `request(call, loading, key, policy, errors, onError, onFinally, onSuccess)` runs a `suspend () -> AppResult<T>` in `viewModelScope`.
  - `loading` is `Blocking`, `Background`, `Refresh` or null. Loaders are counted, so parallel requests keep the loader until the last one ends.
  - `key` names the request, and `RequestPolicy` (`CancelPrevious`, `KeepRunning`, `Parallel`) says what happens when one with the same key is still running. Without a key every call runs on its own.
  - `errors` is `Message`, `Screen`, `Dialog` or `Silent`. A screen error replaces the content and offers a retry, so use it for the first load and keep `Message` for a refresh of data already shown. It goes away on `retry()` or when a request with the same key starts; `retry()` reruns every request that put a screen error up.
  - An exception thrown by `call`, `onSuccess` or `onError` is mapped to an `AppError` and handled like any failure. `onFinally` also runs for a cancelled request.
  - Override `handleError(error, display)` for app-wide rules, such as opening the login screen on `Unauthorized`.
- `showMessage(text, kind, action, durationMillis)` queues a message, 4 seconds by default and 8 with an action. `showDialog(...) { result -> }` opens a dialog; one dialog is open at a time, and a dialog replaced by a newer one gets `Dismiss`. `onFirstAppear`, `onAppear`, `onDisappear` are lifecycle hooks the base screen calls: the screen is opened, uncovered or the app returns to the foreground, on both platforms.
- Call a ViewModel from the main thread only.
- `AppResult<T>` is `Success` or `Failure(AppError)` with `map`, `mapError`, `flatMap`, `fold`, `recover`, `onSuccess`, `onFailure`, `getOrNull`, `getOrElse`, `ensureNotNull`, `AppResult.zip` and `AppResult.combine`. `catching(mapper) { }` turns exceptions into failures, rethrows cancellation and leaves `Error`s alone. `AppError` is `NoConnection`, `Timeout`, `Unauthorized`, `Http`, `Serialization`, `Unknown` or `Custom`.
- `MvvmEnvironment(errorMapper, texts, errorPresenter, errorReporter)` holds the app-wide rules: `errorMapper` turns throwables into `AppError`, `texts` (`MvvmTexts`) are the strings the base layer shows (`ok`, `retry`, one per error kind; English by default), `errorPresenter` turns an `AppError` into a `UiError` and never shows the diagnostics of a `Serialization` or `Unknown` error, and `errorReporter` receives every failure before it is shown, for logs and crash reports. Replace `MvvmEnvironment.Default` once at startup, or pass an environment to a ViewModel constructor.
- `StackNavigator(root)` holds a `NavStack(root, path, sheet)`: `push`, `pop`, `popTo` (false when the route is not in the stack), `replaceAll`, `setPath`.
  - Routes are `data class` or `data object`: they are compared by value. A route may be in the stack more than once, and each occurrence is a screen of its own, with its own saved state and ViewModel, on both platforms.
  - `popWithResult(key, value)` hands a result to the screen below and `results(key)` receives it, once, in the collector that started last. A result nobody collects is dropped at the next stack change. A `ResultKey` is matched by identity: declare it once as a top-level `val`.
  - Platforms report system gestures through `onSystemPop(newPath)`, applied only when `newPath` is a shorter beginning of the current path, and `onSystemDismissSheet(expected)`.

## Lists, StringDesc and routes in Swift

- `List<DataClass>` becomes `[Struct]`, nullable lists stay optional. Recursive types map natively: a list of the type itself is fine, and a cycle through a sealed type makes that enum `indirect`.
- A moko `StringDesc` field becomes a Swift `String` (`.localized()`). The value is read when the state is emitted, so a locale change shows on the next emission.
- Every route is a case of one exported sealed type that implements `Route` (directly, through `SheetRoute` or through an app interface). Such a type becomes a `Hashable` Swift enum with `kotlinValue` (Swift to Kotlin) and `init?(route:)` (Kotlin to Swift). Payloads must be generated types, scalars, strings or Kotlin classes; a route with an interface, `Any`, generated-list or `StringDesc` field stays non-hashable with a build warning.
- A standalone non-sealed route class is not supported in Swift: keep one sealed route type per navigator.
- A generated type has `kotlinValue`, so Swift can hand it back to a ViewModel, when Swift can call its primary constructor, every constructor parameter is a property, and every field is a scalar, a string, a passed-through type, another such type, or a list of such types. A `StringDesc` field qualifies when `moko.resources` is exported from the framework; it goes back as a raw string. Initializer labels follow the Kotlin constructor, so a property Kotlin/Native renames in Swift (`description`) still works.
- A property that every case of a sealed type has with the same type (`id` on every `Node`) becomes a computed property of the enum.

## Compose

`lockstep-compose` is Android-only Jetpack Compose (compileSdk 36, minSdk 24, Navigation 3 1.1.7).

```kotlin
@Composable
fun OrdersListScreen() {
    val viewModel = viewModel { OrdersViewModel(graph.repo, graph.navigator) }
    MvvmScreen(
        viewModel = viewModel,
        screenName = "orders",
        onRefresh = viewModel::refresh,
        topBar = { TopAppBar(title = { Text("Orders") }) },
    ) { state, padding ->
        LazyColumn(contentPadding = padding) { items(state.orders) { Text(it.title) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() = NavigatorDisplay(graph.navigator) {
    entry<OrdersRoute.List> { OrdersListScreen() }
    entry<OrdersRoute.Details> { OrderDetailsScreen(it.id) }
}
```

- `MvvmScreen` collects `state` and `chrome` with `collectAsStateWithLifecycle`, draws what `chrome` holds, and calls `screenAppeared`/`screenDisappeared` on start and stop.
  - Layout slots: `topBar`, `bottomBar`, `utilityBar`, `floatingActionButton`. `onRefresh` adds pull to refresh, driven by `LoadingKind.Refresh`; `onBack` installs a back handler; `imeAware` lifts the frame above the keyboard.
  - Chrome slots: `loading`, `backgroundLoading`, `error`, `snackbar`, `dialog`. Defaults live in `ScreenDefaults` and are replaceable per call; a custom snackbar reads `SnackbarData.messageKind`. The blocking loader covers the content area between the bars, and the default dialog marks a destructive confirm button.
  - A message stays for its `durationMillis`, extended by the accessibility timeout settings, and leaves the queue when the time is up, when its action is tapped or when its screen goes away.
  - `screenName` is reported to `LocalScreenTracker` each time the screen appears.
- `ScreenScaffold` is the same frame without a ViewModel, with `snackbarHost` and `overlay` slots.
- `NavigatorDisplay` renders a `StackNavigator` with Navigation 3. Each back-stack entry gets its own saved state and `ViewModelStore`, a repeated route included, so a ViewModel is cleared when its entry is popped.
- Navigation 3 1.2.0 needs AGP 9.1 and compileSdk 37, so the build pins 1.1.7 (AGP 8.13, compileSdk 36). Raise both together.

## SwiftUI

When the framework contains an `MvvmViewModel`, the plugin also emits `LockstepMvvmRuntime.swift` (iOS 17, Observation). A framework without `lockstep-mvvm` gets no SwiftUI code.

```swift
struct OrdersListScreen: View {
    @State private var owner = ViewModelOwner {
        OrdersViewModel(repo: graph.repo, navigator: graph.navigator)
    }

    var body: some View {
        let viewModel = owner.viewModel
        MvvmScreen(viewModel, screenName: "orders", onRefresh: { viewModel.refresh() }) { state in
            List(state.orders, id: \.id) { Text($0.title) }
        }
        .navigationTitle("Orders")
    }
}

struct OrdersRoot: View {
    var body: some View {
        NavigatorStack(graph.navigator) { (route: OrdersRoute) in
            switch route {
            case .list: OrdersListScreen()
            case .details(let id): OrderDetailsScreen(id: id)
            case .filter(let preset): OrderFilterSheet(preset: preset)
            }
        }
    }
}
```

- `ViewModelOwner` creates the ViewModel on the first `viewModel` access (in `body`, never in `init`, because SwiftUI builds the view struct on every redraw) and clears its store on `deinit`. Hold it in `@State` of the screen view, not in the destination closure of `NavigatorStack`.
- Every exported concrete `MvvmViewModel` subclass conforms to the emitted `MvvmScreenModel` protocol, so `MvvmScreen(viewModel) { state in ... }` takes any of them, with the state and action types inferred.
  - It draws the blocking loader, the background loading line, the error view with retry, toasts and the alert from `chrome`. `onRefresh:` adds pull to refresh, which lasts while `chrome.loading` is `Refresh`. `onAction:` receives the screen actions.
  - It calls `screenAppeared`/`screenDisappeared` when the view appears or disappears and when the app enters the background or returns, so the hooks match Android. This also works inside a `UIHostingController`.
  - `screenName:` is reported to the `\.mvvmScreenTracker` environment value each time the screen appears.
  - Appearance is set with the `\.mvvmScreenStyle` environment value: `MvvmScreenStyle(loading:backgroundLoading:error:toast:)`. A toast is announced to VoiceOver and stays for its `durationMillis`.
- `NavigatorStack` builds a `NavigationStack` from a `StackNavigator`, reports swipe-back through `onSystemPop` and presents the sheet through `.sheet(item:)`.

## Bottom sheets

- A feature sheet (own ViewModel, own state) is a route that implements `SheetRoute` and is opened with `navigator.push(Route)`. `NavStack.sheet` holds it; at most one sheet is open. Pushing an equal sheet route does nothing, a different sheet route replaces it, pushing a screen route or calling `pop()` closes it.
- A swipe-down is reported with `onSystemDismissSheet(expected)`, which only clears the sheet when it still equals `expected`, so a late callback cannot close a newer sheet.
- Android: write the sheet with `MvvmSheet(viewModel) { state -> ... }` and register it as an entry. It has the chrome slots of `MvvmScreen` (`SheetDefaults` for the loader and the error view), draws the blocking loader over the content and the snackbar over its bottom edge, and owns its snackbar host. `NavigatorDisplay` takes `sheetProperties`.
- iOS: write the sheet with the same `MvvmScreen` and add `.presentationDetents` inside the `destination` closure; its `@State ViewModelOwner` is cleared when the sheet goes away.
- A local sheet that only toggles a flag on its screen needs none of this: keep a boolean in the screen state.

## Screens described in KMP

`lockstep-screens` describes a screen as a tree of `Node`s, built with a DSL or decoded from JSON, and draws it with native components on both platforms. Add `api` and `export` of `lockstep-screens` in the framework module.

```kotlin
val login = screen("login") {
    column("root", spacing = Space.M, style = Style(padding = Space.M)) {
        text("title", "Sign in", TextStyleToken.Title, a11y = A11y(heading = true))
        input("email", "email", "Email", InputKind.Email, listOf(InputRule.Required("Enter your email")))
        button("submit", "Sign in", NodeEvent.Submit)
    }
}

val served = ScreenSpec.decode(json) { violation -> logger.warn(violation.message) }
```

- Nodes: column, row, box (all three tappable when they carry an `event`), lazy list, spacer, text, image, button, input, toggle, `Custom` (a native piece the app registers by key) and `Unknown`. Spacing, colors and text styles are tokens each platform resolves, dark mode included.
- Decoding is lenient, so a screen written for a newer app version still draws in an older one. An unknown node type or a node with broken content becomes `Unknown` (it draws its `fallback` or nothing), an unknown event or input rule becomes `NodeEvent.Unknown` or `InputRule.Unknown`, and an unknown token value falls back to the default. `ScreenSpec.schema` is compared with `ScreenSpec.SCHEMA`, and a newer schema is reported. `decode` throws only when the JSON is not a screen at all or is nested deeper than 128 levels.
- Layout rules: a tree that crashes or collapses on a platform is rejected. That covers a vertical scroll or lazy list inside a vertical one (and the same horizontally), a flexible spacer outside a column or row that is bounded on its axis, and siblings sharing an id. In the DSL the first three do not compile: the call resolves to an overload deprecated with level ERROR whose message names the platform failure, and the IDE quick-fix offers the safe form. A lazy list with a fixed `height` or `width` may sit inside a scroll on its axis.
- Duplicate ids, and every rule for JSON, are checked when the tree is built or decoded, together with two inputs bound to one field, unknown events and rules, and a pattern that does not compile. Each problem goes to the `onViolation` handler and the returned tree is repaired so that it draws: a nested scroll stops scrolling, a nested lazy list becomes a column or row, a flexible spacer in a scroll becomes fixed, repeated ids are made unique. The default handler throws, which suits development; pass a logging one in release builds. `ScreenSpec.checked()` and `Node.violations()` run the same check on a tree you built yourself.
- Form state: inputs and toggles keep their values in `FormState` by field, so the tree stays the same while the user types. The ViewModel applies events with `form.reduce(event, root)`, and `NodeEvent.Submit` runs the inputs' rules. `FormState.toString()` hides the values, so a password does not reach a log.
- Android: `RenderNode(node, form, onEvent)` from `lockstep-compose`; tokens, images and custom pieces come from `LocalNodeTheme`. Create the `NodeTheme` once (`remember`).
- iOS: the plugin emits a SwiftUI `NodeView(node, form:, onEvent:)`, with tokens, images and custom pieces in `\.nodeTheme`, when `Node`, `NodeEvent`, `FormState`, `Style` and `A11y` reach Swift through a state flow as generated types under their own names, and the token enums (`Space`, `ColorToken`, `TextStyleToken`, `ButtonVariant`, `InputKind`, `HAlign`, `VAlign`, `ListAxis`) keep theirs. When an app class takes one of these names, Kotlin/Native renames one of the two, and the build then warns that `NodeView` is not generated and names the type. Send events back with `viewModel.onEvent(event: event.kotlinValue)`.
- An image or a `Custom` key the theme does not have is logged on both platforms (`Log.w`, `NSLog`); a missing image keeps its place on screen.
- A new `Node` type without a renderer fails both builds: the Compose `when` and the emitted Swift `switch` are exhaustive.

## Ktor

`lockstep-ktor` maps Ktor to `AppResult`. It depends on `lockstep-result` only, so a data-layer module does not pull in ViewModels.

```kotlin
val client = HttpClient { installLockstepDefaults(retries = 2) }

suspend fun orders(): AppResult<List<Order>> = client.call { url("https://api.example.com/orders") }
```

- `call<T>(parser, block)` returns `Success(body)` or `Failure(AppError)`: 401 is `Unauthorized`, other non-2xx is `Http(code, message, details)`, timeouts are `Timeout`, lost connections are `NoConnection`, decoding errors are `Serialization`. Cancellation is rethrown. A response without a body decodes only as `Unit`.
- `ErrorBodyParser` reads the server's error body, for 401 too: a 401 always comes out as `Unauthorized`, and keeps the message the parser found. The default parser reads nothing, and the error presenter then shows the text for the error kind, not the HTTP status phrase.
- `installLockstepDefaults(retries, retryMethods)` installs `HttpRequestRetry` with a growing delay for server errors and lost connections, and only for `retryMethods`: `GET`, `HEAD` and `OPTIONS` by default, because a repeated `POST` could create a second order. Timeouts are not retried. Install `HttpTimeout` and `ContentNegotiation` yourself. `KtorErrorMapper` is the `ErrorMapper` for throwables outside `call`.

## Migrating from a hand-written base layer

| Hand-written | Lockstep |
| --- | --- |
| `BaseViewModel<State, Action>` (moko-mvvm or androidx) | `MvvmViewModel<S, A>` (androidx) |
| `CStateFlow`, `StateFlow<ScreenState<State>>` | `UiStateFlow<S>`, a native Swift struct on iOS |
| action `Channel` | `UiFlow<A>`, screen effects only |
| base error helpers (`showError()`, `dismissError()`, `onError`) | `request(errors = ...)` and an overridable `handleError` |
| per-screen `Loading/Error/Idle` state | `ScreenChrome.loading`, `backgroundLoading` and `error` |
| a message manager or an event bus for snackbars | `showMessage()` and the `chrome.messages` queue |
| confirm-dialog actions and flags | `showDialog(...) { result -> }` |
| custom result types (`Result<T, E>`, `NetworkResult`) | `AppResult<T>` and `AppError` |
| request helpers that cast the response body | `lockstep-ktor` `call<T>()` |
| navigation manager, service or router singletons | `StackNavigator` with Navigation 3 and `NavigatorStack` |
| a base screen composable or SwiftUI wrapper | `MvvmScreen` / `ScreenScaffold` |
| state holders that are never cleared | a `ViewModelStore` per back-stack entry (Android), `ViewModelOwner` (iOS) |
| Swift mirror types plus subscription bags | generated structs and the emitted `MvvmScreen` |
| screen-opened callbacks | `onFirstAppear` / `onAppear` / `onDisappear` |

## New screen recipe

1. Contract (commonMain): a `data class` state, a `sealed interface` of screen actions, and a route in the app's sealed route type (`SheetRoute` for a sheet).
2. ViewModel: extend `MvvmViewModel<State, Action>`, load in `onFirstAppear` through `request(...)`, navigate through the `StackNavigator`.
3. Android: a `MvvmScreen(viewModel) { state, padding -> ... }` (or `MvvmSheet`) and an `entry<Route>` in `NavigatorDisplay`.
4. iOS: a view holding `@State ViewModelOwner { ... }`, `MvvmScreen(owner.viewModel) { state in ... }`, and a `case` in the `NavigatorStack` destination closure.
5. Export `lockstep`, `lockstep-result`, `lockstep-mvvm`, `androidx.lifecycle.viewmodel` and `moko.resources` from the iOS framework, and `lockstep-screens` when the app uses it (see `sample/shared/build.gradle.kts`).

The working example is the Orders flow in `sample/` (`orders/` in `shared`, `OrdersScreens.kt` on Android, `OrdersScreens.swift` on iOS).

## Compatibility

| | |
| --- | --- |
| Kotlin | 2.4.10 |
| SKIE | 0.10.14 exactly: the build fails with a clear message on any other version |
| kotlinx.coroutines | 1.11.0 (`UiStateFlow` implements `StateFlow`, which coroutines marks experimental for inheritance, so build against this version) |
| Other libraries | androidx lifecycle 2.10.0, moko-resources 0.27.1, Ktor 3.6.0, kotlinx-serialization 1.11.0 |
| iOS runtime | 17.0+ (Observation; `UiState`, `UiEvents` and the emitted SwiftUI runtime are `@available(iOS 17.0, *)`) |
| Android UI | compileSdk 36, minSdk 24, Navigation 3 1.1.7 (1.2.0 needs AGP 9.1 and compileSdk 37) |
| Integration | direct Xcode integration (`embedAndSignAppleFrameworkForXcode`), one static umbrella framework; CocoaPods is untested |

The sub-plugin builds on SKIE internals (`SkieSubplugin`, `SkiePluginRegistrar`, KIR/SIR phases), which are undocumented and not stable across SKIE releases, so one plugin version supports one SKIE version. Upgrade SKIE and the plugin together and run the sample tests. To try another SKIE version before the plugin supports it, set `lockstep.allowUnsupportedSkie=true` in `gradle.properties`: the failure becomes a warning and the result is yours to check.

## Sample

```bash
./gradlew :sample:shared:linkDebugFrameworkIosX64
xcodegen generate --spec sample/iosApp/project.yml
xcodebuild test -project sample/iosApp/Sample.xcodeproj -scheme Sample -destination 'platform=iOS Simulator,name=iPhone 17'
```

On Apple Silicon use the `iosSimulatorArm64` link task. The scheme runs the unit tests and the UI tests. The same screens on Android (Compose, `collectAsStateWithLifecycle` and `actions.collect`):

```bash
./gradlew :sample:androidApp:installDebug
```

## Checks

Run targeted tasks; a bare `./gradlew test` builds every variant of every module.

```bash
./gradlew --continue :lockstep:testAndroidHostTest :lockstep:iosX64Test :lockstep-result:testAndroidHostTest :lockstep-result:iosX64Test
./gradlew --continue :lockstep-mvvm:testAndroidHostTest :lockstep-mvvm:iosX64Test :lockstep-ktor:testAndroidHostTest :lockstep-ktor:iosX64Test
./gradlew --continue :lockstep-screens:testAndroidHostTest :lockstep-screens:iosX64Test :lockstep-compose:testDebugUnitTest
./gradlew -p lockstep-plugin :lockstep-skie-plugin:test :lockstep-gradle-plugin:test
./gradlew checkKotlinAbi
scripts/check-generated-swift.sh
scripts/typecheck-swift.sh sample/shared/build/bin/iosX64/debugFramework sample/swift-check/Generated.swift
```

- On Apple Silicon replace `iosX64` with `iosSimulatorArm64`; the two scripts pick the architecture of the machine.
- `checkKotlinAbi` compares the public API of every library with the dumps in `<module>/api`. After an intended API change run `./gradlew updateKotlinAbi` and review the diff.
- `scripts/check-generated-swift.sh` compares the Swift generated for the sample with `sample/swift-check/expected`, so a change in the generator shows up as a diff. Link the sample framework first; `--update` accepts the new output.
- `.github/workflows/ci.yml` runs the same set on a macOS runner.

## Not yet

Swift-to-Kotlin mapping of ViewModel parameters, proxies for `suspend` functions, conversion of sets and maps, `kotlinx-datetime`, results addressed to the screen that asked for them, saving the navigation stack across process death, screenshot tests.
