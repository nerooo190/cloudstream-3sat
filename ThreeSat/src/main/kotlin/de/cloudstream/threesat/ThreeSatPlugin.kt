package de.cloudstream.threesat

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class ThreeSatPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(ThreeSatProvider())
    }
}
