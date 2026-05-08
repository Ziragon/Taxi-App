import 'dart:convert';
import 'dart:async';
import 'package:arbuz_express/widgets/app_ui.dart';
import 'package:arbuz_express/screens/profile_screen.dart';
import 'package:arbuz_express/screens/homeScreensWidgets/search_results_list.dart';
import 'package:arbuz_express/screens/homeScreensWidgets/collapsible_bottom_card.dart';
import 'package:arbuz_express/screens/homeScreensWidgets/active_order_card.dart';
import 'package:arbuz_express/screens/menuScreens/notifications_panel.dart';
import 'package:arbuz_express/screens/menuScreens/notifications_button.dart';
import 'package:arbuz_express/CustomTextField/HomeMapScreen/pickup_marker.dart';
import 'package:arbuz_express/CustomTextField/HomeMapScreen/destination_marker.dart';
import 'package:arbuz_express/models/trip_models.dart';
import 'package:arbuz_express/services/trip_service.dart';
import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:geolocator/geolocator.dart';
import 'package:latlong2/latlong.dart';
import 'package:http/http.dart' as http;
import 'homeScreensWidgets/stats_bottom_sheet.dart';

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
  LatLng? _currentPosition;
  LatLng? _toPosition;
  List<LatLng> _routePoints = [];
  List<dynamic> _searchResults = [];
  bool _isSearchingFrom = true;
  int _selectedTariff = 0;
  bool _isCollapsed = false;
  Timer? _debounce;

  bool _isOrderAccepted = false;
  Map<String, String> _orderOptions = {};
  TripCalculationResponse? _lastTripData;

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
      debugPrint(e.toString());
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
        headers: {'User-Agent': 'ArbuzExpressApp/1.0'},
      );
      if (response.statusCode == 200) {
        setState(() {
          _searchResults = json.decode(response.body);
          _isSearchingFrom = isFrom;
        });
      }
    } catch (e) {
      debugPrint(e.toString());
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
        headers: {'User-Agent': 'ArbuzExpressApp/1.0'},
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
      debugPrint(e.toString());
    }
  }

  Future<void> _updateRoute() async {
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

      final tripData = await TripService.calculateTrip(request);

      setState(() {
        _lastTripData = tripData;
      });

      _buildRouteFromResponse(tripData);
    } catch (e) {
      debugPrint(e.toString());
      _buildRouteFallback();
    }
  }

  List<LatLng> _decodePolyline(String encoded) {
    List<LatLng> points = [];
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

      points.add(LatLng(lat / 1E5, lng / 1E5));
    }
    return points;
  }

  void _buildRouteFromResponse(TripCalculationResponse tripData) {
    try {
      List<LatLng> routePoints = [];
      final dynamic geom = tripData.routeGeometry;

      if (geom == null) {
        _buildRouteFallback();
        return;
      }

      if (geom is String) {
        if (geom.startsWith('{')) {
          final decoded = jsonDecode(geom);
          if (decoded['coordinates'] != null) {
            for (var coord in decoded['coordinates']) {
              routePoints.add(LatLng(coord[1].toDouble(), coord[0].toDouble()));
            }
          }
        } else {
          routePoints = _decodePolyline(geom);
        }
      } else {
        try {
          if (geom.coordinates != null) {
            for (var p in geom.coordinates) {
              try {
                routePoints.add(
                  LatLng(p.latitude.toDouble(), p.longitude.toDouble()),
                );
              } catch (_) {
                routePoints.add(LatLng(p[1].toDouble(), p[0].toDouble()));
              }
            }
          }
        } catch (_) {}
      }

      if (routePoints.isEmpty) {
        _buildRouteFallback();
        return;
      }

      setState(() {
        _routePoints = routePoints;
      });

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
      debugPrint(e.toString());
      _buildRouteFallback();
    }
  }

  void _buildRouteFallback() {
    try {
      final bounds = LatLngBounds.fromPoints([_currentPosition!, _toPosition!]);
      _mapController.fitCamera(
        CameraFit.bounds(
          bounds: bounds,
          padding: const EdgeInsets.all(40),
          maxZoom: 15.0,
        ),
      );
    } catch (e) {
      debugPrint(e.toString());
    }
  }

  void _showStatsDialogSheet() {
    if (_currentPosition == null || _toPosition == null) return;

    final request = TripCalculationRequest(
      originAddress: _fromController.text,
      originLat: _currentPosition!.latitude,
      originLng: _currentPosition!.longitude,
      destAddress: _toController.text,
      destLat: _toPosition!.latitude,
      destLng: _toPosition!.longitude,
    );

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) => FutureBuilder<TripCalculationResponse>(
        future: TripService.calculateTrip(request),
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return Container(
              decoration: const BoxDecoration(color: Colors.transparent),
              height: 200,
              child: const Center(
                child: CircularProgressIndicator(
                  valueColor: AlwaysStoppedAnimation<Color>(Color(0xFFFFC107)),
                ),
              ),
            );
          }
          if (snapshot.hasError) {
            return Container(
              decoration: const BoxDecoration(color: Colors.transparent),
              height: 200,
              child: Center(
                child: Padding(
                  padding: const EdgeInsets.all(24),
                  child: Text(
                    'Ошибка: ${snapshot.error}',
                    style: const TextStyle(color: Colors.white),
                    textAlign: TextAlign.center,
                  ),
                ),
              ),
            );
          }
          if (!snapshot.hasData) {
            return Container(
              decoration: const BoxDecoration(color: Colors.transparent),
              height: 200,
              child: const Center(
                child: Text(
                  'Нет данных',
                  style: TextStyle(color: Colors.white),
                ),
              ),
            );
          }
          return StatsBottomSheet(
            tripData: snapshot.data!,
            selectedTariff: _selectedTariff,
            onAccept: (options) {
              setState(() {
                _isOrderAccepted = true;
                _orderOptions = options;
              });
            },
          );
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
                          onCall: () {},
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
                              if (_debounce?.isActive ?? false) {
                                _debounce!.cancel();
                              }
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
                              if (_debounce?.isActive ?? false) {
                                _debounce!.cancel();
                              }
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
                          tariffs: _lastTripData?.tariffs,
                          selectedTariff: _selectedTariff,
                          onTariffSelected: (index) =>
                              setState(() => _selectedTariff = index),
                          onOrderPressed:
                              (showTariffs &&
                                  _lastTripData != null &&
                                  _lastTripData!.tariffs.isNotEmpty)
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
