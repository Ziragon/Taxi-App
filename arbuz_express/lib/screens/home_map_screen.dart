import 'dart:convert';
import 'dart:async';
import 'package:arbuz_express/widgets/app_ui.dart';
import 'package:arbuz_express/screens/profile_screen.dart';
import 'package:arbuz_express/screens/homeScreensWidgets/verification_banner.dart';
import 'package:arbuz_express/screens/homeScreensWidgets/search_results_list.dart';
import 'package:arbuz_express/screens/homeScreensWidgets/collapsible_bottom_card.dart';
import 'package:arbuz_express/screens/homeScreensWidgets/active_order_card.dart';
import 'package:arbuz_express/screens/menuScreens/notifications_panel.dart';
import 'package:arbuz_express/screens/menuScreens/notifications_button.dart';
import 'package:arbuz_express/CustomTextField/HomeMapScreen/pickup_marker.dart';
import 'package:arbuz_express/CustomTextField/HomeMapScreen/destination_marker.dart';
import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:geolocator/geolocator.dart';
import 'package:latlong2/latlong.dart';
import 'package:http/http.dart' as http;
import 'homeScreensWidgets/stats_bottom_sheet.dart';

class HomeMapScreen extends StatefulWidget {
  const HomeMapScreen({
    super.key,
    this.isDriver = false,
    this.showVerificationBanner = false,
  });

  final bool isDriver;
  final bool showVerificationBanner;

  @override
  State<HomeMapScreen> createState() => _HomeMapScreenState();
}

class _HomeMapScreenState extends State<HomeMapScreen> {
  static const LatLng _initialCenter = LatLng(55.0084, 82.9357);
  final TextEditingController _fromController = TextEditingController();
  final TextEditingController _toController = TextEditingController();
  final MapController _mapController = MapController();
  LatLng? _currentPosition;
  LatLng? _toPosition;
  List<LatLng> _routePoints = [];
  List<dynamic> _searchResults = [];
  bool _isSearchingFrom = true;
  int _selectedTariff = 0;
  bool _isCollapsed = false;
  Timer? _debounce;

  int _nearbyCarsCount = 0;
  String _weatherTariff = '0 ₽';
  String _distanceTariff = '0 ₽';
  double _totalTariff = 0;
  double _routeDistanceKm = 0;
  int _weatherSurchargeRaw = 0;
  int _distanceBaseRaw = 0;

  bool _isOrderAccepted = false;
  Map<String, String> _orderOptions = {};

