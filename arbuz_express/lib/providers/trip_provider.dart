import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:arbuz_express/models/trip_models.dart';
import 'package:arbuz_express/services/trip_service.dart';
import 'package:flutter_riverpod/legacy.dart';

final tripCalculationProvider =
    FutureProvider.family<TripCalculationResponse, TripCalculationRequest>((
      ref,
      request,
    ) async {
      return await TripService.calculateTrip(request);
    });

final tripDetailsProvider = FutureProvider.family<TripDetails, int>((
  ref,
  tripId,
) async {
  return await TripService.getTripDetails(tripId);
});

final selectedTripProvider = StateProvider<TripCalculationResponse?>(
  (ref) => null,
);
