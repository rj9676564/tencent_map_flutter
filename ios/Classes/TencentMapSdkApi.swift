import Flutter
import Foundation
import QMapKit
import TencentLBS



class TencentMapSdkApi: NSObject, QMSSearchDelegate {
    static var locationManager: TencentLBSLocationManager?
    static var search: QMSSearcher?
    static var pendingResult: FlutterResult?
    static var pendingLatitude: Double?
    static var pendingLongitude: Double?
    static var apiInstance: TencentMapSdkApi?
    
    static func agreePrivacy(agreePrivacy: Bool) {
        QMapServices.shared().setPrivacyAgreement(agreePrivacy)
        TencentLBSLocationManager.setUserAgreePrivacy(agreePrivacy)
    }

    static func setup( registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "plugins.flutter.dev/tencent_map_flutter_initializer", binaryMessenger: registrar.messenger())
        channel.setMethodCallHandler { call, result in
            switch call.method {
            case "agreePrivacy":
                if let args = call.arguments as? [String: Any] {
                    let agree = args["agree"] as? Bool ?? false
                    let apiKey = args["apiKey"] as? String
                    
                    
                    QMapServices.shared().setPrivacyAgreement(agree)
                    TencentLBSLocationManager.setUserAgreePrivacy(agree)
                
                    if let apiKey = apiKey {
                        QMapServices.shared().apiKey = apiKey
                        locationManager = TencentLBSLocationManager()
                        locationManager?.apiKey = apiKey
                    
                        TencentMapSdkApi.apiInstance = TencentMapSdkApi()
                        search = QMSSearcher(delegate: TencentMapSdkApi.apiInstance!)
                    }
                    
                    result(nil)
                } else {
                    result(FlutterError(code: "INVALID_ARGUMENTS", message: "Invalid arguments", details: nil))
                }
                
            case "getLocationOnce":
                if locationManager == nil {
                    result(FlutterError(code: "NOT_INITIALIZED", message: "Location manager not initialized", details: nil))
                    return
                }
                
                if let args = call.arguments as? [String: Any], let type = args["type"] as? String {
                    locationManager?.coordinateType = type == "WGS84" ? .WGS84 : .GCJ02
                    locationManager?.requestLocation { location, error in
                        if let error = error {
                            result(FlutterError(code: "LOCATION_ERROR", message: error.localizedDescription, details: nil))
                            return
                        }
                        
                        guard let location = location else {
                            result(FlutterError(code: "NO_LOCATION", message: "Location not available", details: nil))
                            return
                        }
                        
                        let map = [
                            "latitude": location.location.coordinate.latitude,
                            "longitude": location.location.coordinate.longitude,
                            "code": 0,
                            "altitude": location.location.altitude,
                            "name": location.name ?? "",
                            "address": location.address ?? "",
                            "city": location.city ?? "",
                            "province": location.province ?? "",
                            "district": location.district ?? "",
                            "street": location.street ?? "",
                            "area": location.district ?? ""
                        ] as [String : Any]
                        
                        result(map)
                    }
                } else {
                    result(FlutterError(code: "INVALID_ARGUMENTS", message: "Type parameter is required", details: nil))
                }
                
            case "geo2address":
                guard let args = call.arguments as? [String: Any],
                      let secretKey = args["secretKey"] as? String,
                      let apiKey = args["apiKey"] as? String,
                      let latitude = args["latitude"] as? Double,
                      let longitude = args["longitude"] as? Double else {
                    result(FlutterError(code: "INVALID_ARGUMENTS", message: "Invalid latitude or longitude", details: nil))
                    return
                }
                
                if search == nil {
                    result(FlutterError(code: "NOT_INITIALIZED", message: "Search manager not initialized", details: nil))
                    return
                }
                
                QMSSearchServices.shared().apiKey = apiKey
                QMSSearchServices.shared().secretKey = secretKey
                // 保存当前的 result 和坐标信息，以便在代理回调中使用
                TencentMapSdkApi.pendingResult = result
                TencentMapSdkApi.pendingLatitude = latitude
                TencentMapSdkApi.pendingLongitude = longitude
                
                let option = QMSReverseGeoCodeSearchOption()
                option.setLocationWithCenter( CLLocationCoordinate2D(latitude: latitude, longitude: longitude))
                option.get_poi = true
                
                TencentMapSdkApi.search?.searchWithReverseGeoCodeSearchOption( option)
            case "poiSearchMap":
                guard let args = call.arguments as? [String: Any],
                      let city = args["city"] as? String,
                      let keyWord = args["keyWord"] as? String,
                      let apiKey = args["apiKey"] as? String,
                      let secretKey = args["secretKey"] as? String
                       else {
                    result(FlutterError(code: "INVALID_ARGUMENTS", message: "Invalid arguments for poiSearchMap", details: nil))
                    return
                }
                
                if search == nil {
                    result(FlutterError(code: "NOT_INITIALIZED", message: "Search manager not initialized", details: nil))
                    return
                }
                
                QMSSearchServices.shared().apiKey = apiKey
                QMSSearchServices.shared().secretKey = secretKey
                TencentMapSdkApi.pendingResult = result
                
                let option = QMSPoiSearchOption()
                option.keyword = keyWord

                option.setBoundaryByRegionWithCityName(city, autoExtend: true)
                TencentMapSdkApi.search?.searchWithPoiSearchOption(option)
//                TencentMapSdkApi.search?.searchWithSuggestionSearchOption(option)
                
            default:
                result(FlutterMethodNotImplemented)
            }
        }
    }
    
    // MARK: - QMSSearchDelegate
    
    func search(with searchOption: QMSSearchOption, didFailWithError error: Error) {
        guard let result = TencentMapSdkApi.pendingResult else {
            return
        }
        
        result(FlutterError(code: "-1", message: error.localizedDescription, details: nil))
        
        // 清除保存的数据
        TencentMapSdkApi.pendingResult = nil
        TencentMapSdkApi.pendingLatitude = nil
        TencentMapSdkApi.pendingLongitude = nil
    }
    
    func search(with suggestionSearchOption: QMSSuggestionSearchOption, didReceive suggestionSearchResult: QMSSuggestionResult) {
        print("suggestionSearchResult ]\(suggestionSearchResult) \(suggestionSearchResult.dataArray.count)");
        guard let result = TencentMapSdkApi.pendingResult else {
            return
        }
        
        var poiList = suggestionSearchResult.dataArray.map { poi -> [String: Any] in
            return [
                "title": poi.title,
                "address": poi.address,
                "latitude": poi.location.latitude,
                "longitude": poi.location.longitude,
                "city":poi.city,
                "province":poi.province,
                "district":poi.district,
            ]
        }

        result(poiList)
        
    }

    func search(with geoCodeSearchOption: QMSGeoCodeSearchOption, didReceive geoCodeSearchResult: QMSGeoCodeSearchResult) {
        guard let result = TencentMapSdkApi.pendingResult else {
            return
        }
        let title = "\(geoCodeSearchResult.address_components.city ?? "")\(geoCodeSearchResult.address_components.district ?? "")\(geoCodeSearchResult.address_components.street ?? "")\(geoCodeSearchResult.address_components.street_number ?? "")"
        let resultMap = [
            "latitude": geoCodeSearchResult.location.latitude,
            "longitude": geoCodeSearchResult.location.longitude,
            "title": title,
            "address": title,
            "province": geoCodeSearchResult.address_components.province,
            "city": geoCodeSearchResult.address_components.city,
            "district": geoCodeSearchResult.address_components.district,
        ] as [String : Any]
        
        result(resultMap)
        TencentMapSdkApi.pendingResult = nil
    }

    
    // - (void)searchWithReverseGeoCodeSearchOption:(QMSReverseGeoCodeSearchOption *)reverseGeoCodeSearchOption didReceiveResult:(QMSReverseGeoCodeSearchResult *)reverseGeoCodeSearchResult;
    func search(with reverseGeoCodeSearchOption: QMSReverseGeoCodeSearchOption, didReceive reverseGeoCodeSearchResult: QMSReverseGeoCodeSearchResult) {
        print("searchWithReverseGeoCodeSearchOption")
    
        guard let result = TencentMapSdkApi.pendingResult,
              let latitude = TencentMapSdkApi.pendingLatitude,
              let longitude = TencentMapSdkApi.pendingLongitude else {
            return
        }
        
        
        let resultMap = [
            "province": reverseGeoCodeSearchResult.address_component.province ?? "",
            "city": reverseGeoCodeSearchResult.address_component.city ?? "",
            "district": reverseGeoCodeSearchResult.address_component.district ?? "",
            "address": reverseGeoCodeSearchResult.address ?? "",
            "title": reverseGeoCodeSearchResult.address ?? "",
            "latitude": latitude,
            "longitude": longitude
        ] as [String : Any]
        
        result(resultMap)
        
        // 清除保存的数据
        TencentMapSdkApi.pendingResult = nil
        TencentMapSdkApi.pendingLatitude = nil
        TencentMapSdkApi.pendingLongitude = nil
    }
}
