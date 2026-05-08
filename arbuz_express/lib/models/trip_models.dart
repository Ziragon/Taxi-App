class RouteGeometry {
  final List<LatLngPoint> coordinates;

  RouteGeometry({required this.coordinates});

  factory RouteGeometry.fromJson(dynamic json) {
    if (json is String) {
      return RouteGeometry(coordinates: _decodePolyline(json));
    }
    if (json is Map<String, dynamic>) {
      var list = json['coordinates'] as List? ?? [];
      return RouteGeometry(
        coordinates: list.map((e) => LatLngPoint.fromJson(e)).toList(),
      );
    }
    return RouteGeometry(coordinates: []);
  }
}

class LatLngPoint {
  final double latitude;
  final double longitude;

  LatLngPoint({required this.latitude, required this.longitude});

  factory LatLngPoint.fromJson(dynamic json) {
    if (json is List) {
      return LatLngPoint(
        latitude: _parseDouble(json[1]),
        longitude: _parseDouble(json[0]),
      );
    }
    return LatLngPoint(
      latitude: _parseDouble(json['lat']),
      longitude: _parseDouble(json['lng']),
    );
  }
}

class TripCalculationRequest {
  final String originAddress;
  final double originLat;
  final double originLng;
  final String destAddress;
  final double destLat;
  final double destLng;

  TripCalculationRequest({
    required this.originAddress,
    required this.originLat,
    required this.originLng,
    required this.destAddress,
    required this.destLat,
    required this.destLng,
  });

  Map<String, dynamic> toJson() => {
    'originAddress': originAddress,
    'originLat': originLat,
    'originLng': originLng,
    'destAddress': destAddress,
    'destLat': destLat,
    'destLng': destLng,
  };
}

class Tariff {
  final int id;
  final String tripClass;
  final int baseFare;
  final int pricePerKm;
  final int distanceCost;
  final int pricePerMin;
  final int durationCost;
  final int price;
  final int carsNearby;

  Tariff({
    required this.id,
    required this.tripClass,
    required this.baseFare,
    required this.pricePerKm,
    required this.distanceCost,
    required this.pricePerMin,
    required this.durationCost,
    required this.price,
    required this.carsNearby,
  });

  factory Tariff.fromJson(Map<String, dynamic> json) {
    return Tariff(
      id: _parseInt(json['id']),
      tripClass: _parseString(json['tripClass']),
      baseFare: _parseInt(json['baseFare']),
      pricePerKm: _parseInt(json['pricePerKm']),
      distanceCost: _parseInt(json['distanceCost']),
      pricePerMin: _parseInt(json['pricePerMin']),
      durationCost: _parseInt(json['durationCost']),
      price: _parseInt(json['price']),
      carsNearby: _parseInt(json['carsNearby']),
    );
  }
}

class TripCalculationResponse {
  final int id;
  final int passengerId;
  final String status;
  final String originAddress;
  final double originLat;
  final double originLng;
  final String destAddress;
  final double destLat;
  final double destLng;
  final double distanceKm;
  final int durationMin;
  final double weatherCoef;
  final double surgeCoef;
  final List<Tariff> tariffs;
  final RouteGeometry? routeGeometry;

  TripCalculationResponse({
    required this.id,
    required this.passengerId,
    required this.status,
    required this.originAddress,
    required this.originLat,
    required this.originLng,
    required this.destAddress,
    required this.destLat,
    required this.destLng,
    required this.distanceKm,
    required this.durationMin,
    required this.weatherCoef,
    required this.surgeCoef,
    required this.tariffs,
    this.routeGeometry,
  });

