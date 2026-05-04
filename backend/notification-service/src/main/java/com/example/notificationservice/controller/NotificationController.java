package com.example.notificationservice.controller;

import com.example.notificationservice.dto.NotificationPayload;
import com.example.notificationservice.entity.Notification;
import com.example.notificationservice.entity.enums.NotificationStatus;
import com.example.notificationservice.repository.NotificationRepository;
import com.example.shared.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Управление уведомлениями пользователя")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping
    @Operation(
            summary = "История уведомлений",
            description = "Все уведомления текущего пользователя, от новых к старым",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Список уведомлений")
            }
    )
    public ResponseEntity<List<NotificationPayload>> getMyNotifications(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<NotificationPayload> result = notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(principal.userId())
                .stream()
                .map(this::toPayload)
                .toList();

        return ResponseEntity.ok(result);
    }

    @GetMapping("/unread")
    @Operation(
            summary = "Непрочитанные уведомления",
            description = "Уведомления со статусом SENT — доставлены, но ещё не прочитаны",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Список непрочитанных")
            }
    )
    public ResponseEntity<List<NotificationPayload>> getUnread(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<NotificationPayload> result = notificationRepository
                .findByRecipientIdAndStatusOrderByCreatedAtDesc(
                        principal.userId(),
                        NotificationStatus.SENT
                )
                .stream()
                .map(this::toPayload)
                .toList();

        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/read")
    @Operation(
            summary = "Отметить уведомление прочитанным",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Отмечено"),
                    @ApiResponse(responseCode = "404", description = "Не найдено")
            }
    )
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        notificationRepository.findById(id).ifPresent(notification -> {
            if (notification.getRecipientId().equals(principal.userId())) {
                notificationRepository.updateStatus(id, NotificationStatus.READ);
            }
        });

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    @Operation(
            summary = "Отметить все уведомления прочитанными",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Все отмечены")
            }
    )
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        notificationRepository.markAllAsReadByRecipientId(
                principal.userId(),
                NotificationStatus.READ
        );

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unread/count")
    public ResponseEntity<Long> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        long count = notificationRepository.countUnreadByRecipientId(principal.userId());
        return ResponseEntity.ok(count);
    }

    private NotificationPayload toPayload(Notification n) {
        return new NotificationPayload(
                n.getId(),
                n.getTripId(),
                n.getEventType(),
                n.getMessage(),
                n.getCreatedAt()
        );
    }
}