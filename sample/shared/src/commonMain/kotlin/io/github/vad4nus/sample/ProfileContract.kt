package io.github.vad4nus.sample

data class ProfileState(
    val name: String = "",
    val age: Int? = null,
    val followers: Long = 0L,
    val rating: Double? = null,
    val verified: Boolean = false,
    val address: Address? = null,
    val tags: List<String> = emptyList(),
    val screen: ProfileScreenState = ProfileScreenState.Idle,
    val category: Category = Category.Personal,
    val categoryPairs: List<Pair<Int, Category>> = emptyList(),
    val categories: List<Category> = emptyList(),
    val categoryMap: Map<String, Category> = emptyMap(),
    val maybeCategory: Category? = null,
    val pairOrNull: Pair<Int, Category>? = null,
) {
    fun withName(name: String): ProfileState = copy(name = name)

    fun screenOrIdle(): ProfileScreenState = screen
}

data class Address(val city: String, val zip: Int) {
    fun label(): String = "$city $zip"

    fun copy(prefix: String): Address = copy(city = prefix + city)
}

sealed interface ProfileScreenState {
    data object Idle : ProfileScreenState
    data object Loading : ProfileScreenState
    data class Error(val message: String, val code: Int?) : ProfileScreenState
}

enum class Category { Personal, Business }

sealed interface ProfileAction {
    data object NavigateBack : ProfileAction
    data class ShowToast(val text: String) : ProfileAction
}

data class ListState(val items: List<String> = emptyList())

data class SettingsState(val darkMode: Boolean = false)
