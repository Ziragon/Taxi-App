import 'dart:async';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:geolocator/geolocator.dart';
import 'package:latlong2/latlong.dart';
import 'package:http/http.dart' as http;

import 'package:arbuz_express/widgets/app_ui.dart';
import 'package:arbuz_express/screens/profile_screen.dart';
import 'package:arbuz_express/screens/homeScreens/verification_banner.dart';
import 'package:arbuz_express/CustomTextField/HomeMapScreen/pickup_marker.dart';

import '../CustomTextField/HomeMapScreen/car_marker.dart';
import '../CustomTextField/HomeMapScreen/driver_online_toggle.dart';
import '../CustomTextField/HomeMapScreen/incoming_order_dialog.dart';
import '../CustomTextField/HomeMapScreen/driver_active_order_panel.dart';

class DriverMapScreen extends StatefulWidget {
  final bool showVerificationBanner;

  const DriverMapScreen({super.key, this.showVerificationBanner = false});

  @override
  State<DriverMapScreen> createState() => _DriverMapScreenState();
}

class _DriverMapScreenState extends State<DriverMapScreen> {
  static const LatLng _initialCenter = LatLng(55.0084, 82.9357);
  final MapController _mapController = MapController();

  LatLng? _currentPosition;
  LatLng? _clientPosition;
  List<LatLng> _routePoints = [];

  bool _isOnline = false;
  bool _isOrderActive = false;
  Timer? _searchTimer;

  final String _mockClientName = 'Алексей Д.';
  final String _mockClientRating = '4.9';
  String _mockFromAddress = 'Комсомольская улица, 2';
  final String _mockToAddress = 'ул. Кирова, 113';

  @override
  void initState() {
    super.initState();
    _getCurrentLocation();
  }

  Future<void> _getCurrentLocation() async {
    try {
      bool serviceEnabled = await Geolocator.isLocationServiceEnabled();
      if (!serviceEnabled) return;
      LocationPermission permission = await Geolocator.checkPermission();
      if (permission == LocationPermission.denied) {
        permission = await Geolocator.requestPermission();
      }
      if (permission == LocationPermission.deniedForever) return;

      Position position = await Geolocator.getCurrentPosition();
      setState(() {
        _currentPosition = LatLng(position.latitude, position.longitude);
      });
      _mapController.move(_currentPosition!, 15.0);
    } catch (e) {
      setState(() {
        _currentPosition = _initialCenter;
      });
    }
  }

  void _toggleOnlineStatus() {
    setState(() {
      _isOnline = !_isOnline;
      if (!_isOnline) {
        _searchTimer?.cancel();
      } else {
        _searchTimer = Timer(const Duration(seconds: 5), _showIncomingOrder);
      }
    });
  }

