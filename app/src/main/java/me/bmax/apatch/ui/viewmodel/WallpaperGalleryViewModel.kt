package me.bmax.apatch.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.bmax.apatch.ui.wallpaper.WallpaperCatalog
import me.bmax.apatch.ui.wallpaper.WallpaperDevice
import me.bmax.apatch.ui.wallpaper.WallpaperItem
import me.bmax.apatch.ui.wallpaper.WallpaperProvider
import me.bmax.apatch.ui.wallpaper.WallpaperProviderRegistry

data class WallpaperUiState(
    val providers: List<WallpaperProvider> = emptyList(),
    val selectedProviderId: String? = null,
    val device: WallpaperDevice = WallpaperDevice.PHONE,
    val items: List<WallpaperItem> = emptyList(),
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
)

class WallpaperGalleryViewModel(private val app: Application) : AndroidViewModel(app) {

    private val catalog = WallpaperCatalog()
    private val seen = HashSet<String>()

    private val _state = MutableStateFlow(WallpaperUiState())
    val state: StateFlow<WallpaperUiState> = _state.asStateFlow()

    init {
        val providers = runCatching { WallpaperProviderRegistry.load(app) }.getOrDefault(emptyList())
        _state.update { it.copy(providers = providers, selectedProviderId = providers.firstOrNull()?.id) }
        refresh()
    }

    private fun currentProvider(): WallpaperProvider? {
        val state = _state.value
        return state.providers.firstOrNull { it.id == state.selectedProviderId }
            ?: state.providers.firstOrNull()
    }

    fun selectDevice(device: WallpaperDevice) {
        if (_state.value.device == device) return
        _state.update { it.copy(device = device) }
        refresh()
    }

    /** Retain measured proportions across scrolling, layout changes and activity rotation. */
    fun recordImageSize(item: WallpaperItem, width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        _state.update { state ->
            val existing = state.items.firstOrNull { it.url == item.url }
            if (existing == null || existing.width != null) state else state.copy(
                items = state.items.map {
                    if (it.url == item.url) it.copy(width = width, height = height) else it
                },
            )
        }
    }

    fun refresh() {
        val provider = currentProvider() ?: return
        val device = _state.value.device
        val hasItems = _state.value.items.isNotEmpty()
        seen.clear()
        _state.update {
            it.copy(
                loading = !hasItems,
                refreshing = hasItems,
                loadingMore = false,
                error = null,
                items = if (hasItems) it.items else emptyList(),
            )
        }
        viewModelScope.launch {
            runCatching { catalog.load(provider, device, PAGE_SIZE, seen) }
                .onSuccess { list ->
                    list.forEach { seen.add(it.url) }
                    _state.update { it.copy(loading = false, refreshing = false, items = list) }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(loading = false, refreshing = false, error = throwable.message ?: "failed")
                    }
                }
        }
    }

    fun loadMore() {
        val state = _state.value
        if (state.loading || state.loadingMore) return
        val provider = currentProvider() ?: return
        val device = state.device
        _state.update { it.copy(loadingMore = true) }
        viewModelScope.launch {
            runCatching { catalog.load(provider, device, PAGE_SIZE, seen) }
                .onSuccess { list ->
                    list.forEach { seen.add(it.url) }
                    _state.update { it.copy(loadingMore = false, items = it.items + list) }
                }
                .onFailure {
                    _state.update { it.copy(loadingMore = false) }
                }
        }
    }

    companion object {
        private const val PAGE_SIZE = 18

        fun Factory(app: Application): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                WallpaperGalleryViewModel(app) as T
        }
    }
}
