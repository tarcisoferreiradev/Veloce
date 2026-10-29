package com.veloce.presentation.map

import android.annotation.SuppressLint
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.location.Location
import android.view.MotionEvent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.location.LocationServices
import com.veloce.domain.model.LocationPoint
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.*
import org.maplibre.android.style.expressions.Expression.get
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
    var isFollowingUser by remember { mutableStateOf(true) }
    val currentPoints by rememberUpdatedState(points)

    val hasLocationPermission = remember {
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    DisposableEffect(view, lifecycle, hasLocationPermission) {
        val bridge = MapLifecycle(view)
        lifecycle.addObserver(bridge)
        val memory = object : ComponentCallbacks2 {
            override fun onConfigurationChanged(configuration: Configuration) = Unit
            override fun onLowMemory() = view.onLowMemory()
            override fun onTrimMemory(level: Int) { if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) view.onLowMemory() }
        }
        context.registerComponentCallbacks(memory)

        // Robust offline fallback: if online style fails to load, fallback seamlessly to local offline style
        val failureListener = MapView.OnDidFailLoadingMapListener {
            mapError = false
            loadedStyle(view) { loaded, style ->
                map = loaded
                setupMapLayers(style, loaded, currentPoints, isFollowingUser)
                ready = true
            }
        }
        view.addOnDidFailLoadingMapListener(failureListener)

        view.getMapAsync { loaded ->
            if (!bridge.destroyed) {
                map = loaded
                loaded.uiSettings.isScrollGesturesEnabled = true
                loaded.uiSettings.isZoomGesturesEnabled = true
                loaded.uiSettings.isRotateGesturesEnabled = true
                loaded.uiSettings.isTiltGesturesEnabled = true
                loaded.uiSettings.isDoubleTapGesturesEnabled = true
                loaded.uiSettings.isQuickZoomGesturesEnabled = true
                loaded.uiSettings.isAttributionEnabled = false
                loaded.uiSettings.isLogoEnabled = false

                loaded.setStyle(Style.Builder().fromUri("https://tiles.openfreemap.org/styles/dark")) { style ->
                    if (!bridge.destroyed) {
                        setupMapLayers(style, loaded, currentPoints, isFollowingUser)
                        ready = true
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

    LaunchedEffect(points, ready, hasLocationPermission) {
        if (ready) {
            if (points.isEmpty()) {
                map?.centerOnUserLocation(context)
            } else {
                map?.render(points, isFollowingUser)
            }
        }
    }

    Box(modifier) {
        AndroidView(factory = { view }, modifier = Modifier.matchParentSize())

        // Map Controls (Strava style right-side vertical stack)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.End
        ) {
            if (!isFollowingUser || points.isEmpty()) {
                FloatingActionButton(
                    onClick = {
                        isFollowingUser = true
                        val last = currentPoints.lastOrNull()
                        if (last != null) {
                            map?.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(LatLng(last.latitude, last.longitude), 16.0),
                                400
                            )
                        } else {
                            map?.centerOnUserLocation(context)
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Centralizar", modifier = Modifier.size(22.dp))
                }
            }

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 8.dp
            ) {
                Column {
                    IconButton(onClick = { map?.animateCamera(CameraUpdateFactory.zoomIn(), 250) }) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom In")
                    }
                    HorizontalDivider(modifier = Modifier.width(28.dp).align(Alignment.CenterHorizontally))
                    IconButton(onClick = { map?.animateCamera(CameraUpdateFactory.zoomOut(), 250) }) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
                    }
                }
            }
        }

        if (mapError) {
            Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Mapa indisponível. Seus pontos continuam salvos.",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

private fun loadedStyle(view: MapView, onReady: (MapLibreMap, Style) -> Unit) {
    view.getMapAsync { loaded ->
        loaded.setStyle(Style.Builder().fromUri("asset://offline-style.json")) { style ->
            onReady(loaded, style)
        }
    }
}

private fun setupMapLayers(style: Style, loaded: MapLibreMap, points: List<LocationPoint>, followUser: Boolean) {
    style.layers.forEach { layer ->
        if (layer is SymbolLayer && layer.id != "strava-lighthouse-layer") {
            try {
                layer.setProperties(textColor("#E2E8F0"))
            } catch (_: Exception) {}
        }
    }

    if (style.getSource("route") == null) {
        val beaconBitmap = createStravaBeaconBitmap()
        style.addImage("strava-beacon", beaconBitmap)

        style.addSource(GeoJsonSource("route", FeatureCollection.fromFeatures(emptyArray<Feature>())))
        style.addSource(GeoJsonSource("start-point", FeatureCollection.fromFeatures(emptyArray<Feature>())))
        style.addSource(GeoJsonSource("position", FeatureCollection.fromFeatures(emptyArray<Feature>())))

        // Neon glowing route trail (Strava dark mode style)
        style.addLayer(LineLayer("route-glow", "route").withProperties(
            lineColor("#38BDF8"),
            lineWidth(14f),
            lineOpacity(0.35f),
            lineCap(Property.LINE_CAP_ROUND),
            lineJoin(Property.LINE_JOIN_ROUND)
        ))
        style.addLayer(LineLayer("route-casing", "route").withProperties(
            lineColor("#05080E"),
            lineWidth(8f),
            lineCap(Property.LINE_CAP_ROUND),
            lineJoin(Property.LINE_JOIN_ROUND)
        ))
        style.addLayer(LineLayer("route-line", "route").withProperties(
            lineColor("#38BDF8"),
            lineWidth(4.5f),
            lineCap(Property.LINE_CAP_ROUND),
            lineJoin(Property.LINE_JOIN_ROUND)
        ))
        style.addLayer(CircleLayer("start-marker", "start-point").withProperties(
            circleRadius(6f),
            circleColor("#10B981"),
            circleStrokeWidth(2f),
            circleStrokeColor("#FFFFFF")
        ))
        // Neon green beacon puck with directional beam
        style.addLayer(SymbolLayer("strava-lighthouse-layer", "position").withProperties(
            iconImage("strava-beacon"),
            iconSize(1.6f),
            iconAllowOverlap(true),
            iconIgnorePlacement(true),
            iconRotate(get("bearing")),
            iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_VIEWPORT)
        ))
    }
    loaded.render(points, followUser)
}

@SuppressLint("MissingPermission")
private fun MapLibreMap.centerOnUserLocation(context: Context) {
    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        fusedClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 16.0))
                val bearing = if (location.hasBearing()) location.bearing.toDouble() else 0.0
                val point = Feature.fromGeometry(Point.fromLngLat(location.longitude, location.latitude)).apply {
                    addNumberProperty("bearing", bearing)
                }
                style?.getSourceAs<GeoJsonSource>("position")?.setGeoJson(FeatureCollection.fromFeatures(arrayOf(point)))
            }
        }
    }
}

