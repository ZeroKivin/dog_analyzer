import 'dart:async';

import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';

import '../domain/dog_analyzer_controller.dart';
import 'component/rect_painter.dart';

class CameraPage extends StatefulWidget {
  const CameraPage({
    super.key,
  });

  @override
  State<CameraPage> createState() => _CameraPageState();
}

class _CameraPageState extends State<CameraPage> {
  final Completer<int?> _textureIdCompleter = Completer();

  @override
  void initState() {
    super.initState();

    _requestPermissionIfNeed().then((_) async {
      _textureIdCompleter.complete(
        DogAnalyzerController.instance.startCamera(),
      );
    });
  }

  @override
  void dispose() {
    DogAnalyzerController.instance.stopCamera();

    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(),
        body: Center(
          child: AnalysingDataVisualizer(
            analysingDataStream:
                DogAnalyzerController.instance.analysingDataStream,
            cameraView: FutureBuilder<int?>(
              future: _textureIdCompleter.future,
              builder: (_, snapshot) {
                final textureId = snapshot.data;

                if (textureId == null) {
                  return const Center(
                    child: CircularProgressIndicator(),
                  );
                }

                return Texture(
                  key: ValueKey(textureId),
                  textureId: textureId,
                );
              },
            ),
          ),
        ),
      );

  Future<void> _requestPermissionIfNeed() async {
    const cameraPermission = Permission.camera;

    if (await cameraPermission.status != PermissionStatus.granted) {
      await cameraPermission.request();
    }
  }
}
