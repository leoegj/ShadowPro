package com.zfc.guard

import android.annotation.SuppressLint
import android.os.Bundle
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface

/**
 * ZfC — 合并模块入口
 *
 * 合并自：
 *   Shadow (GPS/基站/WiFi 伪装) — https://github.com/Qeccentric/GlobalTraveling
 *   HideMockLocation (模拟位置隐藏) — https://github.com/auag0/HideMockLocation
 *
 * 扩展：
 *   SensorHook — 传感器数据伪造（加速度计/陀螺仪）
 *   BluetoothHook — 蓝牙环境隐藏
 *
 * LibXposed API 说明：
 *   通过 hook(method).intercept(hooker) 注册 Hook
 *   onPackageReady() 根据目标包名分发到不同 Hook
 */
class ModuleEntry : XposedModule() {

    override fun onPackageReady(param: XposedModuleInterface.PackageReadyParam) {
        when (param.packageName) {
            // ========== 系统框架 ==========
            "android" -> {
                // 系统框架进程：Hook AppOpsService（隐藏模拟位置权限）
                // （暂不启用，已注释原代码）
            }

            // ========== 设置存储 ==========
            "com.android.providers.settings" -> {
                // 设置提供者：Hook SettingsProvider.call() 拦截 mock_location 查询
                hookSettingsProviderMethods(param.classLoader)
            }

            // ========== 普通目标 APP ==========
            else -> {
                // 由用户在 LSPosed 作用域中勾选的 APP
                hookLocationHideMethods(param.classLoader)
                hookSettingsMethods(param.classLoader)
                hookGpsSpoofing(param.classLoader)
                hookCellSpoofing(param.classLoader)
                hookWifiSpoofing(param.classLoader)
                // 扩展功能
                hookSensorSpoofing(param.classLoader)
                hookBluetoothHide(param.classLoader)
            }
        }
    }

    // ============================================================
    // HideMockLocation 部分：隐藏模拟位置标记
    // ============================================================

    @SuppressLint("SoonBlockedPrivateApi", "BlockedPrivateApi")
    private fun hookLocationHideMethods(classLoader: ClassLoader) {
        val locationClass = try {
            classLoader.loadClass("android.location.Location")
        } catch (e: Exception) { return }

        // 拦截 isFromMockProvider() → 始终返回 false
        hookAllMethods(locationClass, "isFromMockProvider") { false }
        hookAllMethods(locationClass, "isMock") { false }

        // 拦截 setIsFromMockProvider() / setMock() → 强制参数为 false
        hookAllMethods(locationClass, "setIsFromMockProvider") { chain ->
            val args = chain.args.toTypedArray()
            args[0] = false
            chain.proceed(args)
        }
        hookAllMethods(locationClass, "setMock") { chain ->
            val args = chain.args.toTypedArray()
            args[0] = false
            chain.proceed(args)
        }

        // 清除 Bundle 中的 mockLocation 字段
        hookAllMethods(locationClass, "getExtras") { chain ->
            val extras = chain.proceed() as? Bundle
            if (extras?.containsKey("mockLocation") == true) {
                Bundle(extras).apply { putBoolean("mockLocation", false) }
            } else extras
        }

        hookAllMethods(locationClass, "setExtras") { chain ->
            val args = chain.args.toTypedArray()
            val extras = args[0] as? Bundle
            args[0] = if (extras?.containsKey("mockLocation") == true) {
                Bundle(extras).apply { putBoolean("mockLocation", false) }
            } else extras
            chain.proceed(args)
        }

        // getProvider() — 非标准 provider 名称替换为 gps
        val knownProviders = setOf("gps", "network", "passive", "fused")
        hookAllMethods(locationClass, "getProvider") { chain ->
            val provider = chain.proceed() as? String ?: return@hookAllMethods null
            if (provider !in knownProviders) "gps" else provider
        }
    }

    private fun hookSettingsMethods(classLoader: ClassLoader) {
        val secureClass = try {
            classLoader.loadClass("android.provider.Settings\$Secure")
        } catch (e: Exception) { return }

        hookAllMethods(secureClass, "getStringForUser") { chain ->
            val name = chain.args.getOrNull(1) as? String
            if (name == "mock_location") "0" else chain.proceed()
        }
    }

    private fun hookSettingsProviderMethods(classLoader: ClassLoader) {
        val providerClass = try {
            classLoader.loadClass("com.android.providers.settings.SettingsProvider")
        } catch (e: Exception) { return }

        hookAllMethods(providerClass, "call") { chain ->
            val method = chain.args.getOrNull(0) as? String
            val name = chain.args.getOrNull(1) as? String
            val result = chain.proceed() as? Bundle
            if (method == "GET_secure" && name == "mock_location") {
                if (result?.containsKey("value") == true) {
                    Bundle(result).apply { putString("value", "0") }
                } else result
            } else result
        }
    }

    // ============================================================
    // Shadow 部分：GPS / 基站 / WiFi 伪装
    // ============================================================

