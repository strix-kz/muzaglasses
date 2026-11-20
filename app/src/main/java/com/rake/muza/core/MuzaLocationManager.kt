package com.rake.muza.core

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
)

data class MuzaLocationState(
    val isTracking: Boolean = false,
    val hasGpsFix: Boolean = false,
    val latitude: Double = 43.238949,
    val longitude: Double = 76.889709,
    val accuracyMeters: Float = 0f,
    val speedKmh: Float = 0f,
    val bearingDegrees: Float = 0f,
    val altitudeMeters: Double = 0.0,
    val city: String = "Алматы",
    val streetAddress: String = "Алматы, Абай көшесі",
    val providerName: String = "GPS",
    val trail: List<GeoPoint> = emptyList(),
    val statusMessage: String = "",
)

/**
 * Real Android GPS & Network Location tracker with reverse Geocoding (Kazakh/Russian/English).
 * Powers both the interactive Map screen, live Home camera location badge, photo geotagging,
 * and voice queries like "Муза, қайдамын?" / "Муза, GPS қос".
 */
class MuzaLocationManager(context: Context) {
    companion object {
        private const val TAG = "MuzaLocationManager"
        private const val MIN_TIME_MS = 2_500L
        private const val MIN_DISTANCE_M = 3f
        private const val MAX_TRAIL_POINTS = 120
    }

    private val appContext = context.applicationContext
    private val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(MuzaLocationState())
    val state: StateFlow<MuzaLocationState> = _state.asStateFlow()

    private var lastGeocodeLat = 0.0
    private var lastGeocodeLon = 0.0

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            handleNewLocation(location)
        }

        @Deprecated("Deprecated in Android API 29")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (!hasLocationPermission()) {
            _state.update {
                it.copy(
                    isTracking = false,
                    statusMessage = "GPS рұқсаты қажет",
                )
            }
            return
        }
        val lm = locationManager ?: return

        runCatching {
            // 1. Seed immediately from best last known location
            val candidates = listOfNotNull(
                runCatching { lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) }.getOrNull(),
                runCatching { lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) }.getOrNull(),
                runCatching { lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER) }.getOrNull(),
            )
            candidates.maxByOrNull { it.time }?.let { best ->
                handleNewLocation(best)
            }

            // 2. Register live updates on both GPS and Network providers
            var anyProvider = false
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_MS,
                    MIN_DISTANCE_M,
                    locationListener,
                    Looper.getMainLooper(),
                )
                anyProvider = true
            }
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    MIN_TIME_MS * 2,
                    MIN_DISTANCE_M,
                    locationListener,
                    Looper.getMainLooper(),
                )
                anyProvider = true
            }

            _state.update {
                it.copy(
                    isTracking = true,
                    statusMessage = if (anyProvider) "GPS белсенді" else "GPS сигналы күтілуде...",
                )
            }
        }.onFailure { e ->
            Log.w(TAG, "Failed to start GPS tracking: ${e.message}")
            _state.update { it.copy(isTracking = false, statusMessage = e.localizedMessage ?: "GPS error") }
        }
    }

    fun stopTracking() {
        runCatching {
            locationManager?.removeUpdates(locationListener)
        }
        _state.update { it.copy(isTracking = false) }
    }

    fun refreshNow() {
        stopTracking()
        startTracking()
    }

    private fun handleNewLocation(location: Location) {
        val point = GeoPoint(location.latitude, location.longitude)
        val speedKmh = if (location.hasSpeed()) (location.speed * 3.6f).coerceAtLeast(0f) else 0f

        _state.update { current ->
            val updatedTrail = (current.trail + point).takeLast(MAX_TRAIL_POINTS)
            current.copy(
                isTracking = true,
                hasGpsFix = true,
                latitude = location.latitude,
                longitude = location.longitude,
                accuracyMeters = location.accuracy,
                speedKmh = speedKmh,
                bearingDegrees = if (location.hasBearing()) location.bearing else current.bearingDegrees,
                altitudeMeters = if (location.hasAltitude()) location.altitude else current.altitudeMeters,
                providerName = location.provider?.uppercase(Locale.US) ?: "GPS",
                trail = updatedTrail,
            )
        }

        val deltaLat = kotlin.math.abs(location.latitude - lastGeocodeLat)
        val deltaLon = kotlin.math.abs(location.longitude - lastGeocodeLon)
        if (deltaLat > 0.0004 || deltaLon > 0.0004) {
            lastGeocodeLat = location.latitude
            lastGeocodeLon = location.longitude
            reverseGeocode(location.latitude, location.longitude)
        }
    }

    private fun reverseGeocode(lat: Double, lon: Double) {
        if (!Geocoder.isPresent()) return
        scope.launch {
            runCatching {
                val locale = LanguageManager.currentLanguage.ttsLocale
                val geocoder = Geocoder(appContext, locale)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(lat, lon, 1) { addresses ->
                        applyGeocodedAddress(addresses.firstOrNull())
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val list = geocoder.getFromLocation(lat, lon, 1)
                    applyGeocodedAddress(list?.firstOrNull())
                }
            }.onFailure { e ->
                Log.w(TAG, "Reverse geocode skipped: ${e.message}")
            }
        }
    }

    private fun applyGeocodedAddress(address: android.location.Address?) {
        if (address == null) return
        val city = address.locality
            ?: address.subAdminArea
            ?: address.adminArea
            ?: _state.value.city
        val street = listOfNotNull(
            address.thoroughfare,
            address.subThoroughfare,
        ).joinToString(" ").trim()
        val combined = if (street.isNotBlank()) "$city, $street" else address.getAddressLine(0) ?: city
        _state.update {
            it.copy(
                city = city.take(60),
                streetAddress = combined.take(90),
            )
        }
    }

    fun spokenLocationSummary(lang: AppLanguage = LanguageManager.currentLanguage): String {
        val st = _state.value
        val coords = String.format(Locale.US, "%.4f, %.4f", st.latitude, st.longitude)
        return when (lang) {
            AppLanguage.KK -> "Сіз қазір ${st.streetAddress} маңындасыз. GPS координаттары: $coords."
            AppLanguage.RU -> "Вы сейчас находитесь рядом с: ${st.streetAddress}. Координаты GPS: $coords."
            AppLanguage.EN -> "You are currently near ${st.streetAddress}. GPS coordinates: $coords."
        }
    }
}
