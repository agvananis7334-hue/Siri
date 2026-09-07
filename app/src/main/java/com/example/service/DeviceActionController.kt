package com.example.service

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.AlarmClock
import android.provider.Settings
import android.util.Log

data class BatteryInfo(
    val level: Int,
    val isCharging: Boolean,
    val statusText: String
)

data class VivoPhoneInfo(
    val model: String,
    val brand: String,
    val processor: String,
    val batterySpec: String,
    val displaySpec: String,
    val cameraSpec: String,
    val currentBatteryPercent: Int,
    val availableRamMb: Long,
    val totalRamMb: Long,
    val freeStorageGb: Long,
    val totalStorageGb: Long,
    val androidVersion: String
)

class DeviceActionController(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var isTorchOn = false
    private var cameraIdWithFlash: String? = null

    init {
        findCameraWithFlash()
    }

    private fun findCameraWithFlash() {
        try {
            cameraManager?.cameraIdList?.forEach { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraIdWithFlash = id
                    return
                }
            }
            if (cameraIdWithFlash == null && cameraManager?.cameraIdList?.isNotEmpty() == true) {
                cameraIdWithFlash = cameraManager.cameraIdList[0]
            }
        } catch (e: Exception) {
            Log.e("DeviceActionController", "Error finding camera flash: ${e.message}")
        }
    }

    fun toggleTorch(enable: Boolean? = null): Boolean {
        vibrateFeedback()
        val targetState = enable ?: !isTorchOn
        val camId = cameraIdWithFlash ?: return false
        return try {
            cameraManager?.setTorchMode(camId, targetState)
            isTorchOn = targetState
            true
        } catch (e: CameraAccessException) {
            Log.e("DeviceActionController", "Torch error: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e("DeviceActionController", "Torch exception: ${e.message}")
            false
        }
    }

    fun isTorchActive(): Boolean = isTorchOn

    fun getBatteryInfo(): BatteryInfo {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val level = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 85
        val status = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) ?: BatteryManager.BATTERY_STATUS_UNKNOWN
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val statusText = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "ચાર્જ થઈ રહ્યું છે (Charging)"
            BatteryManager.BATTERY_STATUS_FULL -> "સંપૂર્ણ ચાર્જ (Full)"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "ડિસ્ચાર્જિંગ (Discharging)"
            else -> "સામાન્ય (Normal)"
        }
        return BatteryInfo(level = level, isCharging = isCharging, statusText = statusText)
    }

    fun setRingerMode(mode: String): String {
        vibrateFeedback()
        val am = audioManager ?: return "ઓડિયો મેનેજર ઉપલબ્ધ નથી"
        return try {
            when (mode.lowercase()) {
                "silent" -> {
                    am.ringerMode = AudioManager.RINGER_MODE_SILENT
                    "ફોન સાયલન્ટ મોડ પર મુકાયો છે (Phone set to Silent)"
                }
                "vibrate" -> {
                    am.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                    "વાઇબ્રેટ મોડ સેટ કર્યો છે (Vibrate Mode active)"
                }
                else -> {
                    am.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    "નોર્મલ રિંગર મોડ સેટ કર્યો છે (Normal Ringer active)"
                }
            }
        } catch (e: Exception) {
            "DND પરવાનગી જરૂરી છે: ${e.message}"
        }
    }

    fun openApp(packageName: String): Boolean {
        vibrateFeedback()
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun openDialer(phoneNumber: String = ""): Boolean {
        vibrateFeedback()
        return try {
            val uri = if (phoneNumber.isNotBlank()) Uri.parse("tel:$phoneNumber") else Uri.parse("tel:")
            val intent = Intent(Intent.ACTION_DIAL, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openSms(body: String = ""): Boolean {
        vibrateFeedback()
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:")
                if (body.isNotBlank()) putExtra("sms_body", body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun setAlarm(hour: Int, minute: Int, message: String = "Siri Alarm"): Boolean {
        vibrateFeedback()
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            showAlarms()
        }
    }

    fun setTimer(seconds: Int, message: String = "Siri Timer"): Boolean {
        vibrateFeedback()
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun showAlarms(): Boolean {
        return try {
            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openSettings(type: String = "general"): Boolean {
        vibrateFeedback()
        val action = when (type.lowercase()) {
            "wifi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "display" -> Settings.ACTION_DISPLAY_SETTINGS
            "sound" -> Settings.ACTION_SOUND_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        return try {
            val intent = Intent(action).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openCamera(): Boolean {
        vibrateFeedback()
        val launchSuccess = openApp("com.android.camera") || openApp("com.google.android.GoogleCamera")
        if (!launchSuccess) {
            return try {
                val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                false
            }
        }
        return true
    }

    fun getVivoPhoneSpecs(): VivoPhoneInfo {
        val battery = getBatteryInfo()
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)

        val totalRamMb = (memInfo.totalMem / (1024 * 1024))
        val availRamMb = (memInfo.availMem / (1024 * 1024))

        val stat = StatFs(Environment.getDataDirectory().path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availBlocks = stat.availableBlocksLong
        val totalStorageGb = (totalBlocks * blockSize) / (1024 * 1024 * 1024)
        val freeStorageGb = (availBlocks * blockSize) / (1024 * 1024 * 1024)

        return VivoPhoneInfo(
            model = "Vivo T3 5G",
            brand = "Vivo (Funtouch OS)",
            processor = "MediaTek Dimensity 7200 (4nm Octa-Core)",
            batterySpec = "5000 mAh Li-ion (44W FlashCharge)",
            displaySpec = "6.67\" 120Hz Ultra Vision AMOLED, 1800 nits",
            cameraSpec = "50 MP Sony IMX882 OIS + 2 MP Bokeh",
            currentBatteryPercent = battery.level,
            availableRamMb = availRamMb,
            totalRamMb = totalRamMb,
            freeStorageGb = freeStorageGb,
            totalStorageGb = totalStorageGb,
            androidVersion = "Android 14 (API ${Build.VERSION.SDK_INT})"
        )
    }

    fun vibrateFeedback(durationMs: Long = 45) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (e: Exception) {
            // Ignore if vibration disallowed
        }
    }
}
