import 'dart:convert';
import 'package:arbuz_express/widgets/app_ui.dart';
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

  static const tariffs = [
    ('Эконом', '650 ₽', Icons.directions_car),
    ('Комфорт', '820 ₽', Icons.airport_shuttle),
    ('Бизнес', '1200 ₽', Icons.workspace_premium),
  ];

  final TextEditingController _fromController = TextEditingController();
  final TextEditingController _toController = TextEditingController();
  final MapController _mapController = MapController();

  LatLng? _currentPosition;
  LatLng? _toPosition;
  List<LatLng> _routePoints = [];
  List<dynamic> _searchResults = [];
  bool _isSearchingFrom = true;
  int _selectedTariff = 0;
  bool _showTariffs = false;
  bool _isCollapsed = false;

  Future<void> _getCurrentLocation() async {
    try {
      Position position = await Geolocator.getCurrentPosition();
      setState(() {
        _currentPosition = LatLng(position.latitude, position.longitude);
        _fromController.text = 'Моё местоположение';
      });
      _mapController.move(_currentPosition!, 15.0);
      _updateRoute();
    } catch (e) {}
  }

  Future<void> _getSuggestions(String query, bool isFrom) async {
    if (query.length < 3) {
      setState(() => _searchResults = []);
      return;
    }
    try {
      final url = Uri.parse(
        'https://nominatim.openstreetmap.org/search?q=${Uri.encodeComponent(query)}&format=json&limit=5&addressdetails=1',
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
    final name = item['display_name'].split(',')[0];

    setState(() {
      if (_isSearchingFrom) {
        _currentPosition = pos;
        _fromController.text = name;
      } else {
        _toPosition = pos;
        _toController.text = name;
        _showTariffs = true;
      }
      _searchResults = [];
    });

    _mapController.move(pos, 15.0);
    _updateRoute();
    FocusScope.of(context).unfocus();
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
        }
      }
    } catch (e) {}
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0A0A0C),
      resizeToAvoidBottomInset: false,
      body: Stack(
        children: [
          Positioned.fill(
            child: FlutterMap(
              mapController: _mapController,
              options: const MapOptions(
                initialCenter: _initialCenter,
                initialZoom: 14.5,
              ),
              children: [
                TileLayer(
                  urlTemplate:
                      'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png',
                  subdomains: const ['a', 'b', 'c', 'd'],
                ),
                if (_routePoints.isNotEmpty)
                  PolylineLayer(
                    polylines: [
                      Polyline(
                        points: _routePoints,
                        color: const Color(0xFFFFC107),
                        strokeWidth: 5.0,
                        borderColor: Colors.black26,
                        borderStrokeWidth: 1.0,
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
                        child: const _PickupMarker(),
                      ),
                    if (_toPosition != null)
                      Marker(
                        point: _toPosition!,
                        width: 40,
                        height: 40,
                        child: const Icon(
                          Icons.location_on,
                          color: Colors.red,
                          size: 40,
                        ),
                      ),
                  ],
                ),
              ],
            ),
          ),
          SafeArea(
            child: Column(
              children: [
                Padding(
                  padding: const EdgeInsets.all(16),
                  child: Row(
                    children: [
                      CircleIconButton(icon: Icons.menu_rounded, onTap: () {}),
                      const SizedBox(width: 12),
                      const Expanded(
                        child: GlassCard(
                          radius: 24,
                          padding: EdgeInsets.symmetric(
                            horizontal: 16,
                            vertical: 14,
                          ),
                          child: Row(
                            children: [
                              Icon(
                                Icons.search_rounded,
                                color: Color(0xFFFFC107),
                                size: 22,
                              ),
                              SizedBox(width: 12),
                              Text(
                                'Новосибирск',
                                style: TextStyle(
                                  color: Colors.white,
                                  fontWeight: FontWeight.w600,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                if (_searchResults.isNotEmpty)
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    child: GlassCard(
                      padding: EdgeInsets.zero,
                      child: ListView.separated(
                        shrinkWrap: true,
                        itemCount: _searchResults.length,
                        separatorBuilder: (_, __) =>
                            const Divider(color: Colors.white10, height: 1),
                        itemBuilder: (context, index) {
                          final item = _searchResults[index];
                          return ListTile(
                            title: Text(
                              item['display_name'],
                              style: const TextStyle(
                                color: Colors.white,
                                fontSize: 13,
                              ),
                              maxLines: 2,
                            ),
                            onTap: () => _selectAddress(item),
                          );
                        },
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
                                      Icons.location_on,
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
                                      _buildTariffList(),
                                    ],
                                    const SizedBox(height: 16),
                                    Row(
                                      children: [
                                        CircleIconButton(
                                          icon: Icons.layers_outlined,
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
            onChanged: (v) => _getSuggestions(v, isFrom),
          ),
        ),
        if (isFrom)
          IconButton(
            icon: const Icon(
              Icons.my_location,
              color: Color(0xFFFFC107),
              size: 20,
            ),
            onPressed: _getCurrentLocation,
          ),
      ],
    );
  }

  Widget _buildTariffList() {
    return SizedBox(
      height: 90,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        itemCount: tariffs.length,
        separatorBuilder: (_, __) => const SizedBox(width: 10),
        itemBuilder: (context, index) {
          final t = tariffs[index];
          final selected = _selectedTariff == index;
          return GestureDetector(
            onTap: () => setState(() => _selectedTariff = index),
            child: Container(
              width: 100,
              decoration: BoxDecoration(
                color: selected
                    ? const Color(0x1AFFC107)
                    : const Color(0xFF1A1A1E),
                borderRadius: BorderRadius.circular(16),
                border: Border.all(
                  color: selected
                      ? const Color(0xFFFFC107)
                      : Colors.white.withOpacity(0.05),
                ),
              ),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(
                    t.$3,
                    color: selected ? const Color(0xFFFFC107) : Colors.white60,
                    size: 22,
                  ),
                  const SizedBox(height: 4),
                  Text(
                    t.$1,
                    style: const TextStyle(color: Colors.white, fontSize: 12),
                  ),
                  Text(
                    t.$2,
                    style: const TextStyle(
                      color: Color(0xFFFFC107),
                      fontSize: 12,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }
}

class _PickupMarker extends StatelessWidget {
  const _PickupMarker();
  @override
  Widget build(BuildContext context) {
    return const Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(Icons.person_pin, color: Color(0xFFFFC107), size: 48),
        SizedBox(height: 4),
        Icon(Icons.circle, color: Color(0xFFFFC107), size: 10),
      ],
    );
  }
}
