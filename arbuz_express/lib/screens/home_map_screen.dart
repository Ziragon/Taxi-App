// home_map_screen.dart
import 'dart:async';
import 'dart:convert';
import 'package:arbuz_express/CustomTextField/HomeMapScreen/destination_marker.dart';
import 'package:arbuz_express/CustomTextField/HomeMapScreen/pickup_marker.dart';
import 'package:arbuz_express/models/trip_models.dart';
import 'package:arbuz_express/screens/homeScreensWidgets/active_order_card.dart';
import 'package:arbuz_express/screens/homeScreensWidgets/collapsible_bottom_card.dart';
import 'package:arbuz_express/screens/homeScreensWidgets/search_results_list.dart';
import 'package:arbuz_express/screens/menuScreens/notifications_button.dart';
import 'package:arbuz_express/screens/menuScreens/notifications_panel.dart';
import 'package:arbuz_express/screens/profile_screen.dart';
import 'package:arbuz_express/services/trip_service.dart';
import 'package:arbuz_express/services/websocket_manager.dart';
import 'package:arbuz_express/widgets/app_ui.dart';
import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:geolocator/geolocator.dart';
import 'package:http/http.dart' as http;
import 'package:latlong2/latlong.dart';

class HomeMapScreen extends StatefulWidget {
  const HomeMapScreen({super.key});

  @override
  State<HomeMapScreen> createState() => _HomeMapScreenState();
}

class _HomeMapScreenState extends State<HomeMapScreen> {
  static const LatLng _initialCenter = LatLng(55.0084, 82.9357);

  final TextEditingController _fromController = TextEditingController();
  final TextEditingController _toController = TextEditingController();
  final MapController _mapController = MapController();
  final WebSocketManager _wsManager = WebSocketManager();

  LatLng? _currentPosition;
  LatLng? _toPosition;
  List<LatLng> _routePoints = [];
  List<dynamic> _searchResults = [];
  final List<Map<String, dynamic>> _notifications = [];

  bool _isSearchingFrom = true;
  bool _isCollapsed = false;
  bool _isOrderAccepted = false;
  bool _isSearchingDriver = false;
  int _selectedTariff = 0;
  Timer? _debounce;
  Timer? _tripRefreshTimer;

  Map<String, String> _orderOptions = {};
  TripCalculationResponse? _lastTripData;
  TripDetails? _activeTrip;

  @override
  void initState() {
    super.initState();
    _registerNotificationListener();
    _loadActiveTrip();
  }

  Future<void> _loadActiveTrip() async {
    try {
      final activeTrip = await TripService.getActiveTrip();
      if (!mounted) return;
      if (activeTrip != null) {
        _applyTripState(activeTrip, isFreshSearch: false);
        _startTripRefresh();
      }
    } catch (e) {
      debugPrint(e.toString());
    }
  }

  Future<void> _refreshTripDetails([int? tripId]) async {
    final id = tripId ?? _activeTrip?.id ?? _lastTripData?.id;
    if (id == null) return;

    try {
      final trip = await TripService.getTripDetails(id);
      if (!mounted) return;
      _applyTripState(trip, isFreshSearch: false);

      if (trip.status == 'COMPLETED' || trip.status == 'CANCELLED') {
        _clearOrderState();
      }
    } catch (e) {
      debugPrint(e.toString());
    }
  }

  void _applyTripState(TripDetails trip, {required bool isFreshSearch}) {
    final isSearching = trip.status == 'SEARCHING' || isFreshSearch;
    final routePoints = trip.routeGeometry == null
        ? <LatLng>[]
        : trip.routeGeometry!.coordinates
              .map((point) => LatLng(point.latitude, point.longitude))
              .toList();

    setState(() {
      _activeTrip = trip;
      _isOrderAccepted = true;
      _isSearchingDriver = isSearching;
      _orderOptions = {
        'driverName': isSearching ? 'Идёт поиск' : 'Водитель назначен',
        'carModel': trip.status,
        'carNumber': isSearching ? '...' : 'Активный заказ',
        'avatarUrl': '',
      };
      _fromController.text = trip.originAddress;
      _toController.text = trip.destAddress;
      _currentPosition = LatLng(trip.originLat, trip.originLng);
      _toPosition = LatLng(trip.destLat, trip.destLng);
      if (routePoints.isNotEmpty) {
        _routePoints = routePoints;
      }
    });

    if (routePoints.isEmpty) {
      _updateRoute();
    } else {
      _fitRoute(routePoints);
    }
  }

  void _fitRoute(List<LatLng> points) {
    if (_currentPosition == null || _toPosition == null || points.isEmpty)
      return;

    final bounds = LatLngBounds.fromPoints([
      _currentPosition!,
      _toPosition!,
      ...points,
    ]);
    _mapController.fitCamera(
      CameraFit.bounds(
        bounds: bounds,
        padding: const EdgeInsets.all(80),
        maxZoom: 15.0,
      ),
    );
  }

