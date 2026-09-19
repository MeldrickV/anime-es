package com.zhuchii.anies

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.zhuchii.anies.data.AppDb
import com.zhuchii.anies.scraper.AnimeFlvScraper
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Application de la app: configura el ImageLoader global de Coil 3 con
 * OkHttp, mismo User-Agent de navegador que los scrapers, para que las
 * portadas de AnimeFLV (vww.animeflv.one) y J-Kanime (cdn.jkdesa.com) no
 * caigan en 403 por falta de User-Agent. Tambien inicializa la base Room.
 */
class AnieEsApp : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        AppDb.init(this)
    }

    override fun newImageLoader(context: Context): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(okHttpClient()))
            }
            .build()

    private fun okHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .callTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", AnimeFlvScraper.USER_AGENT)
                    .build(),
            )
        }
        .build()
}