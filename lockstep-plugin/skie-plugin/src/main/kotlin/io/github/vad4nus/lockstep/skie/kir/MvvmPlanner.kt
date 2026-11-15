package io.github.vad4nus.lockstep.skie.kir

import co.touchlab.skie.kir.KirProvider
import co.touchlab.skie.kir.element.KirClass
import co.touchlab.skie.util.KirReporter
import io.github.vad4nus.lockstep.skie.model.FlowKind

internal class MvvmPlan(
    val androidViewModel: KirClass,
    val store: KirClass,
    val navigator: KirClass,
    val route: KirClass,
    val chrome: KirClass,
    val uiError: KirClass,
    val uiMessage: KirClass,
    val navStack: KirClass,
    val viewModels: List<KirClass>,
)

private const val MVVM_PACKAGE = "$LOCKSTEP_PACKAGE.mvvm"

internal class MvvmPlanner(
    private val plan: ExtractionPlan,
    private val kirProvider: KirProvider,
    private val reporter: KirReporter,
) {
    fun plan(): MvvmPlan? {
        val viewModel = find("$MVVM_PACKAGE.MvvmViewModel") ?: return null
        val androidViewModel = find("androidx.lifecycle.ViewModel")
        val store = find("androidx.lifecycle.ViewModelStore")
        val navigator = find("$MVVM_PACKAGE.navigation.StackNavigator")
        val route = plan.route
        val chrome = generated("$MVVM_PACKAGE.ScreenChrome")
        val uiError = generated("$MVVM_PACKAGE.UiError")
        val uiMessage = generated("$MVVM_PACKAGE.UiMessage")
        val navStack = generated("$MVVM_PACKAGE.navigation.NavStack")
        if (androidViewModel == null || store == null || navigator == null || route == null ||
            chrome == null || uiError == null || uiMessage == null || navStack == null
        ) {
            reporter.warning("lockstep: MvvmViewModel is exported without ViewModel, ViewModelStore, StackNavigator, Route or the screen chrome types; export lockstep-mvvm and lifecycle-viewmodel to get the SwiftUI runtime", viewModel)
            return null
        }
        val viewModels = plan.accessors.filter { it.owner.inherits(viewModel) }.groupBy { it.owner }.filter { (_, group) ->
            fun has(name: String, kind: FlowKind) = group.any { it.property.kotlinName == name && it.flowKind == kind && !it.nullable }
            has("state", FlowKind.State) && has("chrome", FlowKind.State) && has("actions", FlowKind.Events)
        }.keys.toList()
        if (viewModels.isEmpty()) return null
        return MvvmPlan(androidViewModel, store, navigator, route, chrome, uiError, uiMessage, navStack, viewModels)
    }

    private fun find(fqName: String): KirClass? = kirProvider.findClassByFqName(fqName)

    private fun generated(fqName: String): KirClass? = find(fqName)?.takeIf { it in plan.generated }
}
