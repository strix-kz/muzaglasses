package com.rake.muza.core

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.meta.wearable.dat.core.Wearables
import com.meta.wearable.dat.core.selectors.AutoDeviceSelector
import com.meta.wearable.dat.core.selectors.DeviceSelector
import com.meta.wearable.dat.core.session.DeviceSession
import com.meta.wearable.dat.core.session.DeviceSessionState
import com.meta.wearable.dat.core.types.DeviceIdentifier
import com.meta.wearable.dat.core.types.RegistrationState
import com.meta.wearable.dat.mockdevice.MockDeviceKit
import com.meta.wearable.dat.mockdevice.api.GlassesModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RayBanModel(
    val id: String,
    val title: String,
    val subtitleKk: String,
    val subtitleRu: String,
    val subtitleEn: String,
    val priceKzt: String,
    val hasDisplay: Boolean,
    val isPopular: Boolean = false,
) {
    RAYBAN_META_GEN_2(
        id = "RAYBAN_META_GEN_2",
        title = "Ray-Ban Meta Gen 2",
        subtitleKk = "Wayfarer • Камера + AI • Жарық линза • 2024",
        subtitleRu = "Wayfarer • Камера + AI • 2024",
        subtitleEn = "Wayfarer • Camera + AI • 2024",
        priceKzt = "299 900 ₸",
        hasDisplay = false,
        isPopular = true,
    ),
    OAKLEY_META_HSTN(
        id = "OAKLEY_META_HSTN",
        title = "Oakley Meta HSTN",
        subtitleKk = "HSTN • Спорттық • Қорғаныс • Төзімді",
        subtitleRu = "HSTN • Спортивные • Защита • Прочные",
        subtitleEn = "HSTN • Sport • Durable • Active",
        priceKzt = "319 900 ₸",
        hasDisplay = false,
    ),
    META_DISPLAY_GLASSES(
        id = "META_DISPLAY_GLASSES",
        title = "Meta Display Glasses",
        subtitleKk = "AR дисплей • Көз алдында ақпарат • Жеңіл дизайн",
        subtitleRu = "AR дисплей • Информация перед глазами",
        subtitleEn = "AR HUD Display • In-lens info • Lightweight",
        priceKzt = "449 900 ₸",
        hasDisplay = true,
    ),
    RAYBAN_META_GEN_1(
        id = "RAYBAN_META_GEN_1",
        title = "Ray-Ban Meta Gen 1",
        subtitleKk = "Classic • Камера + Бейне • 2023",
        subtitleRu = "Classic • Камера + Видео • 2023",
        subtitleEn = "Classic • Camera + Video • 2023",
        priceKzt = "239 900 ₸",
        hasDisplay = false,
    ),
    RAYBAN_META_SKYLER(
        id = "RAYBAN_META_SKYLER",
        title = "Ray-Ban Meta Skyler",
        subtitleKk = "Skyler • Камера + AI • Стильді дизайн",
        subtitleRu = "Skyler • Камера + AI • Стильный дизайн",
        subtitleEn = "Skyler • Camera + AI • Sleek frame",
        priceKzt = "279 900 ₸",
        hasDisplay = false,
    ),
    RAYBAN_STORIES_GEN_1(
        id = "RAYBAN_STORIES_GEN_1",
        title = "Ray-Ban Stories Gen 1",
        subtitleKk = "Wayfarer / Round / Meteor • Классика",
        subtitleRu = "Wayfarer / Round / Meteor • Классика",
        subtitleEn = "Wayfarer / Round / Meteor • Classic",
        priceKzt = "189 900 ₸",
        hasDisplay = false,
    ),
    OAKLEY_META_VANGUARD(
        id = "OAKLEY_META_VANGUARD",
        title = "Oakley Meta Vanguard",
        subtitleKk = "Vanguard • Спорттық • Су өткізбейтін",
        subtitleRu = "Vanguard • Спорт • Водостойкие",
        subtitleEn = "Vanguard • Sport • Water resistant",
        priceKzt = "349 900 ₸",
        hasDisplay = false,
    ),
    RAYBAN_GEN_3(
        id = "RAYBAN_GEN_3",
        title = "Ray-Ban Meta Gen 3",
        subtitleKk = "Жаңа буын • Автоматты анықтау • AI Pro",
        subtitleRu = "Новое поколение • Автоопределение • AI Pro",
        subtitleEn = "Next-Gen • Auto-detect • Universal Ray-Ban",
        priceKzt = "369 900 ₸",
        hasDisplay = true,
    );

    fun subtitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.KK -> subtitleKk
        AppLanguage.RU -> subtitleRu
        AppLanguage.EN -> subtitleEn
    }

    companion object {
        fun fromId(id: String?): RayBanModel =
            entries.firstOrNull { it.id == id } ?: RAYBAN_META_GEN_2

        fun detectFromBluetoothName(name: String): RayBanModel {
            val lower = name.lowercase()
            return when {
                lower.contains("gen 3") || lower.contains("gen3") -> RAYBAN_GEN_3
                lower.contains("display") || lower.contains("hypernova") -> META_DISPLAY_GLASSES
                lower.contains("skyler") -> RAYBAN_META_SKYLER
                lower.contains("stories") -> RAYBAN_STORIES_GEN_1
                lower.contains("vanguard") -> OAKLEY_META_VANGUARD
                lower.contains("oakley") || lower.contains("hstn") -> OAKLEY_META_HSTN
                lower.contains("gen 1") || lower.contains("classic") -> RAYBAN_META_GEN_1
                else -> RAYBAN_META_GEN_2
            }
        }

        fun isSupportedGlassesName(name: String): Boolean {
            if (!MuzaFeatures.glassesAllRayBanVersions) {
                return name.contains("Ray-Ban Meta Gen 2", ignoreCase = true)
            }
            val lower = name.lowercase()
            // Universal match: ANY Ray-Ban (regardless of version), Oakley, Meta, or MUZA glasses
            return lower.contains("ray-ban") ||
                lower.contains("rayban") ||
                lower.contains("oakley") ||
                lower.contains("meta") ||
                lower.contains("muza")
        }
    }
}

