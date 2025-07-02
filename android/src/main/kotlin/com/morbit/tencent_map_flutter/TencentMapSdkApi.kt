package com.morbit.tencent_map_flutter

import android.content.Context
import android.os.Looper
import com.tencent.map.geolocation.TencentLocation
import com.tencent.map.geolocation.TencentLocationListener
import com.tencent.map.geolocation.TencentLocationManager
import com.tencent.map.geolocation.TencentLocationRequest
import com.tencent.tencentmap.mapsdk.maps.TencentMapInitializer
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import com.morbit.tencent_map_flutter.InitOptions

class TencentMapSdkApi : FlutterPlugin {
  companion object {
    private var locationManager: TencentLocationManager? = null
    private var tencentLocationRequest: TencentLocationRequest? = null
    private val resultList = arrayListOf<MethodChannel.Result>()

    fun setup(binding: FlutterPlugin.FlutterPluginBinding) {
      val initializerChannel = MethodChannel(
        binding.binaryMessenger,
        "plugins.flutter.dev/tencent_map_flutter_initializer"
      )
      locationManager = TencentLocationManager.getInstance(binding.applicationContext)
      tencentLocationRequest = TencentLocationRequest.create()

      initializerChannel.setMethodCallHandler { call: MethodCall, result: MethodChannel.Result ->
        when (call.method) {
          "agreePrivacy" -> {
            val agree = call.argument<Boolean>("agree") ?: false
            agreePrivacy(binding.applicationContext, agree)

            val args = call.arguments as? Map<*, *>
            if (args != null) {
              val options = InitOptions.getData(locationManager!!, tencentLocationRequest!!, args)
              locationManager?.coordinateType = options.coordinateType
              locationManager?.setMockEnable(options.mockEnable)
            }
            result.success(null)
          }
          "getLocationOnce" -> {
            var type = call.argument<String>("type")
//            if(type != null) {
//              if(type == 'WGS84'){
//                locationManager.coordinateType = TencentLocationManager.COORDINATE_TYPE_WGS84
//              }else{
//                locationManager.coordinateType = TencentLocationManager.COORDINATE_TYPE_GCJ02
//              }
//            }
            if (type != null) {
              when (type) {
                "WGS84" -> locationManager?.coordinateType = TencentLocationManager.COORDINATE_TYPE_WGS84
                "GCJ02" -> locationManager?.coordinateType = TencentLocationManager.COORDINATE_TYPE_GCJ02
                else -> locationManager?.coordinateType = TencentLocationManager.COORDINATE_TYPE_GCJ02 // Default
              }
            }
            getLocationOnce(result)
          }
          else -> {
            result.notImplemented()
          }
        }
      }
    }

    private fun agreePrivacy(context:Context, agreePrivacy: Boolean) {
      TencentMapInitializer.setAgreePrivacy(context,agreePrivacy)
      TencentLocationManager.setUserAgreePrivacy(agreePrivacy)
      TencentMapInitializer.start(context)
    }

    private fun getLocationOnce(result: MethodChannel.Result) {
      resultList.add(result)
      val error = locationManager?.requestSingleFreshLocation(
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

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {

  }

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
