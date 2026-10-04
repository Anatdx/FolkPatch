package me.bmax.apatch.ui.wallpaper

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WallpaperCatalogTest {
    private lateinit var server: HttpServer
    private lateinit var client: OkHttpClient
    private lateinit var provider: WallpaperProvider
    private val selections = AtomicInteger()
    private var redirect = true
    private var failImage = false
    private var sameImage = false

    @Before
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            exchange.use {
                when (exchange.requestURI.path) {
                    "/random" -> {
                        assertTrue(exchange.requestURI.query.contains("pool=phone"))
                        val choice = if (sameImage) 1 else selections.incrementAndGet()
                        if (redirect) {
                            exchange.responseHeaders.add("Location", "/redirect/$choice")
                            exchange.sendResponseHeaders(302, -1)
                        } else {
                            val bytes = "random image $choice".toByteArray()
                            exchange.sendResponseHeaders(200, bytes.size.toLong())
                            exchange.responseBody.write(bytes)
                        }
                    }
                    else -> {
                        val choice = exchange.requestURI.path.substringAfterLast('/')
                            .substringBefore('.')
                        if (exchange.requestURI.path.startsWith("/redirect/")) {
                            exchange.responseHeaders.add("Location", "/images/$choice.png")
                            exchange.sendResponseHeaders(302, -1)
                        } else if (failImage) {
                            exchange.sendResponseHeaders(503, -1)
                        } else {
                            val bytes = "fixed image $choice".toByteArray()
                            exchange.responseHeaders.add("Content-Type", "image/png")
                            exchange.sendResponseHeaders(200, bytes.size.toLong())
                            exchange.responseBody.write(bytes)
                        }
                    }
                }
            }
        }
        server.start()
        client = OkHttpClient()
        provider = WallpaperProvider(
            id = "random", name = "Random", homepage = "",
            baseUrl = "http://127.0.0.1:${server.address.port}",
            devicePaths = mapOf("phone" to "/random"),
            responseType = "redirect", urlField = "", query = mapOf("pool" to "phone"),
            idParam = null,
        )
    }

    @After
    fun tearDown() {
        server.stop(0)
        client.connectionPool.evictAll()
        client.dispatcher.executorService.shutdown()
    }

    @Test
    fun thumbnailPreviewAndDownloadKeepTheSameImageAfterRelativeRedirects() = runBlocking {
        val item = WallpaperCatalog(client).load(provider, WallpaperDevice.PHONE, 1, emptySet()).single()

        assertEquals("${provider.baseUrl}/images/1.png", item.url)
        assertTrue(item.fileName.endsWith(".png"))
        repeat(3) {
            client.newCall(Request.Builder().url(item.url).build()).execute().use { response ->
                assertArrayEquals("fixed image 1".toByteArray(), response.body.bytes())
            }
        }
        assertEquals(1, selections.get())
    }

    @Test
    fun deduplicatesAndExcludesResolvedImagesInsteadOfRandomEndpointNonces() = runBlocking {
        sameImage = true
        val catalog = WallpaperCatalog(client)
        val items = catalog.load(provider, WallpaperDevice.PHONE, 4, emptySet())

        assertEquals(1, items.size)
        assertTrue(catalog.load(provider, WallpaperDevice.PHONE, 4, setOf(items.single().url)).isEmpty())
    }

    @Test
    fun failedImageDoesNotExposeRandomEndpointAsFallback() = runBlocking {
        failImage = true
        assertTrue(WallpaperCatalog(client).load(provider, WallpaperDevice.PHONE, 1, emptySet()).isEmpty())
    }

    @Test
    fun nonRedirectingRandomResponseDoesNotExposeAnUnstableImageUrl() = runBlocking {
        redirect = false
        assertTrue(WallpaperCatalog(client).load(provider, WallpaperDevice.PHONE, 1, emptySet()).isEmpty())
    }
}