  factory TripCalculationResponse.fromJson(Map<String, dynamic> json) {
    return TripCalculationResponse(
      id: _parseInt(json['id']),
      passengerId: _parseInt(json['passengerId']),
      status: _parseString(json['status']),
      originAddress: _parseString(json['originAddress']),
      originLat: _parseDouble(json['originLat']),
      originLng: _parseDouble(json['originLng']),
      destAddress: _parseString(json['destAddress']),
      destLat: _parseDouble(json['destLat']),
      destLng: _parseDouble(json['destLng']),
      distanceKm: _parseDouble(json['distanceKm']),
      durationMin: _parseInt(json['durationMin']),
      weatherCoef: _parseDouble(json['weatherCoef']),
      surgeCoef: _parseDouble(json['surgeCoef']),
      tariffs: _parseTariffs(json['tariffs']),
      routeGeometry: json['routeGeometry'] != null
          ? RouteGeometry.fromJson(json['routeGeometry'])
          : null,
    );
  }
}

class TripDetails {
  final int id;
  final int passengerId;
  final String status;
  final String originAddress;
  final double originLat;
  final double originLng;
  final String destAddress;
  final double destLat;
  final double destLng;
  final double distanceKm;
  final int durationMin;
  final double weatherCoef;
  final double surgeCoef;
  final List<Tariff> tariffs;
  final RouteGeometry? routeGeometry;

  TripDetails({
    required this.id,
    required this.passengerId,
    required this.status,
    required this.originAddress,
    required this.originLat,
    required this.originLng,
    required this.destAddress,
    required this.destLat,
    required this.destLng,
    required this.distanceKm,
    required this.durationMin,
    required this.weatherCoef,
    required this.surgeCoef,
    required this.tariffs,
    this.routeGeometry,
  });

  factory TripDetails.fromJson(Map<String, dynamic> json) {
    return TripDetails(
      id: _parseInt(json['id']),
      passengerId: _parseInt(json['passengerId']),
      status: _parseString(json['status']),
      originAddress: _parseString(json['originAddress']),
      originLat: _parseDouble(json['originLat']),
      originLng: _parseDouble(json['originLng']),
      destAddress: _parseString(json['destAddress']),
      destLat: _parseDouble(json['destLat']),
      destLng: _parseDouble(json['destLng']),
      distanceKm: _parseDouble(json['distanceKm']),
      durationMin: _parseInt(json['durationMin']),
      weatherCoef: _parseDouble(json['weatherCoef']),
      surgeCoef: _parseDouble(json['surgeCoef']),
      tariffs: _parseTariffs(json['tariffs']),
      routeGeometry: json['routeGeometry'] != null
          ? RouteGeometry.fromJson(json['routeGeometry'])
          : null,
    );
  }
}

int _parseInt(dynamic value) {
  if (value == null) return 0;
  if (value is int) return value;
  if (value is double) return value.toInt();
  if (value is String) return int.tryParse(value) ?? 0;
  return 0;
}

double _parseDouble(dynamic value) {
  if (value == null) return 0.0;
  if (value is double) return value;
  if (value is int) return value.toDouble();
  if (value is String) return double.tryParse(value) ?? 0.0;
  return 0.0;
}

String _parseString(dynamic value) {
  if (value == null) return '';
  return value.toString();
}

List<Tariff> _parseTariffs(dynamic value) {
  if (value == null) return [];
  if (value is! List) return [];
  return value
      .map((e) {
        if (e is Map<String, dynamic>) {
          return Tariff.fromJson(e);
        }
        return null;
      })
      .whereType<Tariff>()
      .toList();
}

List<LatLngPoint> _decodePolyline(String encoded) {
  List<LatLngPoint> points = [];
  int index = 0, len = encoded.length;
  int lat = 0, lng = 0;

  while (index < len) {
    int b, shift = 0, result = 0;
    do {
      b = encoded.codeUnitAt(index++) - 63;
      result |= (b & 0x1f) << shift;
      shift += 5;
    } while (b >= 0x20);
    int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
    lat += dlat;

    shift = 0;
    result = 0;
    do {
      b = encoded.codeUnitAt(index++) - 63;
      result |= (b & 0x1f) << shift;
      shift += 5;
    } while (b >= 0x20);
    int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
    lng += dlng;

    points.add(LatLngPoint(
      latitude: lat / 1E5,
      longitude: lng / 1E5,
    ));
  }
  return points;
}