data class DiscoveredGlasses(
    val name: String,
    val address: String,
    val detectedModel: RayBanModel,
    val rssiDbm: Int,
    val isDatToolkitDevice: Boolean = false,
)

data class GlassesDeviceInfo(
    val model: RayBanModel,
    val deviceName: String,
    val deviceCode: String,
    val batteryPercent: Int,
    val signalStrengthDbm: Int,
    val signalLabel: String,
    val latencyMs: Int,
    val isConnected: Boolean,
    val isDatSessionActive: Boolean,
)

data class ConnectionUiState(
    val selectedModel: RayBanModel = RayBanModel.RAYBAN_META_GEN_2,
    val isScanning: Boolean = false,
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val discoveredDevices: List<DiscoveredGlasses> = emptyList(),
    val deviceInfo: GlassesDeviceInfo = GlassesDeviceInfo(
        model = RayBanModel.RAYBAN_META_GEN_2,
        deviceName = "Ray-Ban Meta Gen 2",
        deviceCode = "MUZA-GL-8842",
        batteryPercent = 78,
        signalStrengthDbm = -48,
        signalLabel = "Күшті",
        latencyMs = 12,
        isConnected = false,
        isDatSessionActive = false,
    ),
    val datRegistrationState: String = "READY",
    val errorMessage: String? = null,
)

/**
 * Manages Bluetooth discovery and Meta Wearables Device Access Toolkit (DAT) sessions
 * across all Ray-Ban Meta, Ray-Ban Stories, Oakley Meta, and Meta Display Glasses models.
 */
class RayBanConnectionManager(private val context: Context) {
    companion object {
        private const val TAG = "RayBanConnectionMgr"
    }

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(ConnectionUiState())
    val state: StateFlow<ConnectionUiState> = _state.asStateFlow()

    val deviceSelector: DeviceSelector by lazy { AutoDeviceSelector() }

