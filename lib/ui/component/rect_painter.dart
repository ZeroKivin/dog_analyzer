import 'package:dog_analyzer/model/analysing_data.dart';
import 'package:flutter/material.dart';

class AnalysingDataVisualizer extends StatelessWidget {
  final Stream<AnalysingData?>? analysingDataStream;
  final Widget cameraView;

  const AnalysingDataVisualizer({
    required this.analysingDataStream,
    required this.cameraView,
    super.key,
  });

  @override
  Widget build(BuildContext context) {
    return StreamBuilder<AnalysingData?>(
      stream: analysingDataStream,
      builder: (_, snapshot) {
        final data = snapshot.data;

        return CustomPaint(
          foregroundPainter: data != null ? _RectPainter(data) : null,
          child: cameraView,
        );
      },
    );
  }
}

class _RectPainter extends CustomPainter {
  final AnalysingData data;

  const _RectPainter(this.data);

  @override
  void paint(Canvas canvas, Size size) {
    final rect = Rect.fromLTRB(data.left, data.top, data.right, data.bottom);

    TextPainter textPainter = TextPainter(
      text: TextSpan(
        text: data.result,
        style: const TextStyle(
          color: Colors.white,
          fontSize: 20,
          backgroundColor: Colors.black26,
        ),
      ),
      maxLines: 2,
      textDirection: TextDirection.ltr,
    );

    final paint = Paint()
      ..color = Colors.red
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2.0;

    canvas.drawRect(rect, paint);

    textPainter.layout(maxWidth: size.width);
    textPainter.paint(
      canvas,
      Offset(
        (rect.width - textPainter.width).abs() * 0.5,
        rect.top - textPainter.height,
      ),
    );
  }

  @override
  bool shouldRepaint(_RectPainter oldDelegate) => oldDelegate.data != data;
}
