package com.eventstream.repository;

import com.eventstream.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, String> {
    List<Notification> findByEventId(String eventId);
    List<Notification> findByStatus(Notification.NotificationStatus status);
}
