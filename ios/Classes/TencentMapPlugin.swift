import Flutter
import UIKit

public class TencentMapPlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    registrar.register(TencentMapFactory(registrar: registrar), withId: "tencent_map_flutter")
    TencentMapSdkApi.setup(registrar: registrar)
  }
}
