base class AnalysingData {
  final double left;
  final double top;
  final double right;
  final double bottom;
  final String result;

  AnalysingData({
    required this.left,
    required this.top,
    required this.right,
    required this.bottom,
    required this.result,
  });

  factory AnalysingData.fromJson(Map<String, Object?> json) {
    return AnalysingData(
      left: json['left'] as double,
      top: json['top'] as double,
      right: json['right'] as double,
      bottom: json['bottom'] as double,
      result: json['result'] as String,
    );
  }
}
