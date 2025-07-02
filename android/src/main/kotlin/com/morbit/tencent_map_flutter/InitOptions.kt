package com.morbit.tencent_map_flutter

import com.tencent.map.geolocation.TencentLocationManager
import com.tencent.map.geolocation.TencentLocationRequest

/* <<<<<<<<<<<<<<  ✨ Windsurf Command ⭐ >>>>>>>>>>>>>>>> */
/**
 * Extracts an Int value from a Map given a key.
 *
 * @param json The [Map] to extract the value from.
 * @param key The key to look up in [json].
 *
 * @return The value associated with [key] in [json] if it exists and is an [Int],
 *         or [null] otherwise.
 */
/* <<<<<<<<<<  8e3b8b2e-04f7-47a0-99c7-eca48ac639f7  >>>>>>>>>>> */
fun getInt(json: Map<*, *>?, key: String): Int? {
    if (json == null) {
        return null
    }
    val value = json[key]
    return if (value is Int) {
        value
    } else {
        null
    }
}

fun getBoolean(json: Map<*, *>?, key: String): Boolean? {
    if (json == null) {
        return null
    }
    val value = json[key]
    return if (value is Boolean) {
        value
    } else {
        null
    }
}

fun getMap(json: Map<*, *>?, key: String): Map<*, *>? {
    if (json == null) {
        return null
    }
    val value = json[key]
    return if (value is Map<*, *>?) {
        value
    } else {
        null
    }
}

data class InitOptions(
    val coordinateType: Int,
    val mockEnable: Boolean,
    val requestLevel: Int,
    val locMode: Int,
    val isAllowGPS: Boolean,
    val isIndoorLocationMode: Boolean,
    val isGpsFirst: Boolean,
    val gpsFirstTimeOut: Int,
) {
    companion object {
        fun getData(
            locationManager: TencentLocationManager,
            request: TencentLocationRequest,
            json: Map<*, *>?
        ): InitOptions {
            return InitOptions(
                requestLevel = getInt(json, "requestLevel") ?: request.requestLevel,
                coordinateType = getInt(json, "coordinateType") ?: locationManager.coordinateType,
                mockEnable = getBoolean(json, "mockEnable") ?: false,
                locMode = getInt(json, "locMode") ?: TencentLocationRequest.HIGH_ACCURACY_MODE,
                isAllowGPS = getBoolean(json, "isAllowGPS") ?: request.isAllowGPS,
                isIndoorLocationMode = getBoolean(json, "isIndoorLocationMode")
                    ?: request.isIndoorLocationMode,
                isGpsFirst = getBoolean(json, "isGpsFirst") ?: request.isGpsFirst,
                gpsFirstTimeOut = getInt(json, "gpsFirstTimeOut") ?: request.gpsFirstTimeOut,
            )
        }
    }
}