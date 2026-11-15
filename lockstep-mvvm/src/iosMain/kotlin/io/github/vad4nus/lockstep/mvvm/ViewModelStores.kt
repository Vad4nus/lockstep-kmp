package io.github.vad4nus.lockstep.mvvm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import kotlin.reflect.KClass

/** The ViewModel kept under [key], made by [factory] on the first call. The generated Swift `ViewModelOwner` calls it. */
@Suppress("UNCHECKED_CAST")
public fun <VM : ViewModel> ViewModelStore.getOrPut(key: String, factory: () -> VM): VM {
    val provider = ViewModelProvider.create(
        this,
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T = factory() as T
        },
    )
    return provider.get(key, ViewModel::class) as VM
}
