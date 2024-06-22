import 'package:flutter/services.dart';

import '../model/analysing_data.dart';

abstract interface class DogAnalyzerController {
  static DogAnalyzerController? _instance;

  static DogAnalyzerController get instance {
    return _instance ??= _DogAnalyzerController();
  }

  Future<int?> startCamera();

  Future<void> stopCamera();

  Stream<AnalysingData?> get analysingDataStream;
}

final class _DogAnalyzerController implements DogAnalyzerController {
  static const MethodChannel methodChannel = MethodChannel(
    'example/dog_analyzer',
  );

  static const EventChannel eventChannel = EventChannel(
    'example/dog_analyzer.stream',
  );

  @override
  Stream<AnalysingData?> get analysingDataStream =>
      eventChannel.receiveBroadcastStream().map(_decodeAnalysingDataEvent);

  @override
  Future<int?> startCamera() => methodChannel.invokeMethod<int>('startCamera');

  @override
  Future<void> stopCamera() => methodChannel.invokeMethod<int>('stopCamera');

  AnalysingData? _decodeAnalysingDataEvent(Object? event) {
    if (event is! Map) {
      return null;
    }

    return AnalysingData.fromJson(
      event.cast<String, Object?>(),
    );
  }
}
