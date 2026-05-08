import 'dart:async';
import 'dart:convert';
import 'package:arbuz_express/services/token_storage.dart';
import 'package:arbuz_express/screens/driverScreensWidgets/status_notification.dart';
import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:geolocator/geolocator.dart';
import 'package:latlong2/latlong.dart';
import 'package:http/http.dart' as http;
import 'package:arbuz_express/widgets/app_ui.dart';
import 'package:arbuz_express/screens/profile_screen.dart';
import 'package:arbuz_express/screens/homeScreensWidgets/verification_banner.dart';
import 'package:arbuz_express/screens/menuScreens/notifications_panel.dart';
import 'package:arbuz_express/screens/menuScreens/notifications_button.dart';
import 'package:arbuz_express/CustomTextField/HomeMapScreen/pickup_marker.dart';
import 'package:arbuz_express/hooks/use_driver_status.dart';
import 'package:arbuz_express/services/websocket_manager.dart';
import 'driverScreensWidgets/car_marker.dart';
import 'driverScreensWidgets/driver_online_toggle.dart';
import 'driverScreensWidgets/incoming_order_dialog.dart';
import 'driverScreensWidgets/driver_active_order_panel.dart';

class DriverMapScreen extends StatefulWidget {
  final bool showVerificationBanner;
  const DriverMapScreen({super.key, this.showVerificationBanner = false});
  @override
  State<DriverMapScreen> createState() => _DriverMapScreenState();
}

class _DriverMapScreenState extends State<DriverMapScreen> {
  static const LatLng _initialCenter = LatLng(55.0084, 82.9357);
  final MapController _mapController = MapController();
  final UseDriverStatus _statusHook = UseDriverStatus();
  final WebSocketManager _wsManager = WebSocketManager();

  LatLng? _currentPosition;
  LatLng? _clientPosition;
  List<LatLng> _routePoints = [];

  bool _isOnline = false;
  bool _isOrderActive = false;
  bool _isUpdating = false;
  Timer? _searchTimer;
  Timer? _locationUpdateTimer;

