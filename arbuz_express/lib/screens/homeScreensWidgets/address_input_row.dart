import 'package:flutter/material.dart';

class AddressInputRow extends StatelessWidget {
  final TextEditingController controller;
  final String hint;
  final IconData icon;
  final bool isFrom;
  final VoidCallback? onLocationTap;
  final ValueChanged<String> onChanged;

  const AddressInputRow({
    super.key,
    required this.controller,
    required this.hint,
    required this.icon,
    required this.isFrom,
    this.onLocationTap,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
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
            onChanged: onChanged,
          ),
        ),
        if (isFrom && onLocationTap != null)
          IconButton(
            icon: const Icon(
              Icons.gps_fixed_rounded,
              color: Color(0xFFFFC107),
              size: 20,
            ),
            onPressed: onLocationTap,
          ),
      ],
    );
  }
}
