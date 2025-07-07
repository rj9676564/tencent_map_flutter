package com.morbit.tencent_map_flutter

import android.content.Context
import android.os.Looper
import android.util.Log
import com.tencent.lbssearch.HttpResponseListener
import com.tencent.lbssearch.TencentSearch
import com.tencent.lbssearch.httpresponse.BaseObject
import com.tencent.lbssearch.`object`.param.Geo2AddressParam
import com.tencent.lbssearch.`object`.param.SearchParam
import com.tencent.lbssearch.`object`.param.SuggestionParam
import com.tencent.lbssearch.`object`.result.Geo2AddressResultObject
import com.tencent.lbssearch.`object`.result.SearchResultObject
import com.tencent.lbssearch.`object`.result.SuggestionResultObject
import com.tencent.map.geolocation.TencentLocation
import com.tencent.map.geolocation.TencentLocationListener
import com.tencent.map.geolocation.TencentLocationManager
import com.tencent.map.geolocation.TencentLocationRequest
import com.tencent.tencentmap.mapsdk.maps.TencentMapInitializer
import com.tencent.tencentmap.mapsdk.maps.model.LatLng
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class TencentMapSdkApi : FlutterPlugin {
    companion object {
        private var locationManager: TencentLocationManager? = null
        private var tencentLocationRequest: TencentLocationRequest? = null
        private val resultList = arrayListOf<MethodChannel.Result>()

        fun setup(binding: FlutterPlugin.FlutterPluginBinding) {
            val initializerChannel =
                MethodChannel(
                    binding.binaryMessenger,
                    "plugins.flutter.dev/tencent_map_flutter_initializer"
                )
            locationManager = TencentLocationManager.getInstance(binding.applicationContext)
            tencentLocationRequest = TencentLocationRequest.create()

            initializerChannel.setMethodCallHandler { call: MethodCall, result: MethodChannel.Result
                ->
                when (call.method) {
                    "agreePrivacy" -> {
                        val agree = call.argument<Boolean>("agree") ?: false
                        agreePrivacy(binding.applicationContext, agree)

                        val args = call.arguments as? Map<*, *>
                        if (args != null) {
                            val options =
                                InitOptions.getData(
                                    locationManager!!,
                                    tencentLocationRequest!!,
                                    args
                                )
                            locationManager?.coordinateType = options.coordinateType
                            locationManager?.setMockEnable(options.mockEnable)
                        }
                        result.success(null)
                    }

                    "getLocationOnce" -> {
                        var type = call.argument<String>("type")
                        if (type != null) {
                            when (type) {
                                "WGS84" ->
                                    locationManager?.coordinateType =
                                        TencentLocationManager.COORDINATE_TYPE_WGS84

                                "GCJ02" ->
                                    locationManager?.coordinateType =
                                        TencentLocationManager.COORDINATE_TYPE_GCJ02

                                else ->
                                    locationManager?.coordinateType =
                                        TencentLocationManager
                                            .COORDINATE_TYPE_GCJ02 // Default
                            }
                        }
                        getLocationOnce(result)
                    }

                    "geo2address" -> {

                        val latitude: Double = call.argument<Double>("latitude") ?: 0.0
                        val longitude: Double = call.argument<Double>("longitude") ?: 0.0
                        val apiKey: String = call.argument<String>("apiKey") ?: ""
                        val secretKey: String = call.argument<String>("secretKey") ?: ""
                        Log.d(
                            "TencentMapFlutter",
                            "latitude: $latitude, longitude: $longitude, apiKey: $apiKey, secretKey: $secretKey"
                        )
                        val tencentSearch: TencentSearch =
                            TencentSearch(binding.applicationContext, apiKey, secretKey)
                        val latLng = LatLng(latitude, longitude)
                        val geo2AddressParam: Geo2AddressParam = Geo2AddressParam(latLng)
                        tencentSearch.geo2address(
                            geo2AddressParam,
                            object : HttpResponseListener<BaseObject?> {
                                override fun onSuccess(arg0: Int, arg1: BaseObject?) {
                                    if (arg1 == null || arg1 !is Geo2AddressResultObject) {
                                        val errorResult =
                                            HashMap<String, Any>().apply {
                                                put("code", -1)
                                                put("message", "Invalid search result")
                                            }
                                        result.error("-1", "Invalid search result", errorResult)
                                        return
                                    }
                                    val obj: Geo2AddressResultObject =
                                        arg1 as Geo2AddressResultObject
                                    val resultMap =
                                        HashMap<String, Any>().apply {
                                            put("province", obj.result.ad_info.province)
                                            put("city", obj.result.ad_info.city)
                                            put("district", obj.result.ad_info.district)
                                            put("address", obj.result.address)
                                            put("title", obj.result.address)
                                            put("latitude", latitude)
                                            put("longitude", longitude)
                                        }
                                    Log.d(
                                        "TencentMapFlutter",
                                        "geo2address onSuccess" + resultMap.toString()
                                    )
                                    result.success(resultMap)
                                }

                                override fun onFailure(
                                    arg0: Int,
                                    arg1: String,
                                    arg2: Throwable?
                                ) {
                                    Log.e("test", "error code:" + arg0 + ", msg:" + arg1)
                                    result.error(arg0.toString(), arg1, null)
                                }
                            }
                        )
                    }

                    "poiSearchMap" -> {
                        var city: String = call.argument<String>("city") ?: ""
                        val keyWord: String = call.argument<String>("keyWord") ?: ""
                        val apiKey: String = call.argument<String>("apiKey") ?: ""
                        val secretKey: String = call.argument<String>("secretKey") ?: ""
                        val pageSize: Int = call.argument<Int>("pageSize") ?: 10
                        val pageIndex: Int = call.argument<Int>("pageIndex") ?: 1
                        val tencentSearch: TencentSearch =
                            TencentSearch(binding.applicationContext, apiKey, secretKey)

                        // 构建地点检索
                        var searchParam: SuggestionParam = SuggestionParam(keyWord, city)
                        searchParam.pageSize(pageSize)
                        searchParam.pageIndex(pageIndex)

                        tencentSearch.suggestion(
                            searchParam,
                            object : HttpResponseListener<BaseObject?> {
                                override fun onFailure(
                                    arg0: Int,
                                    arg2: String?,
                                    arg3: Throwable?
                                ) {
                                    val errorDetails =
                                        HashMap<String, Any?>().apply {
                                            put("code", arg0)
                                            put("message", arg2 ?: "POI search failed")
                                            put("details", arg3?.message)
                                        }
                                    result.error(
                                        arg0.toString(),
                                        arg2 ?: "POI search failed",
                                        errorDetails
                                    )
                                }

                                override fun onSuccess(arg0: Int, arg1: BaseObject?) {
                                    if (arg1 == null || arg1 !is SuggestionResultObject) {
                                        val errorResult =
                                            HashMap<String, Any>().apply {
                                                put("code", -1)
                                                put("message", "Invalid search result")
                                            }
                                        result.error("-1", "Invalid search result", errorResult)
                                        return
                                    }

                                    val obj = arg1 as SuggestionResultObject
                                    if (obj.data == null || obj.data.isEmpty()) {
                                        val errorResult =
                                            HashMap<String, Any>().apply {
                                                put("code", -2)
                                                put("message", "No POI data found")
                                            }
                                        result.error("-2", "No POI data found", errorResult)
                                        return
                                    }

                                    // 格式化POI搜索结果
                                    val poiList = obj.data.map { data ->
                                            HashMap<String, Any?>().apply {
                                                put("title", data.title)
                                                put("address", data.address)
                                                put("latitude", data.latLng.latitude)
                                                put("longitude", data.latLng.longitude)
                                                put("city", data.city)
                                                put("province", data.province)
                                                put("district", data.district)
                                            }
                                        }

                                    // 返回POI列表给Flutter
                                    result.success(poiList)
                                }
                            }
                        )
                    }

                    else -> {
                        result.notImplemented()
                    }
                }
            }
        }

        private fun agreePrivacy(context: Context, agreePrivacy: Boolean) {
            TencentMapInitializer.setAgreePrivacy(context, agreePrivacy)
            TencentLocationManager.setUserAgreePrivacy(agreePrivacy)
            TencentMapInitializer.start(context)
        }

        private fun getLocationOnce(result: MethodChannel.Result) {
            resultList.add(result)
            val error =
                locationManager?.requestSingleFreshLocation(
                    tencentLocationRequest,
                    TencentLocationListenerImpl(),
                    Looper.getMainLooper()
                )
            if (error != TencentLocation.ERROR_OK) {
                val errResult = createErrorResult(error ?: -1)
                sendErrorLocationToFlutter(result, errResult)
                resultList.clear()
            }
        }

        private fun sendErrorLocationToFlutter(result: MethodChannel.Result?, value: Any) {
            result?.error((value as Map<*, *>)["code"].toString(), "Err", value)
        }

        private fun sendSuccessLocationToFlutter(result: MethodChannel.Result?, value: Any) {
            result?.success(value)
        }

        private fun createErrorResult(code: Int): HashMap<String, Any> {
            val result = HashMap<String, Any>()
            result["code"] = code
            result["message"] = "Location request failed"
            return result
        }
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        // No additional setup needed, handled in companion object
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {}

    // Inner class to handle location updates
    private class TencentLocationListenerImpl : TencentLocationListener {
        override fun onLocationChanged(location: TencentLocation?, error: Int, reason: String?) {
            if (error == TencentLocation.ERROR_OK && location != null) {
                val result = HashMap<String, Any?>()
                result["name"] = location.name
                result["latitude"] = location.latitude
                result["longitude"] = location.longitude
                result["address"] = location.address
                result["city"] = location.city
                result["province"] = location.province
                result["area"] = location.district
                result["cityCode"] = location.cityCode
                result["code"] = TencentLocation.ERROR_OK

                for (res in resultList) {
                    sendSuccessLocationToFlutter(res, result)
                }
            } else {
                val errResult = createErrorResult(error)
                errResult["message"] = reason ?: "Unknown error"
                for (res in resultList) {
                    sendErrorLocationToFlutter(res, errResult)
                }
            }
            resultList.clear()
            locationManager?.removeUpdates(this)
        }

        override fun onStatusUpdate(name: String?, status: Int, desc: String?) {
            // Optionally handle status updates
            val result = HashMap<String, Any?>()
            result["name"] = name
            result["status"] = status
            result["desc"] = desc
            // If you want to send status updates to Flutter, you need a MethodChannel instance
            // This requires additional setup, e.g., passing the channel to this class
        }
    }
}