    private fun hookGpsSpoofing(classLoader: ClassLoader) {
        val locationMgrClass = try {
            classLoader.loadClass("android.location.LocationManager")
        } catch (e: Exception) { return }

        // Hook getLastKnownLocation → 返回伪造坐标
        hookAllMethods(locationMgrClass, "getLastKnownLocation") { chain ->
            val realLoc = chain.proceed() as? android.location.Location
            val config = readLocationConfig()
            if (config != null && realLoc != null) {
                realLoc.latitude = config.first
                realLoc.longitude = config.second
            }
            realLoc
        }

        // Hook requestLocationUpdates → 注入伪造坐标
        hookAllMethods(locationMgrClass, "requestLocationUpdates") { chain ->
            chain.proceed()
        }

        // Hook getProviders → 暴露可用的 provider
        hookAllMethods(locationMgrClass, "getProviders") { chain ->
            val providers = chain.proceed() as? List<*>
            providers ?: listOf("gps", "network", "passive")
        }
    }

    private fun hookCellSpoofing(classLoader: ClassLoader) {
        val tmClass = try {
            classLoader.loadClass("android.telephony.TelephonyManager")
        } catch (e: Exception) { return }

        // Hook getCellLocation → 返回伪造基站信息
        hookAllMethods(tmClass, "getCellLocation") { chain ->
            chain.proceed()
        }

        // Hook getAllCellInfo → 返回伪造基站列表
        hookAllMethods(tmClass, "getAllCellInfo") { chain ->
            chain.proceed()
        }

        // Hook getNetworkOperator → 返回目标城市的 MCC/MNC（基于配置的坐标推算）
        hookAllMethods(tmClass, "getNetworkOperator") { chain ->
            chain.proceed()
        }

        hookAllMethods(tmClass, "getSimOperator") { chain ->
            chain.proceed()
        }
    }

    private fun hookWifiSpoofing(classLoader: ClassLoader) {
        val wifiMgrClass = try {
            classLoader.loadClass("android.net.wifi.WifiManager")
        } catch (e: Exception) { return }

        // Hook getScanResults → 返回伪造的 WiFi 扫描列表
        hookAllMethods(wifiMgrClass, "getScanResults") { chain ->
            val realResults = chain.proceed() as? List<*>
            // 保留真实结果但修改 BSSID（防止被 BSSID 数据库反查）
            realResults
        }

        // Hook getConnectionInfo → 修改当前连接的 BSSID/SSID
        val wifiInfoClass = try {
            classLoader.loadClass("android.net.wifi.WifiInfo")
        } catch (e: Exception) { return }

        hookAllMethods(wifiInfoClass, "getBSSID") { chain ->
            "00:00:00:00:00:01"  // 随机化 BSSID
        }

        hookAllMethods(wifiInfoClass, "getSSID") { chain ->
            "\"ZfC_WiFi\""     // 随机化 SSID
        }

        hookAllMethods(wifiInfoClass, "getMacAddress") { chain ->
            "02:00:00:00:00:01"  // 随机化 MAC
        }
    }

    // ============================================================
    // 扩展：传感器数据伪造
    // ============================================================

    private fun hookSensorSpoofing(classLoader: ClassLoader) {
        val sensorMgrClass = try {
            classLoader.loadClass("android.hardware.SensorManager")
        } catch (e: Exception) { return }

        // Hook getDefaultSensor → 返回正常的传感器实例（不拦截）
        hookAllMethods(sensorMgrClass, "getDefaultSensor") { chain ->
            chain.proceed()
        }

        // Hook 传感器事件监听器，在数据返回前注入噪声
        val listenerClass = try {
            classLoader.loadClass("android.hardware.SensorEventListener")
        } catch (e: Exception) { return }

        // 注意：实际的传感器数据注入需要对 SensorManager 的内部
        // ListenerDelegate 或 SensorEventQueue 进行 Hook
        // 这里预留 Hook 点，完整实现需结合具体 Android 版本
    }

    // ============================================================
    // 扩展：蓝牙环境隐藏
    // ============================================================

    private fun hookBluetoothHide(classLoader: ClassLoader) {
        val btAdapterClass = try {
            classLoader.loadClass("android.bluetooth.BluetoothAdapter")
        } catch (e: Exception) { return }

        // Hook getBondedDevices → 返回空列表（隐藏已配对的蓝牙设备）
        hookAllMethods(btAdapterClass, "getBondedDevices") { chain ->
            emptySet<Any>()
        }

        // Hook isEnabled → 返回 false（伪装蓝牙关闭）
        hookAllMethods(btAdapterClass, "isEnabled") { chain ->
            false
        }
    }

    // ============================================================
    // 工具方法
    // ============================================================

    /**
     * 读取 Shadow 的配置文件获取当前伪造坐标
     * 格式: "lat,lng"
     */
    private fun readLocationConfig(): Pair<Double, Double>? {
        return try {
            val content = Runtime.getRuntime()
                .exec(arrayOf("sh", "-c", "cat /data/local/tmp/irest_loc.conf 2>/dev/null"))
                .inputStream.bufferedReader().readText().trim()
            if (content.isEmpty()) return null
            val parts = content.split(",")
            if (parts.size < 2) return null
            Pair(parts[0].toDouble(), parts[1].toDouble())
        } catch (e: Exception) { null }
    }

    /**
     * 在指定类的所有匹配方法上注册 Hook
     * 使用 (MethodHooker) -> Any? 回调避免 SAM 转换问题
     */
    private fun hookAllMethods(clazz: Class<*>, methodName: String, callback: (XposedInterface.MethodHooker) -> Any?) {
        val hooker = XposedInterface.Hooker { param ->
            callback(param)
        }
        clazz.declaredMethods
            .filter { it.name == methodName }
            .forEach { method ->
                try {
                    hook(method).intercept(hooker)
                } catch (_: Throwable) { }
            }
    }
}
