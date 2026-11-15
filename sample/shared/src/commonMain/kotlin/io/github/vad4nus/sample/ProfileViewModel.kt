package io.github.vad4nus.sample

import io.github.vad4nus.lockstep.UiFlow
import io.github.vad4nus.lockstep.asUiFlow
import kotlinx.coroutines.flow.flowOf

class ProfileViewModel : BaseViewModel<ProfileState, ProfileAction>(ProfileState()) {

    fun onNameChanged(name: String) = updateState { copy(name = name) }

    fun onAgeChanged(age: Int) = updateState { copy(age = age) }

    fun onAddressChanged(city: String, zip: Int) = updateState { copy(address = Address(city, zip)) }

    fun onLoad() {
        updateState { copy(screen = ProfileScreenState.Loading) }
        launch { updateState { copy(screen = ProfileScreenState.Error("Failed", 42)) } }
    }

    fun onBack() = sendAction(ProfileAction.NavigateBack)

    fun onToast(text: String) = sendAction(ProfileAction.ShowToast(text))

    val finiteEvents: UiFlow<ProfileAction> = flowOf<ProfileAction>(ProfileAction.NavigateBack).asUiFlow()
}

abstract class ListViewModel : BaseViewModel<ListState, ProfileAction>(ListState())

class FavoritesViewModel : ListViewModel()

class SettingsViewModel : BaseViewModel<SettingsState, ProfileAction>(SettingsState())
