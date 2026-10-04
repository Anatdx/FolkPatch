package me.bmax.apatch.ui.wallpaper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import me.bmax.apatch.apApp
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import kotlin.random.Random

/**
 * Produces [WallpaperItem]s for a provider.
 *
 * If the provider declares a [DirectSource] for the device, items are built locally
 * from a random id (zero API calls, immune to rate limiting). Otherwise the
 * provider's dynamic endpoint is queried in parallel (the fallback path).
 */
class WallpaperCatalog {

    suspend fun load(
        provider: WallpaperProvider,
        device: WallpaperDevice,
        count: Int,
        exclude: Set<String>
    ): List<WallpaperItem> = withContext(Dispatchers.IO) {
        if (device == WallpaperDevice.MIXED) {
            val phoneCount = count / 2
            val phoneItems = load(provider, WallpaperDevice.PHONE, phoneCount, exclude)
            val tabletItems = load(
                provider,
                WallpaperDevice.TABLET,
                count - phoneCount,
                exclude + phoneItems.map { it.url }
            )
            return@withContext phoneItems + tabletItems
        }
        if (provider.responseType.equals("redirect", ignoreCase = true)) {
            return@withContext loadRedirect(provider, device, count, exclude)
        }
        provider.directFor(device.key)?.let {
            return@withContext loadDirect(it, provider, device, count, exclude)
        }
        loadRemote(provider, device, count, exclude)
    }

    /**
     * Redirect path: build cache-busted endpoint URLs locally; the client follows
     * the 302 to a fresh random image (one request per image, always valid).
     */
    private fun loadRedirect(
        provider: WallpaperProvider,
        device: WallpaperDevice,
        count: Int,
        exclude: Set<String>
    ): List<WallpaperItem> {
        val path = provider.devicePaths[device.key] ?: return emptyList()
        if (count <= 0) return emptyList()
        val base = provider.baseUrl + path
        val seen = HashSet<String>()
        val items = ArrayList<WallpaperItem>(count)
        var attempts = 0
        val maxAttempts = count * 4 + 8
        while (items.size < count && attempts < maxAttempts) {
            attempts++
            val nonce = System.nanoTime().toString(36) + Random.nextInt(0x1000000).toString(36)
            val url = "$base?_=$nonce"
            if (!seen.add(url) || url in exclude) continue
            items += WallpaperItem(
                id = nonce,
                url = url,
                providerId = provider.id,
                deviceKey = device.key
            )
        }
        return items
    }

    /** API-free path: pick random ids and build direct image URLs. */
    private fun loadDirect(
        source: DirectSource,
        provider: WallpaperProvider,
        device: WallpaperDevice,
        count: Int,
        exclude: Set<String>
    ): List<WallpaperItem> {
        val range = source.maxId - source.minId + 1
        if (range <= 0 || count <= 0) return emptyList()
        val seen = HashSet(exclude)
        val items = ArrayList<WallpaperItem>(count)
        val maxAttempts = count * 4 + 8
        var attempts = 0
        while (items.size < count && attempts < maxAttempts) {
            attempts++
            val idNum = Random.nextInt(source.minId, source.maxId + 1)
            val id = "img$idNum"
            if (!seen.add(id)) continue
            val url = source.imagePattern.replace("{id}", idNum.toString())
            items += WallpaperItem(
                id = id,
                url = url,
                providerId = provider.id,
                deviceKey = device.key
            )
        }
        return items
    }

    /** Fallback path: query the provider's dynamic endpoint concurrently. */
    private suspend fun loadRemote(
        provider: WallpaperProvider,
        device: WallpaperDevice,
        count: Int,
        exclude: Set<String>
    ): List<WallpaperItem> = coroutineScope {
        val semaphore = Semaphore(MAX_CONCURRENCY)
        val urls = (0 until count).map {
            async {
                semaphore.withPermit {
                    runCatching { fetchUrl(provider, device) }.getOrNull()
                }
            }
        }.awaitAll()
        val seen = HashSet<String>()
        urls.asSequence()
            .filterNotNull()
            .map { it.trim() }
            .filter { it.startsWith("http") && seen.add(it) && it !in exclude }
            .map {
                WallpaperItem(
                    id = it.substringAfterLast('/').substringBefore('?').substringBeforeLast('.', it),
                    url = it,
                    providerId = provider.id,
                    deviceKey = device.key
                )
            }
            .toList()
    }

    private fun fetchUrl(provider: WallpaperProvider, device: WallpaperDevice): String? {
        val path = provider.devicePaths[device.key] ?: return null
        val builder = StringBuilder(provider.baseUrl).append(path)
        if (provider.query.isNotEmpty()) {
            builder.append('?')
            builder.append(
                provider.query.entries.joinToString("&") { (key, value) ->
                    "$key=${URLEncoder.encode(value, "UTF-8")}"
                }
            )
        }
        val request = Request.Builder()
            .url(builder.toString())
            .header("Accept", "text/plain,*/*")
            .build()
        return apApp.okhttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body.string().trim()
            if (body.isEmpty()) return null
            when (provider.responseType.lowercase()) {
                "json" -> runCatching { JSONObject(body).optString(provider.urlField) }.getOrNull()
                else -> body
            }
        }
    }

    companion object {
        private const val MAX_CONCURRENCY = 3
    }
}