  final String _mockClientName = 'Алексей Д.';
  final String _mockClientRating = '4.9';
  String _mockFromAddress = 'Комсомольская улица, 2';
  final String _mockToAddress = 'ул. Кирова, 113';
  final Map<String, String> _mockPreferences = {
    'Кальян': 'разогреть',
    'Музыка': 'глухой водитель',
  };

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
      if (mounted) {
        setState(() {
          _currentPosition = LatLng(position.latitude, position.longitude);
        });
        _mapController.move(_currentPosition!, 15.0);
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _currentPosition = _initialCenter;
        });
      }
    }
  }

  void _startLocationUpdates() {
    _locationUpdateTimer?.cancel();
    _locationUpdateTimer = Timer.periodic(const Duration(seconds: 5), (timer) {
      _sendCurrentLocation();
    });
  }

  void _stopLocationUpdates() {
    _locationUpdateTimer?.cancel();
    _locationUpdateTimer = null;
  }

  Future<void> _sendCurrentLocation() async {
    if (!_isOnline) return;

    try {
      Position position = await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.high,
      );

      if (mounted) {
        setState(() {
          _currentPosition = LatLng(position.latitude, position.longitude);
        });
      }

      final locationData = {
        'lat': position.latitude,
        'lng': position.longitude,
        'timestamp': DateTime.now().toIso8601String(),
      };

      _wsManager.send('/app/driver/location', jsonEncode(locationData));
    } catch (e) {
      debugPrint(e.toString());
    }
  }

  Future<void> _toggleOnlineStatus() async {
    if (_isUpdating) return;

    if (TokenStorage.accessToken == null) return;

    setState(() => _isUpdating = true);
    final bool targetStatus = !_isOnline;

    try {
      final result = targetStatus
          ? await _statusHook.setOnline()
          : await _statusHook.setOffline();

      if (mounted) {
        if (result['success']) {
          setState(() {
            _isOnline = targetStatus;
            if (!_isOnline) {
              _searchTimer?.cancel();
              _stopLocationUpdates();
            } else {
              _startLocationUpdates();
              _searchTimer = Timer(
                const Duration(seconds: 3),
                _showIncomingOrder,
              );
            }
          });
        } else {
          final message = result['message'] as String;
          final code = _parseErrorCode(message);
          showStatusNotification(context, message: message, code: code);
        }
      }
    } catch (e) {
      if (mounted) {
        showStatusNotification(context, message: 'Ошибка', code: 'ERROR');
      }
    } finally {
      if (mounted) setState(() => _isUpdating = false);
    }
  }

  String _parseErrorCode(String message) {
    final lowerMessage = message.toLowerCase();
    if (lowerMessage.contains('vehicle') && lowerMessage.contains('active'))
      return 'VEHICLE_MISSING';
    if (lowerMessage.contains('vehicle') && lowerMessage.contains('verified'))
      return 'VEHICLE_NOT_VERIFIED';
    if (lowerMessage.contains('account') || lowerMessage.contains('profile'))
      return 'ACCOUNT_NOT_VERIFIED';
    return 'FORBIDDEN';
  }

  void _showIncomingOrder() {
    if (!mounted || !_isOnline || _isOrderActive) return;
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => IncomingOrderDialog(
        clientName: _mockClientName,
        rating: _mockClientRating,
        fromAddress: _mockFromAddress,
        toAddress: _mockToAddress,
        preferences: _mockPreferences,
        price: '₽ 500',
        onAccept: () {
          Navigator.pop(context);
          _acceptOrder();
        },
        onDecline: () {
          Navigator.pop(context);
          _toggleOnlineStatus();
        },
      ),
    );
  }

  Future<void> _acceptOrder() async {
    await _getAddressCoordinates();
    if (mounted) {
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
          if (mounted) {
            setState(() {
              _clientPosition = LatLng(lat, lon);
            });
          }
        }
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _clientPosition = const LatLng(55.0305, 82.9200);
        });
      }
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
          if (mounted) {
            setState(() {
              _routePoints = coordinates
                  .map((c) => LatLng(c[1].toDouble(), c[0].toDouble()))
                  .toList();
            });
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
          }
        }
      }
    } catch (e) {
      debugPrint(e.toString());
    }
  }

  void _finishOrCancelOrder() {
    setState(() {
      _isOrderActive = false;
      _clientPosition = null;
      _routePoints = [];
      _isOnline = false;
    });
    _stopLocationUpdates();
    if (_currentPosition != null) {
      _mapController.move(_currentPosition!, 15.0);
    }
  }

  Future<void> _updateLocationManually() async {
    try {
      Position position = await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.high,
        timeLimit: const Duration(seconds: 10),
      );

      if (mounted) {
        setState(() {
          _currentPosition = LatLng(position.latitude, position.longitude);
        });
        _mapController.move(_currentPosition!, 15.0);

        if (_isOnline) {
          final locationData = {
            'lat': position.latitude,
            'lng': position.longitude,
            'timestamp': DateTime.now().toIso8601String(),
          };
          _wsManager.send('/app/driver/location', jsonEncode(locationData));
        }

        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Геолокация обновлена'),
            duration: Duration(seconds: 2),
          ),
        );
      }
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Не удалось получить геолокацию'),
          duration: Duration(seconds: 2),
        ),
      );
    }
  }

  void _showNotifications() {
    showDialog(
      context: context,
      builder: (context) =>
          NotificationsPanel(onClose: () => Navigator.pop(context)),
    );
  }

  @override
  void dispose() {
    _searchTimer?.cancel();
    _locationUpdateTimer?.cancel();
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
              ),
              children: [
                TileLayer(
                  urlTemplate:
                      'https://{s}.tile.openstreetmap.de/tiles/osmde/{z}/{x}/{y}.png',
                  subdomains: const ['a', 'b', 'c'],
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
            left: 0,
            child: SafeArea(
              child: Padding(
                padding: const EdgeInsets.only(left: 16, top: 16),
                child: NotificationsButton(onPressed: _showNotifications),
              ),
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
                  onTap: () => Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (context) => const ProfileScreen(),
                    ),
                  ),
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
                    price: '₽ 500',
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
                child: _isUpdating
                    ? const CircularProgressIndicator(color: Color(0xFFFFC107))
                    : DriverOnlineToggle(
                        isOnline: _isOnline,
                        onToggle: _toggleOnlineStatus,
                      ),
              ),
            ),
          Positioned(
            bottom: 100,
            right: 16,
            child: SafeArea(
              child: FloatingActionButton(
                onPressed: _updateLocationManually,
                backgroundColor: const Color(0xFFFFC107),
                foregroundColor: Colors.black,
                child: const Icon(Icons.my_location),
                tooltip: 'Обновить геолокацию',
              ),
            ),
          ),
        ],
      ),
    );
  }
}
