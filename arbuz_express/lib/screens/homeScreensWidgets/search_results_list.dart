import 'package:flutter/material.dart';
import 'package:arbuz_express/widgets/app_ui.dart';

class SearchResultsList extends StatelessWidget {
  final List<dynamic> results;
  final Function(dynamic) onSelect;

  const SearchResultsList({
    super.key,
    required this.results,
    required this.onSelect,
  });

  @override
  Widget build(BuildContext context) {
    return GlassCard(
      padding: EdgeInsets.zero,
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxHeight: 300),
        child: ListView.separated(
          shrinkWrap: false,
          itemCount: results.length,
          separatorBuilder: (_, __) =>
              const Divider(color: Colors.white10, height: 1),
          itemBuilder: (context, index) {
            final item = results[index];
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
                style: const TextStyle(color: Colors.white38, fontSize: 12),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
              onTap: () => onSelect(item),
            );
          },
        ),
      ),
    );
  }
}