private fun MapLibreMap.render(points: List<LocationPoint>, followUser: Boolean) {
    val coordinates = points.map { Point.fromLngLat(it.longitude, it.latitude) }
    val route = if (coordinates.size >= 2) arrayOf(Feature.fromGeometry(LineString.fromLngLats(coordinates))) else emptyArray()
    style?.getSourceAs<GeoJsonSource>("route")?.setGeoJson(FeatureCollection.fromFeatures(route))

    val first = points.firstOrNull()
    val startMarker = if (first == null) emptyArray() else arrayOf(Feature.fromGeometry(Point.fromLngLat(first.latitude, first.longitude)))
    style?.getSourceAs<GeoJsonSource>("start-point")?.setGeoJson(FeatureCollection.fromFeatures(startMarker))

    val last = points.lastOrNull()
    if (last != null) {
        val prev = if (points.size >= 2) points[points.size - 2] else last
        val bearing = if (prev.latitude == last.latitude && prev.longitude == last.longitude) {
            0.0
        } else {
            calculateBearing(prev.latitude, prev.longitude, last.latitude, last.longitude)
        }
        val point = Feature.fromGeometry(Point.fromLngLat(last.longitude, last.latitude)).apply {
            addNumberProperty("bearing", bearing)
        }
        style?.getSourceAs<GeoJsonSource>("position")?.setGeoJson(FeatureCollection.fromFeatures(arrayOf(point)))
    }

    if (last != null && followUser) {
        moveCamera(CameraUpdateFactory.newLatLng(LatLng(last.latitude, last.longitude)))
    }
}

private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val phi1 = Math.toRadians(lat1)
    val phi2 = Math.toRadians(lat2)
    val dLambda = Math.toRadians(lon2 - lon1)
    val y = kotlin.math.sin(dLambda) * kotlin.math.cos(phi2)
    val x = kotlin.math.cos(phi1) * kotlin.math.sin(phi2) - kotlin.math.sin(phi1) * kotlin.math.cos(phi2) * kotlin.math.cos(dLambda)
    var theta = kotlin.math.atan2(y, x)
    theta = Math.toDegrees(theta)
    return ((theta + 360) % 360)
}

private fun createStravaBeaconBitmap(): Bitmap {
    val size = 128
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply {
        isAntiAlias = true
    }

    // Soft translucent neon green cone / beam of light extending upwards from center
    paint.shader = android.graphics.LinearGradient(
        (size / 2).toFloat(), (size / 2).toFloat(),
        (size / 2).toFloat(), 0f,
        intArrayOf(Color.argb(150, 197, 248, 106), Color.argb(0, 197, 248, 106)),
        null,
        Shader.TileMode.CLAMP
    )
    val path = Path().apply {
        moveTo((size / 2).toFloat(), (size / 2).toFloat())
        lineTo((size / 2 - 32).toFloat(), 0f)
        lineTo((size / 2 + 32).toFloat(), 0f)
        close()
    }
    canvas.drawPath(path, paint)
    paint.shader = null

    // Outer white ring
    paint.color = Color.WHITE
    canvas.drawCircle((size / 2).toFloat(), (size / 2).toFloat(), 18f, paint)

    // Inner neon green dot
    paint.color = Color.parseColor("#C5F86A")
    canvas.drawCircle((size / 2).toFloat(), (size / 2).toFloat(), 13f, paint)

    return bitmap
}

class MapLifecycle(private val view: MapView) : DefaultLifecycleObserver {
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