  void _clearOrderState() {
    _tripRefreshTimer?.cancel();
    if (!mounted) return;

    setState(() {
      _isOrderAccepted = false;
      _isSearchingDriver = false;
      _orderOptions = {};
      _activeTrip = null;
      _lastTripData = null;
      _routePoints = [];
    });
  }

  void _startTripRefresh() {
    _tripRefreshTimer?.cancel();
    _tripRefreshTimer = Timer.periodic(const Duration(seconds: 5), (_) {
      _refreshTripDetails();
    });
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

      final position = await Geolocator.getCurrentPosition();
      if (!mounted) return;

      setState(() {
        _currentPosition = LatLng(position.latitude, position.longitude);
        _fromController.text = 'Моё местоположение';
      });
      _mapController.move(_currentPosition!, 15.0);
      await _updateRoute();
    } catch (e) {
      debugPrint(e.toString());
    }
  }

  void _scheduleSearch(String query, bool isFrom) {
    if (_debounce?.isActive ?? false) {
      _debounce!.cancel();
    }
    _debounce = Timer(const Duration(milliseconds: 350), () {
      _getSuggestions(query, isFrom);
    });
  }

  Future<void> _getSuggestions(String query, bool isFrom) async {
    if (query.isEmpty) {
      setState(() => _searchResults = []);
      return;
    }

    try {
      final url = Uri.parse(
        'https://nominatim.openstreetmap.org/search?q=${Uri.encodeComponent(query)}, Новосибирск&format=json&limit=5&addressdetails=1&countrycodes=ru',
      );
      final response = await http.get(
        url,
        headers: {'User-Agent': 'ArbuzExpressApp'},
      );

      if (response.statusCode == 200 && mounted) {
        final data = json.decode(response.body);
        setState(() {
          _searchResults = data;
          _isSearchingFrom = isFrom;
        });
      }
    } catch (e) {
      debugPrint(e.toString());
    }
  }

  void _selectSuggestion(dynamic suggestion) {
    final name = suggestion['display_name'] ?? 'Неизвестный адрес';
    final lat = double.parse(suggestion['lat']);
    final lon = double.parse(suggestion['lon']);
    final point = LatLng(lat, lon);

    setState(() {
      if (_isSearchingFrom) {
        _currentPosition = point;
        _fromController.text = name;
      } else {
        _toPosition = point;
        _toController.text = name;
      }
      _searchResults = [];
      _lastTripData = null;
    });

    _mapController.move(point, 14.5);
    FocusScope.of(context).unfocus();
    _updateRoute();
  }

  void _onMapTap(LatLng point) {
    if (_isOrderAccepted) return;

    setState(() {
      if (_isSearchingFrom) {
        _currentPosition = point;
        _fromController.text = 'Указанная точка';
      } else {
        _toPosition = point;
        _toController.text = 'Указанная точка';
      }
      _lastTripData = null;
    });
    _updateRoute();
  }

  Future<void> _updateRoute() async {
    if (_currentPosition == null || _toPosition == null) return;

    try {
      final url = Uri.parse(
        'https://router.project-osrm.org/route/v1/driving/${_currentPosition!.longitude},${_currentPosition!.latitude};${_toPosition!.longitude},${_toPosition!.latitude}?overview=full&geometries=geojson',
      );
      final response = await http.get(url);
      if (response.statusCode != 200) return;

      final data = json.decode(response.body);
      if (data['routes'] == null || data['routes'].isEmpty) return;

      final List coordinates = data['routes'][0]['geometry']['coordinates'];
      final points = coordinates
          .map<LatLng>((c) => LatLng(c[1].toDouble(), c[0].toDouble()))
          .toList();
      if (!mounted) return;

      setState(() {
        _routePoints = points;
      });
      _fitRoute(points);
    } catch (e) {
      debugPrint(e.toString());
    }
  }

  Future<void> _calculateTrip() async {
    if (_currentPosition == null || _toPosition == null) return;

    try {
      final request = TripCalculationRequest(
        originAddress: _fromController.text,
        originLat: _currentPosition!.latitude,
        originLng: _currentPosition!.longitude,
        destAddress: _toController.text,
        destLat: _toPosition!.latitude,
        destLng: _toPosition!.longitude,
      );
      final response = await TripService.calculateTrip(request);

      if (!mounted) return;
      final routePoints = response.routeGeometry == null
          ? _routePoints
          : response.routeGeometry!.coordinates
                .map((point) => LatLng(point.latitude, point.longitude))
                .toList();

      setState(() {
        _lastTripData = response;
        _selectedTariff = 0;
        _routePoints = routePoints;
      });

      if (routePoints.isNotEmpty) {
        _fitRoute(routePoints);
      }
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Ошибка при расчете поездки: $e'),
          duration: const Duration(seconds: 3),
        ),
      );
    }
  }

  Future<void> _cancelOrder() async {
    final tripId = _activeTrip?.id ?? _lastTripData?.id;
    if (tripId == null) return;

    try {
      await TripService.cancelTrip(tripId);
      _clearOrderState();
    } catch (e) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('Ошибка отмены: $e')));
    }
  }

  Future<void> _startTripSearch() async {
    if (_lastTripData == null || _lastTripData!.tariffs.isEmpty) return;

    try {
      final selectedClass = _lastTripData!.tariffs[_selectedTariff].tripClass;
      await TripService.startSearching(
        tripId: _lastTripData!.id,
        vehicleClass: selectedClass,
      );
      if (!mounted) return;

      setState(() {
        _isSearchingDriver = true;
        _isOrderAccepted = true;
        _orderOptions = {
          'driverName': 'Идёт поиск',
          'carModel': selectedClass,
          'carNumber': '...',
          'avatarUrl': '',
        };
      });

      await _refreshTripDetails(_lastTripData!.id);
      _startTripRefresh();
    } catch (e) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('Ошибка поиска: $e')));
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

  void _registerNotificationListener() {
    _wsManager.onNotification = (notification) {
      if (!mounted) return;
      _addNotification(notification);

      final eventType = notification['eventType']?.toString() ?? '';
      if (eventType == 'DRIVER_FOUND') {
        _refreshTripDetails();
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Водитель найден! Ожидайте прибытия'),
            duration: Duration(seconds: 3),
          ),
        );
      } else if (eventType == 'NO_DRIVER_FOUND') {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Водитель не найден. Попробуйте позже'),
            duration: Duration(seconds: 3),
          ),
        );
      } else if (eventType == 'TRIP_STARTED') {
        _refreshTripDetails();
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Поездка начата!'),
            duration: Duration(seconds: 3),
          ),
        );
      } else if (eventType == 'TRIP_COMPLETED' ||
          eventType == 'TRIP_CANCELLED') {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              eventType == 'TRIP_COMPLETED'
                  ? 'Поездка завершена!'
                  : 'Поездка была отменена',
            ),
            duration: const Duration(seconds: 3),
          ),
        );
        _clearOrderState();
      }
    };

    if (!_wsManager.isConnected) {
      _wsManager.start('passenger');
    }
  }

  @override
  void dispose() {
    _debounce?.cancel();
    _tripRefreshTimer?.cancel();
    _wsManager.onNotification = null;
    _fromController.dispose();
    _toController.dispose();
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
                onTap: (tapPosition, point) => _onMapTap(point),
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
                        width: 56,
                        height: 70,
                        child: const PickupMarker(),
                      ),
                    if (_toPosition != null)
                      Marker(
                        point: _toPosition!,
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
          if (_searchResults.isNotEmpty && !_isOrderAccepted)
            Positioned(
              top: 100,
              left: 16,
              right: 16,
              child: SafeArea(
                child: SearchResultsList(
                  results: _searchResults,
                  onSelect: _selectSuggestion,
                ),
              ),
            ),
          if (!_isOrderAccepted &&
              _currentPosition != null &&
              _toPosition != null)
            Positioned(
              bottom: 30,
              left: 0,
              right: 0,
              child: SafeArea(
                child: CollapsibleBottomCard(
                  isCollapsed: _isCollapsed,
                  onToggle: () => setState(() => _isCollapsed = !_isCollapsed),
                  fromController: _fromController,
                  toController: _toController,
                  fromHint: 'Откуда',
                  toHint: 'Куда',
                  fromIcon: Icons.my_location_rounded,
                  toIcon: Icons.location_on_rounded,
                  onGetCurrentLocation: _getCurrentLocation,
                  onFromChanged: (value) => _scheduleSearch(value, true),
                  onToChanged: (value) => _scheduleSearch(value, false),
                  showTariffs: _lastTripData != null,
                  selectedTariff: _selectedTariff,
                  onTariffSelected: (index) =>
                      setState(() => _selectedTariff = index),
                  onOrderPressed: _lastTripData == null
                      ? _calculateTrip
                      : _startTripSearch,
                  tariffs: _lastTripData?.tariffs,
                ),
              ),
            ),
          if (_isOrderAccepted)
            Positioned(
              bottom: 30,
              left: 0,
              right: 0,
              child: SafeArea(
                child: ActiveOrderCard(
                  driverName:
                      _orderOptions['driverName'] ?? 'Водитель назначается',
                  carModel: _orderOptions['carModel'] ?? 'Поиск автомобиля',
                  carNumber: _orderOptions['carNumber'] ?? '...',
                  waitTime: _isSearchingDriver ? 'Поиск...' : 'В пути',
                  avatarUrl: _orderOptions['avatarUrl'],
                  onCall: () {},
                  onIAmHere: _cancelOrder,
                ),
              ),
            ),
        ],
      ),
    );
  }
}
