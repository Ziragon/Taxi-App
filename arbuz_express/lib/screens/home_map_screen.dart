import 'dart:convert';
import 'dart:async';
import 'package:arbuz_express/widgets/app_ui.dart';
import 'package:arbuz_express/CustomTextField/HomeMapScreen/pickup_marker.dart';
import 'package:arbuz_express/CustomTextField/HomeMapScreen/destination_marker.dart';
import 'package:arbuz_express/CustomTextField/HomeMapScreen/tariff_selector.dart';
import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:geolocator/geolocator.dart';
import 'package:latlong2/latlong.dart';
import 'package:http/http.dart' as http;

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
  final GlobalKey<ScaffoldState> _scaffoldKey = GlobalKey<ScaffoldState>();
  LatLng? _currentPosition;
  LatLng? _toPosition;
  List<LatLng> _routePoints = [];
  List<dynamic> _searchResults = [];
  bool _isSearchingFrom = true;
  int _selectedTariff = 0;
  bool _showTariffs = false;
  bool _isCollapsed = false;
  Timer? _debounce;

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
    } catch (e) {}
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
    } catch (e) {}
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
        _showTariffs = true;
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
      _showTariffs = false;
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
            _showTariffs = true;
          });
        } else {
          setState(() {
            _toPosition = point;
            _toController.text = 'Указанная точка';
            _showTariffs = true;
          });
        }
        _updateRoute();
      }
    } catch (e) {
      setState(() {
        _toController.text = '';
      });
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
          setState(() {
            _routePoints = coordinates
                .map((c) => LatLng(c[1].toDouble(), c[0].toDouble()))
                .toList();
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
          } catch (e) {}
        }
      }
    } catch (e) {}
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
    return Scaffold(
      key: _scaffoldKey,
      backgroundColor: const Color(0xFF0A0A0C),
      resizeToAvoidBottomInset: false,
      drawer: Drawer(
        backgroundColor: const Color(0xFF0A0A0C),
        child: ListView(
          padding: EdgeInsets.zero,
          children: [
            DrawerHeader(
              decoration: const BoxDecoration(color: Color(0xFF1A1A1E)),
              child: const Row(
                children: [
                  Icon(
                    Icons.directions_car_rounded,
                    color: Color(0xFFFFC107),
                    size: 48,
                  ),
                  SizedBox(width: 16),
                  Text(
                    'Arbuz Express',
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 26,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ],
              ),
            ),
            ListTile(
              leading: const Icon(Icons.home_rounded, color: Colors.white70),
              title: const Text(
                'Главная',
                style: TextStyle(color: Colors.white, fontSize: 16),
              ),
              onTap: () => Navigator.pop(context),
            ),
            ListTile(
              leading: const Icon(Icons.history_rounded, color: Colors.white70),
              title: const Text(
                'История поездок',
                style: TextStyle(color: Colors.white, fontSize: 16),
              ),
              onTap: () => Navigator.pop(context),
            ),
            ListTile(
              leading: const Icon(
                Icons.favorite_rounded,
                color: Colors.white70,
              ),
              title: const Text(
                'Избранные адреса',
                style: TextStyle(color: Colors.white, fontSize: 16),
              ),
              onTap: () => Navigator.pop(context),
            ),
            ListTile(
              leading: const Icon(
                Icons.account_balance_wallet_rounded,
                color: Colors.white70,
              ),
              title: const Text(
                'Платежи и тарифы',
                style: TextStyle(color: Colors.white, fontSize: 16),
              ),
              onTap: () => Navigator.pop(context),
            ),
            ListTile(
              leading: const Icon(Icons.person_rounded, color: Colors.white70),
              title: const Text(
                'Профиль',
                style: TextStyle(color: Colors.white, fontSize: 16),
              ),
              onTap: () => Navigator.pop(context),
            ),
            const Divider(color: Colors.white10, height: 1),
            ListTile(
              leading: const Icon(
                Icons.settings_rounded,
                color: Colors.white70,
              ),
              title: const Text(
                'Настройки',
                style: TextStyle(color: Colors.white, fontSize: 16),
              ),
              onTap: () => Navigator.pop(context),
            ),
            ListTile(
              leading: const Icon(
                Icons.headset_mic_rounded,
                color: Colors.white70,
              ),
              title: const Text(
                'Поддержка',
                style: TextStyle(color: Colors.white, fontSize: 16),
              ),
              onTap: () => Navigator.pop(context),
            ),
          ],
        ),
      ),
      body: Stack(
        children: [
          Positioned.fill(
            child: ColorFiltered(
              colorFilter: const ColorFilter.matrix([
                -1.0,
                0.0,
                0.0,
                0.0,
                255.0,
                0.0,
                -1.0,
                0.0,
                0.0,
                255.0,
                0.0,
                0.0,
                -1.0,
                0.0,
                255.0,
                0.0,
                0.0,
                0.0,
                1.0,
                0.0,
              ]),
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
                        'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
                    userAgentPackageName: 'com.arbuzexpress.app',
                    retinaMode: true,
                  ),
                  if (_routePoints.isNotEmpty)
                    PolylineLayer(
                      polylines: [
                        Polyline(
                          points: _routePoints,
                          strokeWidth: 11.0,
                          color: Colors.white.withOpacity(0.65),
                          strokeCap: StrokeCap.round,
                          strokeJoin: StrokeJoin.round,
                        ),
                        Polyline(
                          points: _routePoints,
                          strokeWidth: 6.5,
                          gradientColors: [
                            const Color(0xFF003EF8),
                            const Color(0xFF00A8DD),
                          ],
                          borderColor: const Color(0xFFFFFFFF),
                          borderStrokeWidth: 3.0,
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
          ),
          Positioned(
            top: 0,
            left: 0,
            child: SafeArea(
              child: Padding(
                padding: const EdgeInsets.only(left: 16, top: 16),
                child: CircleIconButton(
                  icon: Icons.menu_rounded,
                  onTap: () => _scaffoldKey.currentState?.openDrawer(),
                  color: const Color(0xFF1A1A1E),
                ),
              ),
            ),
          ),
          SafeArea(
            child: Column(
              children: [
                const SizedBox(height: 16),
                if (_searchResults.isNotEmpty)
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    child: GlassCard(
                      padding: EdgeInsets.zero,
                      child: ConstrainedBox(
                        constraints: const BoxConstraints(maxHeight: 300),
                        child: ListView.separated(
                          shrinkWrap: false,
                          itemCount: _searchResults.length,
                          separatorBuilder: (_, __) =>
                              const Divider(color: Colors.white10, height: 1),
                          itemBuilder: (context, index) {
                            final item = _searchResults[index];
                            final name = item['display_name'].split(',')[0];
                            final desc = item['display_name']
                                .split(',')
                                .skip(1)
                                .join(',')
                                .trim();
                            return ListTile(
                              contentPadding: const EdgeInsets.symmetric(
                                horizontal: 16,
                                vertical: 4,
                              ),
                              leading: Icon(
                                Icons.location_on_rounded,
                                color: Colors.white.withOpacity(0.5),
                              ),
                              title: Text(
                                name,
                                style: const TextStyle(
                                  color: Colors.white,
                                  fontSize: 15,
                                  fontWeight: FontWeight.w500,
                                ),
                              ),
                              subtitle: Text(
                                desc,
                                style: const TextStyle(
                                  color: Colors.white38,
                                  fontSize: 12,
                                ),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                              ),
                              onTap: () => _selectAddress(item),
                            );
                          },
                        ),
                      ),
                    ),
                  ),
                const Spacer(),
                Padding(
                  padding: EdgeInsets.only(
                    bottom: MediaQuery.of(context).viewInsets.bottom + 16,
                    left: 16,
                    right: 16,
                  ),
                  child: GlassCard(
                    padding: const EdgeInsets.fromLTRB(16, 8, 16, 16),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        GestureDetector(
                          onTap: () =>
                              setState(() => _isCollapsed = !_isCollapsed),
                          child: Container(
                            width: 40,
                            height: 4,
                            margin: const EdgeInsets.only(bottom: 12),
                            decoration: BoxDecoration(
                              color: Colors.white24,
                              borderRadius: BorderRadius.circular(2),
                            ),
                          ),
                        ),
                        AnimatedSize(
                          duration: const Duration(milliseconds: 300),
                          child: _isCollapsed
                              ? const SizedBox(
                                  width: double.infinity,
                                  height: 20,
                                  child: Center(
                                    child: Text(
                                      'Развернуть',
                                      style: TextStyle(
                                        color: Colors.white38,
                                        fontSize: 12,
                                      ),
                                    ),
                                  ),
                                )
                              : Column(
                                  children: [
                                    _buildInputRow(
                                      _fromController,
                                      'Откуда',
                                      Icons.my_location_rounded,
                                      true,
                                    ),
                                    const Divider(
                                      color: Colors.white10,
                                      height: 1,
                                    ),
                                    _buildInputRow(
                                      _toController,
                                      'Куда едем?',
                                      Icons.location_on_outlined,
                                      false,
                                    ),
                                    if (_showTariffs) ...[
                                      const SizedBox(height: 16),
                                      TariffSelector(
                                        selectedTariff: _selectedTariff,
                                        onTariffSelected: (index) => setState(
                                          () => _selectedTariff = index,
                                        ),
                                      ),
                                    ],
                                    const SizedBox(height: 16),
                                    Row(
                                      children: [
                                        CircleIconButton(
                                          icon: Icons.tune_rounded,
                                          onTap: () {},
                                          color: const Color(0xFF1A1A1E),
                                        ),
                                        const SizedBox(width: 12),
                                        Expanded(
                                          child: PrimaryButton(
                                            label: 'Заказать',
                                            onPressed: _showTariffs
                                                ? () {}
                                                : null,
                                            enabled: _showTariffs,
                                          ),
                                        ),
                                      ],
                                    ),
                                  ],
                                ),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildInputRow(
    TextEditingController controller,
    String hint,
    IconData icon,
    bool isFrom,
  ) {
    return Row(
      children: [
        Icon(
          icon,
          color: isFrom ? const Color(0xFFFFC107) : Colors.white70,
          size: 20,
        ),
        const SizedBox(width: 12),
        Expanded(
          child: TextField(
            controller: controller,
            style: const TextStyle(color: Colors.white, fontSize: 15),
            decoration: InputDecoration(
              border: InputBorder.none,
              hintText: hint,
              hintStyle: const TextStyle(color: Colors.white38),
            ),
            onChanged: (v) {
              final trimmedV = v.trim();
              if (trimmedV.isEmpty) {
                if (_debounce?.isActive ?? false) _debounce!.cancel();
                setState(() {
                  if (!isFrom) {
                    _showTariffs = false;
                  }
                  if (isFrom) {
                    _currentPosition = null;
                  } else {
                    _toPosition = null;
                  }
                  _routePoints = [];
                  _searchResults = [];
                });
              } else {
                setState(() {
                  if (!isFrom) {
                    _showTariffs = false;
                  }
                });
                _scheduleSearch(v, isFrom);
              }
            },
          ),
        ),
        if (isFrom)
          IconButton(
            icon: const Icon(
              Icons.gps_fixed_rounded,
              color: Color(0xFFFFC107),
              size: 20,
            ),
            onPressed: _getCurrentLocation,
          ),
      ],
    );
  }
}
