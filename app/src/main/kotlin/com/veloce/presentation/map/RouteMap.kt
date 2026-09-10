package com.veloce.presentation.map

import android.content.ComponentCallbacks2
import android.content.res.Configuration
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.veloce.domain.model.LocationPoint
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.*
import org.maplibre.android.style.layers.*
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.*

@Composable
fun RouteMap(points: List<LocationPoint>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val view = remember(context) { MapView(context).apply { onCreate(null) } }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var ready by remember { mutableStateOf(false) }
    var mapError by remember { mutableStateOf(false) }
    val currentPoints by rememberUpdatedState(points)

    DisposableEffect(view, lifecycle) {
        val bridge = MapLifecycle(view)
        lifecycle.addObserver(bridge)
        val memory = object : ComponentCallbacks2 {
            override fun onConfigurationChanged(configuration: Configuration) = Unit
            override fun onLowMemory() = view.onLowMemory()
            override fun onTrimMemory(level: Int) { if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) view.onLowMemory() }
        }
        context.registerComponentCallbacks(memory)
        val failureListener = MapView.OnDidFailLoadingMapListener { mapError = true }
        view.addOnDidFailLoadingMapListener(failureListener)
        view.getMapAsync { loaded ->
            if (!bridge.destroyed) {
                map = loaded
                loaded.setStyle(Style.Builder().fromUri("asset://offline-style.json")) { style ->
                    if (!bridge.destroyed) {
                        style.addSource(GeoJsonSource("route", FeatureCollection.fromFeatures(emptyArray<Feature>())))
                        style.addLayer(LineLayer("route-line", "route").withProperties(
                            lineColor("#C5F86A"), lineWidth(5f), lineCap(Property.LINE_CAP_ROUND), lineJoin(Property.LINE_JOIN_ROUND)))
                        style.addSource(GeoJsonSource("position", FeatureCollection.fromFeatures(emptyArray<Feature>())))
                        style.addLayer(CircleLayer("current-position", "position").withProperties(
                            circleRadius(6f), circleColor("#FFFFFF"), circleStrokeWidth(3f), circleStrokeColor("#C5F86A")))
                        ready = true
                        loaded.render(currentPoints)
                    }
                }
            }
        }
        onDispose {
            lifecycle.removeObserver(bridge)
            context.unregisterComponentCallbacks(memory)
            view.removeOnDidFailLoadingMapListener(failureListener)
            bridge.destroy()
        }
    }
    LaunchedEffect(points, ready) { if (ready) map?.render(points) }
    androidx.compose.foundation.layout.Box(modifier) {
        AndroidView(factory = { view }, modifier = Modifier.matchParentSize())
        if (mapError) androidx.compose.material3.Text("Mapa indisponível. Seus pontos continuam salvos.")
    }
}

private fun MapLibreMap.render(points: List<LocationPoint>) {
    val coordinates = points.map { Point.fromLngLat(it.longitude, it.latitude) }
    val route = if (coordinates.size >= 2) arrayOf(Feature.fromGeometry(LineString.fromLngLats(coordinates))) else emptyArray()
    style?.getSourceAs<GeoJsonSource>("route")?.setGeoJson(FeatureCollection.fromFeatures(route))
    val last = points.lastOrNull()
    val marker = if (last == null) emptyArray() else arrayOf(Feature.fromGeometry(Point.fromLngLat(last.longitude, last.latitude)))
    style?.getSourceAs<GeoJsonSource>("position")?.setGeoJson(FeatureCollection.fromFeatures(marker))
    if (last != null) moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(last.latitude, last.longitude), 16.0))
}

private class MapLifecycle(private val view: MapView) : DefaultLifecycleObserver {
    private var started = false
    private var resumed = false
    var destroyed = false
        private set
    override fun onStart(owner: LifecycleOwner) { if (!started && !destroyed) { view.onStart(); started = true } }
    override fun onResume(owner: LifecycleOwner) { if (!resumed && !destroyed) { view.onResume(); resumed = true } }
    override fun onPause(owner: LifecycleOwner) = pause()
    override fun onStop(owner: LifecycleOwner) = stop()
    override fun onDestroy(owner: LifecycleOwner) = destroy()
    private fun pause() { if (resumed) { view.onPause(); resumed = false } }
    private fun stop() { pause(); if (started) { view.onStop(); started = false } }
    fun destroy() { if (!destroyed) { stop(); view.onDestroy(); destroyed = true } }
}
