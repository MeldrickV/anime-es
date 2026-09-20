package com.zhuchii.anies.data

import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.zhuchii.anies.AnieEsApp
import com.zhuchii.anies.scraper.AnimeFlvScraper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Ejecuta el embed del servidor de AnimeFLV en un WebView oculto para capturar
 * la URL de video. Los hosts actuales (bysesukior, dooodster, ...) sirven una
 * SPA de JS que resuelve el .m3u8/.mp4 por XHR/fetch, asi que el HTML no trae
 * ninguna URL directa (el `extraerUrlVideo` del Bash ya no sirve; tampoco en
 * el CLI).
 *
 * La captura se hace en [shouldInterceptRequest]: cualquier peticion de
 * subrecurso cuya URL contenga .m3u8/.mp4 es la del video; con fallback de
 * `video.currentSrc` cuando el reproductor usa el elemento <video> nativo.
 * El WebView se reutiliza como singleton (evita leaks y cold-start) y la
 * espera tiene timeout por si el embed no resuelve nada.
 */
object ResolverEmbedWebView {

    private val urlVideo = Regex(""".*\.(?:m3u8|mp4)(?:\?.*)?$""")
    private const val TIMEOUT_MS = 12_000L
    private const val ESPERA_JS_MS = 3_000L

    @Volatile
    private var instancia: WebView? = null

    private fun webView(): WebView {
        instancia?.let { return it }
        return synchronized(this) {
            instancia ?: WebView(AnieEsApp.context()).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.userAgentString = AnimeFlvScraper.USER_AGENT
                settings.mediaPlaybackRequiresUserGesture = false
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                resumeTimers()
                instancia = this
            }
        }
    }

    /** Devuelve la URL .m3u8/.mp4 que resuelve el embed, o null si no aparece. */
    suspend fun resolver(embedUrl: String, referer: String): String? =
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                var encontrada = false
                val view = webView()
                view.stopLoading()

                fun reportar(url: String) {
                    if (encontrada) return
                    if (!urlVideo.matches(url)) return
                    encontrada = true
                    view.stopLoading()
                    if (cont.isActive) cont.resume(url)
                }

                view.webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest,
                    ): WebResourceResponse? {
                        request.url?.toString()?.let { if (it.contains(".m3u8") || it.contains(".mp4")) reportar(it) }
                        return null
                    }
                }

                view.webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView, newProgress: Int) {
                        if (newProgress != 100) return
                        // Algunos reproductores cargan el video despues del load().
                        view.postDelayed({
                            view.evaluateJavascript(
                                """(function () {
                                      var v = document.querySelector('video');
                                      if (!v) return '';
                                      return v.currentSrc || (v.querySelector('source') || {}).src || '';
                                    })();""",
                            ) { resultado ->
                                val limpio = (resultado ?: "").trim().removePrefix("\"").removeSuffix("\"")
                                if (limpio.startsWith("http")) reportar(limpio)
                            }
                        }, ESPERA_JS_MS)
                    }
                }

                val handler = Handler(Looper.getMainLooper())
                val resolver = Runnable {
                    if (!encontrada) {
                        view.stopLoading()
                        if (cont.isActive) cont.resume(null)
                    }
                }
                handler.postDelayed(resolver, TIMEOUT_MS)

                cont.invokeOnCancellation {
                    handler.removeCallbacks(resolver)
                    view.stopLoading()
                }

                view.loadUrl(embedUrl, mapOf("Referer" to referer))
            }
        }
}