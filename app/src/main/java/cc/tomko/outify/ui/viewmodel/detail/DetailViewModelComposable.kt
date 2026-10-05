package cc.tomko.outify.ui.viewmodel.detail

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import javax.inject.Inject
import javax.inject.Singleton

private var _detailViewModelStore: DetailViewModelStore? = null

fun setDetailViewModelStore(store: DetailViewModelStore) {
    _detailViewModelStore = store
}

fun getDetailViewModelStore(): DetailViewModelStore? = _detailViewModelStore

@Singleton
class DetailViewModelStore @Inject constructor() {
    private val viewModelInstances = mutableMapOf<String, ViewModel>()

    fun <T : ViewModel> get(key: String): T? {
        @Suppress("UNCHECKED_CAST")
        return viewModelInstances[key] as? T
    }

    fun <T : ViewModel> set(key: String, viewModel: T) {
        viewModelInstances[key] = viewModel
    }

    fun remove(key: String) {
        viewModelInstances.remove(key)
    }

    fun clear() {
        viewModelInstances.clear()
    }

    fun keys(): Set<String> = viewModelInstances.keys
}

@Composable
inline fun <reified VM : ViewModel> rememberDetailViewModel(
    key: String
): VM {
    val store = getDetailViewModelStore()

    return if (store != null) {
        val existing = store.get<VM>(key)
        if (existing != null) {
            return existing
        }

        val newViewModel = hiltViewModel<VM>(key = key)
        store.set(key, newViewModel)
        newViewModel
    } else {
        hiltViewModel<VM>()
    }
}