  void _showIncomingOrder() {
    if (!mounted) return;

    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => IncomingOrderDialog(
        clientName: _mockClientName,
        rating: _mockClientRating,
        fromAddress: _mockFromAddress,
        toAddress: _mockToAddress,
        onAccept: () {
          Navigator.pop(context);
          _acceptOrder();
        },
        onDecline: () {
          Navigator.pop(context);
          setState(() {
            _isOnline = false;
          });
        },
      ),
    );
  }

  Future<void> _acceptOrder() async {
    if (_currentPosition == null) return;

    await _getAddressCoordinates();

    if (_clientPosition != null) {
      setState(() {
        _isOrderActive = true;
      });
      _buildRouteToClient();
    }
  }

  Future<void> _getAddressCoordinates() async {
    try {
      final url = Uri.parse(
        'https://nominatim.openstreetmap.org/search?q=${Uri.encodeComponent('Комсомольская улица, 2, Новосибирск')}&format=json&limit=1&addressdetails=1&countrycodes=ru',
      );
      final response = await http.get(
        url,
        headers: {'User-Agent': 'ArbuzExpressApp'},
      );

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        if (data.isNotEmpty) {
          final lat = double.parse(data[0]['lat']);
          final lon = double.parse(data[0]['lon']);
          setState(() {
            _clientPosition = LatLng(lat, lon);
          });

          final address = data[0]['address'];
          if (address != null) {
            final street =
                address['road'] ??
                address['residential'] ??
                'Комсомольская улица';
            final house = address['house_number'] ?? '2';
            _mockFromAddress = '$street, $house';
          }
        }
      }
    } catch (e) {
      setState(() {
        _clientPosition = const LatLng(55.0305, 82.9200);
      });
    }
  }

  Future<void> _buildRouteToClient() async {
    if (_currentPosition == null || _clientPosition == null) return;

    try {
      final url = Uri.parse(
        'https://router.project-osrm.org/route/v1/driving/${_currentPosition!.longitude},${_currentPosition!.latitude};${_clientPosition!.longitude},${_clientPosition!.latitude}?overview=full&geometries=geojson',
      );
      final response = await http.get(url);

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        if (data['routes'] != null && data['routes'].isNotEmpty) {
          final List coordinates = data['routes'][0]['geometry']['coordinates'];
          setState(() {
            _routePoints = coordinates
                .map((c) => LatLng(c[1].toDouble(), c[0].toDouble()))
                .toList();
          });
          try {
            final bounds = LatLngBounds.fromPoints([
              _currentPosition!,
              _clientPosition!,
              ..._routePoints,
            ]);

            _mapController.fitCamera(
              CameraFit.bounds(
                bounds: bounds,
                padding: const EdgeInsets.all(80),
                maxZoom: 15.0,
              ),
            );
          } catch (e) {
            final centerLat =
                (_currentPosition!.latitude + _clientPosition!.latitude) / 2;
            final centerLng =
                (_currentPosition!.longitude + _clientPosition!.longitude) / 2;
            _mapController.move(LatLng(centerLat, centerLng), 13.0);
          }
        }
      }
    } catch (e) {
      debugPrint('Ошибка построения маршрута: $e');
    }
  }

  void _finishOrCancelOrder() {
    setState(() {
      _isOrderActive = false;
      _clientPosition = null;
      _routePoints = [];
      _isOnline = false;
    });

    if (_currentPosition != null) {
      _mapController.move(_currentPosition!, 15.0);
    }
  }

  @override
  void dispose() {
    _searchTimer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0A0A0C),
      body: Stack(
        children: [
          Positioned.fill(
            child: FlutterMap(
              mapController: _mapController,
              options: MapOptions(
                initialCenter: _initialCenter,
                initialZoom: 14.5,
                minZoom: 10.0,
                maxZoom: 18.0,
                cameraConstraint: CameraConstraint.contain(
                  bounds: LatLngBounds(
                    const LatLng(-90, -180),
                    const LatLng(90, 180),
                  ),
                ),
              ),
              children: [
                TileLayer(
                  urlTemplate: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
                  userAgentPackageName: 'com.arbuzexpress.app',
                  retinaMode: true,
                ),
                if (_routePoints.isNotEmpty)
                  PolylineLayer(
                    polylines: [
                      Polyline(
                        points: _routePoints,
                        strokeWidth: 5.0,
                        color: const Color(0xFFFFC107),
                        strokeCap: StrokeCap.round,
                        strokeJoin: StrokeJoin.round,
                      ),
                    ],
                  ),
                MarkerLayer(
                  markers: [
                    if (_currentPosition != null)
                      Marker(
                        point: _currentPosition!,
                        width: 44,
                        height: 44,
                        child: const CarMarker(),
                      ),
                    if (_clientPosition != null)
                      Marker(
                        point: _clientPosition!,
                        width: 56,
                        height: 70,
                        child: const PickupMarker(),
                      ),
                  ],
                ),
              ],
            ),
          ),

          Positioned(
            top: 0,
            right: 0,
            child: SafeArea(
              child: Padding(
                padding: const EdgeInsets.only(right: 16, top: 16),
                child: CircleIconButton(
                  icon: Icons.person_rounded,
                  onTap: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (context) => const ProfileScreen(),
                      ),
                    );
                  },
                  color: const Color(0xFF1A1A1E),
                ),
              ),
            ),
          ),

          if (widget.showVerificationBanner)
            const Positioned(
              top: 86,
              left: 0,
              right: 0,
              child: SafeArea(child: VerificationBanner()),
            ),

          if (_isOrderActive)
            Positioned(
              bottom: 0,
              left: 0,
              right: 0,
              child: SafeArea(
                child: Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: DriverActiveOrderPanel(
                    clientName: _mockClientName,
                    fromAddress: _mockFromAddress,
                    onArrived: _finishOrCancelOrder,
                    onCancel: _finishOrCancelOrder,
                  ),
                ),
              ),
            ),

          if (!_isOrderActive)
            Positioned(
              bottom: 30,
              left: 0,
              right: 0,
              child: Center(
                child: DriverOnlineToggle(
                  isOnline: _isOnline,
                  onToggle: _toggleOnlineStatus,
                ),
              ),
            ),
        ],
      ),
    );
  }
}