  @override
  void initState() {
    super.initState();
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
        _fromController.text = 'Моё местоположение';
      });
      _mapController.move(_currentPosition!, 15.0);
      _updateRoute();
    } catch (e) {
      debugPrint('Error getting location: $e');
    }
  }

  void _scheduleSearch(String query, bool isFrom) {
    if (_debounce?.isActive ?? false) _debounce!.cancel();
    _debounce = Timer(const Duration(milliseconds: 350), () {
      _getSuggestions(query, isFrom);
    });
  }

  Future<void> _getSuggestions(String query, bool isFrom) async {
    if (query.length < 3) {
      setState(() => _searchResults = []);
      return;
    }
    try {
      final url = Uri.parse(
        'https://nominatim.openstreetmap.org/search?q=${Uri.encodeComponent(query)}&format=json&limit=8&addressdetails=1&countrycodes=ru&viewbox=82.5,54.6,83.4,55.4&bounded=1',
      );
      final response = await http.get(
        url,
        headers: {'User-Agent': 'ArbuzExpressApp'},
      );
      if (response.statusCode == 200) {
        setState(() {
          _searchResults = json.decode(response.body);
          _isSearchingFrom = isFrom;
        });
      }
    } catch (e) {
      debugPrint('Error getting suggestions: $e');
    }
  }

  void _selectAddress(dynamic item) {
    final lat = double.parse(item['lat']);
    final lon = double.parse(item['lon']);
    final pos = LatLng(lat, lon);
    final address = item['address'];
    final street =
        address['road'] ??
        address['residential'] ??
        address['pedestrian'] ??
        '';
    final house = address['house_number'] ?? '';
    final name = [
      street,
      house,
    ].where((e) => e.toString().isNotEmpty).join(', ');
    final finalName = name.isNotEmpty
        ? name
        : item['display_name'].split(',')[0];
    setState(() {
      if (_isSearchingFrom) {
        _currentPosition = pos;
        _fromController.text = finalName;
      } else {
        _toPosition = pos;
        _toController.text = finalName;
      }
      _searchResults = [];
    });
    _mapController.move(pos, 14.5);
    _updateRoute();
    FocusScope.of(context).unfocus();
  }

  Future<void> _setDestinationFromMap(LatLng point) async {
    setState(() {
      _toController.text = 'Определение адреса...';
      _searchResults = [];
    });
    try {
      final url = Uri.parse(
        'https://nominatim.openstreetmap.org/reverse?lat=${point.latitude}&lon=${point.longitude}&format=json&accept-language=ru',
      );
      final response = await http.get(
        url,
        headers: {'User-Agent': 'ArbuzExpressApp'},
      );
      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        final cls = data['class']?.toString() ?? '';
        final typ = data['type']?.toString() ?? '';
        final addr = data['address'] ?? {};
        final isWater =
            cls.contains('water') ||
            typ == 'water' ||
            typ == 'river' ||
            typ == 'lake' ||
            typ == 'reservoir' ||
            typ == 'bay' ||
            typ == 'ocean' ||
            addr.containsKey('water') ||
            addr.containsKey('river') ||
            addr.containsKey('lake');
        if (isWater) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('Мы не подводная лодка 🚤'),
              backgroundColor: Color(0xFFFF5722),
              duration: Duration(seconds: 2),
            ),
          );
          setState(() {
            _toController.text = '';
          });
          return;
        }
        final address = data['address'];
        if (address != null) {
          final street = address['road'] ?? address['residential'] ?? '';
          final house = address['house_number'] ?? '';
          final name = [
            street,
            house,
          ].where((e) => e.toString().isNotEmpty).join(', ');
          setState(() {
            _toPosition = point;
            _toController.text = name.isNotEmpty ? name : 'Указанная точка';
          });
        } else {
          setState(() {
            _toPosition = point;
            _toController.text = 'Указанная точка';
          });
        }
        _updateRoute();
      }
    } catch (e) {
      setState(() {
        _toController.text = '';
      });
      debugPrint('Error setting destination: $e');
    }
  }

  Future<void> _updateRoute() async {
    if (_currentPosition == null || _toPosition == null) return;
    try {
      final url = Uri.parse(
        'https://router.project-osrm.org/route/v1/driving/${_currentPosition!.longitude},${_currentPosition!.latitude};${_toPosition!.longitude},${_toPosition!.latitude}?overview=full&geometries=geojson',
      );
      final response = await http.get(url);
      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        if (data['routes'].isNotEmpty) {
          final List coordinates = data['routes'][0]['geometry']['coordinates'];
          final distanceMeters = data['routes'][0]['distance'] as double;
          setState(() {
            _routePoints = coordinates
                .map((c) => LatLng(c[1].toDouble(), c[0].toDouble()))
                .toList();
            _routeDistanceKm = distanceMeters / 1000;
          });
          try {
            final bounds = LatLngBounds.fromPoints([
              _currentPosition!,
              _toPosition!,
              ..._routePoints,
            ]);
            _mapController.fitCamera(
              CameraFit.bounds(
                bounds: bounds,
                padding: const EdgeInsets.all(40),
                maxZoom: 15.0,
              ),
            );
          } catch (e) {
            debugPrint('Error fitting camera: $e');
          }
          await _updateTariffInfo();
        }
      }
    } catch (e) {
      debugPrint('Error updating route: $e');
    }
  }

  Future<void> _updateTariffInfo() async {
    _distanceBaseRaw = (_routeDistanceKm * 30).round();
    int weatherSurcharge = 0;
    if (_currentPosition != null) {
      try {
        final weatherUrl = Uri.parse(
          'https://api.open-meteo.com/v1/forecast?latitude=${_currentPosition!.latitude}&longitude=${_currentPosition!.longitude}&current_weather=true',
        );
        final weatherResponse = await http.get(weatherUrl);
        if (weatherResponse.statusCode == 200) {
          final weatherData = json.decode(weatherResponse.body);
          final temperature = weatherData['current_weather']['temperature'];
          if (temperature < -10) {
            weatherSurcharge = 100;
          } else if (temperature < 0) {
            weatherSurcharge = 40;
          } else if (temperature > 30) {
            weatherSurcharge = 80;
          }
        }
      } catch (e) {
        debugPrint('Error getting weather: $e');
      }
    }
    _weatherSurchargeRaw = weatherSurcharge;
    final total = 50 + _distanceBaseRaw + weatherSurcharge;
    final carsCount = (20 + (_routeDistanceKm * 2).round()).clamp(5, 80);
    setState(() {
      _weatherTariff = '$weatherSurcharge ₽';
      _distanceTariff = '$_distanceBaseRaw ₽';
      _totalTariff = total.toDouble();
      _nearbyCarsCount = carsCount;
    });
  }

  void _showStatsDialogSheet() {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) => StatsBottomSheet(
        nearbyCars: _nearbyCarsCount,
        weatherTariff: _weatherTariff,
        distanceTariff: _distanceTariff,
        distanceBaseRaw: _distanceBaseRaw,
        weatherSurchargeRaw: _weatherSurchargeRaw,
        selectedTariff: _selectedTariff,
        totalTariff: _totalTariff,
        onAccept: (options) {
          setState(() {
            _isOrderAccepted = true;
            _orderOptions = options;
          });
        },
      ),
    );
  }

  void _cancelOrder() {
    setState(() {
      _isOrderAccepted = false;
      _orderOptions = {};
    });
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
    _debounce?.cancel();
    _fromController.dispose();
    _toController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final showTariffs =
        !_isOrderAccepted && _currentPosition != null && _toPosition != null;

    return Scaffold(
      backgroundColor: const Color(0xFF0A0A0C),
      resizeToAvoidBottomInset: false,
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
                onLongPress: (_, point) => _setDestinationFromMap(point),
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
                        width: 56,
                        height: 70,
                        child: const PickupMarker(),
                      ),
                    if (_toPosition != null)
                      Marker(
                        point: _toPosition!,
                        width: 44,
                        height: 44,
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
              top: 76,
              left: 0,
              right: 0,
              child: SafeArea(child: VerificationBanner()),
            ),
          SafeArea(
            child: Column(
              children: [
                const SizedBox(height: 16),
                if (_searchResults.isNotEmpty && !_isOrderAccepted)
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    child: SearchResultsList(
                      results: _searchResults,
                      onSelect: _selectAddress,
                    ),
                  ),
                const Spacer(),
                const SizedBox(height: 12),
                Padding(
                  padding: EdgeInsets.only(
                    bottom: MediaQuery.of(context).viewInsets.bottom + 16,
                    left: 16,
                    right: 16,
                  ),
                  child: _isOrderAccepted
                      ? ActiveOrderCard(
                          driverName: _orderOptions['driverName'] ?? 'Никита',
                          carModel:
                              _orderOptions['carModel'] ?? 'Hyundai Solaris',
                          carNumber: _orderOptions['carNumber'] ?? 'А123ВС',
                          waitTime: '5-7 мин',
                          avatarUrl: _orderOptions['avatarUrl'],
                          onCall: () {
                            ScaffoldMessenger.of(context).showSnackBar(
                              const SnackBar(
                                content: Text('Звонок водителю...'),
                              ),
                            );
                          },
                          onIAmHere: _cancelOrder,
                        )
                      : CollapsibleBottomCard(
                          isCollapsed: _isCollapsed,
                          onToggle: () =>
                              setState(() => _isCollapsed = !_isCollapsed),
                          fromController: _fromController,
                          toController: _toController,
                          fromHint: 'Откуда',
                          toHint: 'Куда едем?',
                          fromIcon: Icons.my_location_rounded,
                          toIcon: Icons.location_on_outlined,
                          onGetCurrentLocation: _getCurrentLocation,
                          onFromChanged: (v) {
                            final trimmed = v.trim();
                            if (trimmed.isEmpty) {
                              if (_debounce?.isActive ?? false)
                                _debounce!.cancel();
                              setState(() {
                                _currentPosition = null;
                                _routePoints = [];
                                _searchResults = [];
                              });
                            } else {
                              setState(() {});
                              _scheduleSearch(v, true);
                            }
                          },
                          onToChanged: (v) {
                            final trimmed = v.trim();
                            if (trimmed.isEmpty) {
                              if (_debounce?.isActive ?? false)
                                _debounce!.cancel();
                              setState(() {
                                _toPosition = null;
                                _routePoints = [];
                                _searchResults = [];
                              });
                            } else {
                              setState(() {});
                              _scheduleSearch(v, false);
                            }
                          },
                          showTariffs: showTariffs,
                          selectedTariff: _selectedTariff,
                          onTariffSelected: (index) =>
                              setState(() => _selectedTariff = index),
                          onOrderPressed: showTariffs
                              ? _showStatsDialogSheet
                              : null,
                        ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
