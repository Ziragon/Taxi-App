import 'dart:convert';
import 'package:arbuz_express/CustomTextField/HomeMapScreen/pickup_marker.dart';
import 'package:arbuz_express/CustomTextField/HomeMapScreen/destination_marker.dart';
import 'package:arbuz_express/hooks/use_driver_status.dart';
import 'package:arbuz_express/models/trip_models.dart';
import 'package:arbuz_express/screens/driverScreensWidgets/driver_active_order_panel.dart';
import 'package:arbuz_express/screens/driverScreensWidgets/driver_online_toggle.dart';
import 'package:arbuz_express/screens/driverScreensWidgets/driver_trip_in_progress_panel.dart';
import 'package:arbuz_express/screens/driverScreensWidgets/incoming_order_dialog.dart';
import 'package:arbuz_express/screens/driverScreensWidgets/status_notification.dart';
import 'package:arbuz_express/screens/homeScreensWidgets/verification_banner.dart';
import 'package:arbuz_express/screens/menuScreens/notifications_button.dart';
import 'package:arbuz_express/screens/menuScreens/notifications_panel.dart';
import 'package:arbuz_express/screens/profile_screen.dart';
import 'package:arbuz_express/services/token_storage.dart';
import 'package:arbuz_express/services/trip_service.dart';
import 'package:arbuz_express/services/websocket_manager.dart';
import 'package:arbuz_express/widgets/app_ui.dart';
import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:geolocator/geolocator.dart';
import 'package:http/http.dart' as http;
import 'package:latlong2/latlong.dart';
import 'driverScreensWidgets/car_marker.dart';

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

  final List<Map<String, dynamic>> _notifications = [];
  LatLng? _currentPosition;
  LatLng? _clientPosition;
  LatLng? _destinationPosition;
  List<LatLng> _routePoints = [];

  bool _isOnline = false;
  bool _isOrderActive = false;
  bool _isUpdating = false;
  bool _hasArrivedAtPickup = false;
  bool _isIncomingOrderDialogVisible = false;
  int? _currentTripId;
  TripDetails? _activeTrip;

  String _currentClientName = 'Новый пассажир';
  String _currentClientRating = '5.0';
  String _fromAddress = 'Неизвестно';
  String _toAddress = 'Неизвестно';
  Map<String, String> _currentPreferences = {};
  String _incomingPrice = '₽ 0';

  @override
  void initState() {
    super.initState();
    _getCurrentLocation();
    _registerNotificationListener();
  }

  Future<void> _getCurrentLocation() async {
    try {
      final serviceEnabled = await Geolocator.isLocationServiceEnabled();
      if (!serviceEnabled) return;

      var permission = await Geolocator.checkPermission();
      if (permission == LocationPermission.denied) {
        permission = await Geolocator.requestPermission();
      }
      if (permission == LocationPermission.deniedForever) return;

      final position = await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.high,
      );
      if (!mounted) return;

      setState(() {
        _currentPosition = LatLng(position.latitude, position.longitude);
      });
      _mapController.move(_currentPosition!, 15.0);
      await _checkActiveTrip();
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _currentPosition = _initialCenter;
      });
    }
  }

  Future<void> _checkActiveTrip() async {
    if (_currentPosition == null) return;

    try {
      final trip = await TripService.getDriverActiveTrip(
        _currentPosition!.latitude,
        _currentPosition!.longitude,
      );
      if (!mounted) return;

      final routePoints = trip.routeGeometry == null
          ? <LatLng>[]
          : trip.routeGeometry!.coordinates
                .map((point) => LatLng(point.latitude, point.longitude))
                .toList();

      setState(() {
        _activeTrip = trip;
        _currentTripId = trip.id;
        _isOrderActive = true;
        _hasArrivedAtPickup = trip.status == 'IN_PROGRESS';
        _fromAddress = trip.originAddress;
        _toAddress = trip.destAddress;
        _clientPosition = LatLng(trip.originLat, trip.originLng);
        _destinationPosition = LatLng(trip.destLat, trip.destLng);
        if (routePoints.isNotEmpty) {
          _routePoints = routePoints;
        }
      });

      if (routePoints.isNotEmpty) {
        _fitRoute(_hasArrivedAtPickup ? _destinationPosition : _clientPosition);
      } else if (_hasArrivedAtPickup) {
        await _buildRouteToDestination();
      } else {
        await _buildRouteToClient();
      }
    } catch (e) {
      debugPrint(e.toString());
    }
  }

  void _registerNotificationListener() {
    _wsManager.onNotification = (notification) {
      if (!mounted) return;

      _addNotification(notification);

      final eventType = notification['eventType']?.toString() ?? '';
      if (_isNewTripNotification(notification) &&
          _isOnline &&
          !_isOrderActive) {
        _handleIncomingTripNotification(notification);
      } else if (eventType == 'TRIP_CANCELLED') {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Поездка была отменена пассажиром'),
            duration: Duration(seconds: 3),
          ),
        );
        _resetTripState();
      }
    };

    if (!_wsManager.isConnected) {
      _wsManager.start('driver');
    }
  }

  void _addNotification(Map<String, dynamic> notification) {
    final key = _notificationKey(notification);
    setState(() {
      _notifications.removeWhere((item) => _notificationKey(item) == key);
      _notifications.insert(0, notification);
    });
  }

  String _notificationKey(Map<String, dynamic> notification) {
    final eventType = notification['eventType']?.toString() ?? '';
    final id =
        notification['id']?.toString() ??
        notification['tripId']?.toString() ??
        (notification['tripData'] is Map
            ? notification['tripData']['id']?.toString()
            : null) ??
        '';
    final timestamp = notification['timestamp']?.toString() ?? '';
    if (id.isNotEmpty) {
      return '$eventType|$id';
    }
    if (timestamp.isNotEmpty) {
      return '$eventType|$timestamp';
    }
    return notification.toString();
  }

  bool _isNewTripNotification(Map<String, dynamic> notification) {
    final eventType = notification['eventType']?.toString().toUpperCase() ?? '';
    final title = notification['title']?.toString().toUpperCase() ?? '';
    final message = notification['message']?.toString().toUpperCase() ?? '';

    if (eventType == 'TRIP_COMPLETED' ||
        eventType == 'TRIP_CANCELLED' ||
        eventType == 'TRIP_STARTED' ||
        eventType == 'DRIVER_ASSIGNED') {
      return false;
    }

    return eventType == 'NEW_TRIP_REQUEST' ||
        eventType == 'TRIP_OFFER' ||
        eventType == 'NEW_ORDER' ||
        eventType == 'TRIP_REQUEST' ||
        title.contains('TRIP_OFFER') ||
        title.contains('NEW_TRIP') ||
        title.contains('NEW') ||
        message.contains('TRIP_OFFER') ||
        message.contains('NEW_TRIP') ||
        message.contains('NEW') ||
        (eventType.isEmpty &&
            notification['tripData'] is Map<String, dynamic>) ||
        (eventType.isEmpty &&
            notification['originAddress'] != null &&
            notification['destAddress'] != null &&
            notification['id'] != null);
  }

  void _handleIncomingTripNotification(Map<String, dynamic> notification) {
    if (!mounted) return;
    _applyIncomingTripData(notification);
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        _showIncomingOrder();
      }
    });
  }

  int? _extractTripId(Map<String, dynamic> notification) {
    final tripIdValue =
        notification['tripId'] ??
        notification['id'] ??
        (notification['tripData'] is Map
            ? notification['tripData']['id']
            : null);
    if (tripIdValue is int) return tripIdValue;
    if (tripIdValue is String) return int.tryParse(tripIdValue);
    return null;
  }

  void _applyIncomingTripData(Map<String, dynamic> data) {
    _currentTripId = _extractTripId(data);

    final tripData = data['tripData'] as Map<String, dynamic>? ?? data;

    _fromAddress = tripData['originAddress'] as String? ?? 'Неизвестно';
    _toAddress = tripData['destAddress'] as String? ?? 'Неизвестно';

    final passengerProfile = data['passengerProfile'] as Map<String, dynamic>?;
    if (passengerProfile != null) {
      final firstName = passengerProfile['firstName'] as String? ?? '';
      final lastName = passengerProfile['lastName'] as String? ?? '';
      _currentClientName = '$firstName $lastName'.trim();
      if (_currentClientName.isEmpty) _currentClientName = 'Новый пассажир';

      _currentClientRating =
          (passengerProfile['averageRating'] as num?)?.toStringAsFixed(1) ??
          '5.0';
    } else {
      _currentClientName =
          tripData['passengerName'] as String? ??
          tripData['clientName'] as String? ??
          'Новый пассажир';
      _currentClientRating =
          (tripData['passengerRating'] as num?)?.toStringAsFixed(1) ??
          (tripData['rating'] as num?)?.toStringAsFixed(1) ??
          '5.0';
    }

    final originLat = (tripData['originLat'] as num?)?.toDouble();
    final originLng = (tripData['originLng'] as num?)?.toDouble();
    final destLat = (tripData['destLat'] as num?)?.toDouble();
    final destLng = (tripData['destLng'] as num?)?.toDouble();

    if (originLat != null && originLng != null) {
      _clientPosition = LatLng(originLat, originLng);
    }
    if (destLat != null && destLng != null) {
      _destinationPosition = LatLng(destLat, destLng);
    }

    final rawPreferences = tripData['preferences'];
    if (rawPreferences is Map) {
      _currentPreferences = rawPreferences.map(
        (key, value) => MapEntry(key.toString(), value.toString()),
      );
    } else {
      _currentPreferences = {};
    }

    _incomingPrice = '₽ 500';
    final price = tripData['price'] ?? data['price'];
    if (price != null) {
      _incomingPrice = '₽ $price';
    } else {
      final tariffs = tripData['tariffs'] as List<dynamic>?;
      if (tariffs != null) {
        for (final tariff in tariffs) {
          if (tariff['tripClass'] == 'COMFORT') {
            _incomingPrice = '₽ ${tariff['price']}';
            break;
          }
        }
      }
    }

    final message = data['message'] as String?;
    if (message != null && message.isNotEmpty) {
      if (_fromAddress == 'Неизвестно' && _toAddress == 'Неизвестно') {
        final parts = message.split('→');
        if (parts.length == 2) {
          _fromAddress = parts[0].replaceAll('Новый заказ:', '').trim();
          _toAddress = parts[1].split(',').first.trim();
        }
      }
      if (_incomingPrice == '₽ 500' || _incomingPrice == '₽ 0') {
        final match = RegExp(r'(\d+(?:\.\d+)?)\s*₽').firstMatch(message);
        if (match != null) {
          _incomingPrice = '₽ ${match.group(1)}';
        }
      }
    }
  }

  Future<void> _toggleOnlineStatus() async {
    if (_isUpdating) return;
    if (TokenStorage.accessToken == null) return;

    setState(() => _isUpdating = true);
    final targetStatus = !_isOnline;

    try {
      final result = targetStatus
          ? await _statusHook.setOnline()
          : await _statusHook.setOffline();

      if (!mounted) return;
      if (result['success']) {
        setState(() {
          _isOnline = targetStatus;
        });
      } else {
        final message = result['message'] as String;
        final code = _parseErrorCode(message);
        showStatusNotification(context, message: message, code: code);
      }
    } catch (_) {
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
    if (!mounted ||
        !_isOnline ||
        _isOrderActive ||
        _isIncomingOrderDialogVisible ||
        _currentTripId == null) {
      return;
    }

    _isIncomingOrderDialogVisible = true;

    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => IncomingOrderDialog(
        clientName: _currentClientName,
        rating: _currentClientRating,
        fromAddress: _fromAddress,
        toAddress: _toAddress,
        preferences: _currentPreferences,
        price: _incomingPrice,
        onAccept: () {
          _isIncomingOrderDialogVisible = false;
          Navigator.pop(context);
          _acceptOrder();
        },
        onDecline: () {
          _isIncomingOrderDialogVisible = false;
          Navigator.pop(context);
          _rejectOrder();
        },
      ),
    ).then((_) {
      _isIncomingOrderDialogVisible = false;
    });
  }

  Future<void> _acceptOrder() async {
    if (_currentTripId == null) return;

    try {
      final position = await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.high,
      );

      if (!mounted) return;

      setState(() {
        _currentPosition = LatLng(position.latitude, position.longitude);
      });

      await TripService.acceptTrip(
        _currentTripId!,
        _currentPosition!.latitude,
        _currentPosition!.longitude,
      );

      if (!mounted) return;
      setState(() {
        _isOrderActive = true;
      });

      await _buildRouteToClient();
      await _checkActiveTrip();

      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Заказ принят! Маршрут до пассажира построен'),
          duration: Duration(seconds: 3),
        ),
      );
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Ошибка принятия: $e')));
      }
    }
  }

  Future<void> _rejectOrder() async {
    if (_currentTripId == null) return;

    try {
      await TripService.rejectTrip(_currentTripId!);
      if (!mounted) return;

      setState(() {
        _currentTripId = null;
        _isOrderActive = false;
      });
      _isIncomingOrderDialogVisible = false;
    } catch (e) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('Ошибка отклонения: $e')));
    }
  }

  Future<void> _arriveAtPickup() async {
    if (_currentTripId == null) return;

    try {
      await TripService.startTrip(_currentTripId!);

      if (!mounted) return;

      final position = await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.high,
      );

      if (!mounted) return;

      setState(() {
        _currentPosition = LatLng(position.latitude, position.longitude);
        _hasArrivedAtPickup = true;
      });

      await _buildRouteToDestination();
      await _checkActiveTrip();

      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Поездка начата! Маршрут до точки назначения обновлён'),
          duration: Duration(seconds: 3),
        ),
      );
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Ошибка при начале поездки: $e'),
            duration: const Duration(seconds: 3),
          ),
        );
      }
    }
  }

  Future<void> _cancelTrip() async {
    if (_currentTripId == null) return;

    try {
      await TripService.cancelTrip(_currentTripId!);
      if (!mounted) return;

      _resetTripState();
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Поездка отменена'),
          duration: Duration(seconds: 2),
        ),
      );
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Ошибка при отмене поездки: $e'),
            duration: const Duration(seconds: 3),
          ),
        );
      }
    }
  }

  Future<void> _completeTrip() async {
    if (_currentTripId == null) return;

    try {
      await TripService.completeTrip(_currentTripId!);
      if (!mounted) return;

      _resetTripState();
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Поездка завершена!'),
          duration: Duration(seconds: 3),
        ),
      );
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Ошибка при завершении поездки: $e'),
            duration: const Duration(seconds: 3),
          ),
        );
      }
    }
  }

  Future<void> _buildRouteToDestination() async {
    if (_currentPosition == null || _destinationPosition == null) return;

    try {
      final url = Uri.parse(
        'https://router.project-osrm.org/route/v1/driving/${_currentPosition!.longitude},${_currentPosition!.latitude};${_destinationPosition!.longitude},${_destinationPosition!.latitude}?overview=full&geometries=geojson',
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
            _fitRoute(_destinationPosition);
          }
        }
      }
    } catch (e) {
      debugPrint(e.toString());
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
            _fitRoute(_clientPosition);
          }
        }
      }
    } catch (e) {
      debugPrint(e.toString());
    }
  }

  void _fitRoute(LatLng? target) {
    if (_currentPosition == null || target == null || _routePoints.isEmpty)
      return;

    final bounds = LatLngBounds.fromPoints([
      _currentPosition!,
      target,
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

  void _resetTripState() {
    setState(() {
      _isOrderActive = false;
      _hasArrivedAtPickup = false;
      _currentTripId = null;
      _activeTrip = null;
      _clientPosition = null;
      _destinationPosition = null;
      _routePoints = [];
      _currentClientName = 'Новый пассажир';
      _currentClientRating = '5.0';
      _currentPreferences = {};
      _incomingPrice = '₽ 500';
    });
    _isIncomingOrderDialogVisible = false;
  }

  Future<void> _updateLocationManually() async {
    try {
      final position = await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.high,
        timeLimit: const Duration(seconds: 10),
      );

      if (mounted) {
        setState(() {
          _currentPosition = LatLng(position.latitude, position.longitude);
        });
        _mapController.move(_currentPosition!, 15.0);
        if (_isOrderActive) {
          if (_hasArrivedAtPickup) {
            await _buildRouteToDestination();
          } else {
            await _buildRouteToClient();
          }
        }
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Геолокация обновлена'),
            duration: Duration(seconds: 2),
          ),
        );
      }
    } catch (_) {
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
      builder: (context) => NotificationsPanel(
        onClose: () => Navigator.pop(context),
        onClear: () => setState(() => _notifications.clear()),
        notifications: _notifications,
      ),
    );
  }

  @override
  void dispose() {
    _wsManager.onNotification = null;
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
                    if (_clientPosition != null && !_hasArrivedAtPickup)
                      Marker(
                        point: _clientPosition!,
                        width: 56,
                        height: 70,
                        child: const PickupMarker(),
                      ),
                    if (_destinationPosition != null && _hasArrivedAtPickup)
                      Marker(
                        point: _destinationPosition!,
                        width: 56,
                        height: 70,
                        child: const DestinationMarker(),
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
          if (_isOrderActive && !_hasArrivedAtPickup)
            Positioned(
              bottom: 30,
              left: 0,
              right: 0,
              child: SafeArea(
                child: Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: DriverActiveOrderPanel(
                    clientName: _currentClientName,
                    fromAddress: _fromAddress,
                    price: _incomingPrice,
                    onArrived: _arriveAtPickup,
                    onCancel: _cancelTrip,
                  ),
                ),
              ),
            ),
          if (_isOrderActive && _hasArrivedAtPickup)
            Positioned(
              bottom: 30,
              left: 0,
              right: 0,
              child: SafeArea(
                child: Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: DriverTripInProgressPanel(
                    clientName: _currentClientName,
                    toAddress: _toAddress,
                    price: _incomingPrice,
                    onComplete: _completeTrip,
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
            bottom: _isOrderActive ? 370 : 100,
            right: 16,
            child: SafeArea(
              child: FloatingActionButton(
                onPressed: _updateLocationManually,
                backgroundColor: const Color(0xFFFFC107),
                foregroundColor: Colors.black,
                tooltip: 'Обновить геолокацию',
                child: const Icon(Icons.my_location),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