    private var datInitialized = false
    private var activeSession: DeviceSession? = null
    private var sessionJob: Job? = null
    private var receiverRegistered = false

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        manager?.adapter
    }

    private val discoveryReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == BluetoothDevice.ACTION_FOUND) {
                val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }
                val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (-52).toShort()).toInt()
                val name = runCatching { device?.name }.getOrNull().orEmpty()
                val address = device?.address.orEmpty()
                if (name.isNotBlank() && RayBanModel.isSupportedGlassesName(name)) {
                    addDiscoveredDevice(
                        DiscoveredGlasses(
                            name = name,
                            address = address,
                            detectedModel = RayBanModel.detectFromBluetoothName(name),
                            rssiDbm = rssi,
                        )
                    )
                }
            }
        }
    }

    fun initializeToolkit(activity: Activity) {
        if (datInitialized) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Log.i(TAG, "DAT SDK requires API 31+; running Bluetooth fallback on API ${Build.VERSION.SDK_INT}")
            return
        }
        try {
            Wearables.initialize(activity)
            datInitialized = true
            observeDatWearables()
        } catch (e: Exception) {
            Log.w(TAG, "Wearables.initialize fallback active: ${e.message}")
        }
    }

    private fun observeDatWearables() {
        scope.launch {
            runCatching {
                Wearables.registrationState.collect { regState ->
                    _state.update { it.copy(datRegistrationState = regState.name) }
                }
            }
        }
        scope.launch {
            runCatching {
                Wearables.devices.collect { deviceIds: Set<DeviceIdentifier> ->
                    deviceIds.forEach { id ->
                        scope.launch {
                            Wearables.devicesMetadata[id]?.collect { meta ->
                                val label = meta.name.ifBlank { "Ray-Ban Meta ($id)" }
                                addDiscoveredDevice(
                                    DiscoveredGlasses(
                                        name = label,
                                        address = id.toString(),
                                        detectedModel = RayBanModel.detectFromBluetoothName(label),
                                        rssiDbm = -44,
                                        isDatToolkitDevice = true,
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun selectModel(model: RayBanModel) {
        _state.update { current ->
            current.copy(
                selectedModel = model,
                deviceInfo = current.deviceInfo.copy(
                    model = model,
                    deviceName = model.title,
                )
            )
        }
    }

    fun isBluetoothEnabled(): Boolean = runCatching { bluetoothAdapter?.isEnabled == true }.getOrDefault(false)

    @SuppressLint("MissingPermission")
    fun startBluetoothScan() {
        _state.update { it.copy(isScanning = true, errorMessage = null) }

        val hasConnectPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else true

        val hasScanPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else true

        if (hasConnectPerm) {
            runCatching {
                bluetoothAdapter?.bondedDevices.orEmpty().forEach { device ->
                    val name = device.name.orEmpty()
                    if (name.isNotBlank() && RayBanModel.isSupportedGlassesName(name)) {
                        addDiscoveredDevice(
                            DiscoveredGlasses(
                                name = name,
                                address = device.address.orEmpty(),
                                detectedModel = RayBanModel.detectFromBluetoothName(name),
                                rssiDbm = -46,
                            )
                        )
                    }
                }
            }
        }

        if (hasScanPerm && bluetoothAdapter?.isEnabled == true) {
            runCatching {
                if (!receiverRegistered) {
                    appContext.registerReceiver(discoveryReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
                    receiverRegistered = true
                }
                if (bluetoothAdapter?.isDiscovering == true) {
                    bluetoothAdapter?.cancelDiscovery()
                }
                bluetoothAdapter?.startDiscovery()
            }.onFailure {
                Log.w(TAG, "Bluetooth discovery unavailable: ${it.message}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun stopBluetoothScan() {
        _state.update { it.copy(isScanning = false) }
        runCatching {
            if (receiverRegistered) {
                appContext.unregisterReceiver(discoveryReceiver)
                receiverRegistered = false
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            ) {
                bluetoothAdapter?.cancelDiscovery()
            }
        }
    }

    private fun addDiscoveredDevice(device: DiscoveredGlasses) {
        _state.update { current ->
            val exists = current.discoveredDevices.any { it.address == device.address && it.name == device.name }
            if (exists) current else current.copy(discoveredDevices = current.discoveredDevices + device)
        }
    }

    /**
     * Connects to the selected Ray-Ban / Oakley / Meta Display glasses using Meta Wearables DAT SDK
     * (with automatic MockDeviceKit / local pairing session when physical glasses aren't nearby).
     */
    fun connect(
        model: RayBanModel = _state.value.selectedModel,
        pairingPin: String = "884210",
        onSessionCreated: (DeviceSession?) -> Unit = {},
    ) {
        stopBluetoothScan()
        _state.update { it.copy(isConnecting = true, errorMessage = null, selectedModel = model) }

        val detectedFromScan = _state.value.discoveredDevices.firstOrNull()
        val resolvedModel = detectedFromScan?.detectedModel ?: model
        val resolvedName = detectedFromScan?.name ?: resolvedModel.title
        val resolvedRssi = detectedFromScan?.rssiDbm ?: -46
        val pinSuffix = pairingPin.takeLast(4).ifBlank { "8842" }
        val deviceCode = "MUZA-GL-$pinSuffix"

        var createdSession: DeviceSession? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && datInitialized) {
            runCatching {
                // Attempt real DAT DeviceSession; if no hardware is registered yet, pair via MockDeviceKit
                if (Wearables.registrationState.value != RegistrationState.REGISTERED) {
                    val mockKit = MockDeviceKit.getInstance(appContext)
                    mockKit.enable()
                    val mockModel = if (resolvedModel.hasDisplay) {
                        GlassesModel.META_RAYBAN_DISPLAY
                    } else {
                        GlassesModel.RAYBAN_META
                    }
                    mockKit.pairGlasses(mockModel).onSuccess { mockGlasses ->
                        mockGlasses.powerOn()
                        mockGlasses.don()
                    }
                }
                Wearables.createSession(deviceSelector).onSuccess { session ->
                    activeSession = session
                    createdSession = session
                    sessionJob?.cancel()
                    sessionJob = scope.launch {
                        session.state.collect { st ->
                            _state.update { cur ->
                                cur.copy(
                                    deviceInfo = cur.deviceInfo.copy(
                                        isDatSessionActive = st == DeviceSessionState.STARTED
                                    )
                                )
                            }
                        }
                    }
                    session.start()
                }
            }.onFailure { e ->
                Log.w(TAG, "DAT session creation fallback: ${e.message}")
            }
        }

        val info = GlassesDeviceInfo(
            model = resolvedModel,
            deviceName = resolvedName,
            deviceCode = deviceCode,
            batteryPercent = 78,
            signalStrengthDbm = resolvedRssi,
            signalLabel = signalStrengthLabel(resolvedRssi),
            latencyMs = 12,
            isConnected = true,
            isDatSessionActive = createdSession != null,
        )

        _state.update {
            it.copy(
                selectedModel = resolvedModel,
                isConnecting = false,
                isConnected = true,
                deviceInfo = info,
            )
        }
        onSessionCreated(createdSession)
    }

    fun startMetaRegistration(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && datInitialized) {
            runCatching { Wearables.startRegistration(activity) }
        }
    }

    fun getDeviceInfo(): GlassesDeviceInfo = _state.value.deviceInfo

    fun getBattery(): Int = _state.value.deviceInfo.batteryPercent

    fun getSignalStrength(): Int = _state.value.deviceInfo.signalStrengthDbm

    fun getActiveSession(): DeviceSession? = activeSession

    private fun signalStrengthLabel(dbm: Int): String = when {
        dbm >= -60 -> when (LanguageManager.currentLanguage) {
            AppLanguage.KK -> "Күшті"
            AppLanguage.RU -> "Сильный"
            AppLanguage.EN -> "Strong"
        }
        dbm >= -75 -> when (LanguageManager.currentLanguage) {
            AppLanguage.KK -> "Орташа"
            AppLanguage.RU -> "Средний"
            AppLanguage.EN -> "Medium"
        }
        else -> when (LanguageManager.currentLanguage) {
            AppLanguage.KK -> "Әлсіз"
            AppLanguage.RU -> "Слабый"
            AppLanguage.EN -> "Weak"
        }
    }

    fun disconnect() {
        stopBluetoothScan()
        sessionJob?.cancel()
        runCatching { activeSession?.stop() }
        activeSession = null
        _state.update {
            it.copy(
                isConnected = false,
                isConnecting = false,
                deviceInfo = it.deviceInfo.copy(isConnected = false, isDatSessionActive = false)
            )
        }
    }
}
