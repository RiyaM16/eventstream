package com.eventstream.repository;

import com.eventstream.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, String> {
    List<Event> findByStatus(Event.EventStatus status);
}
