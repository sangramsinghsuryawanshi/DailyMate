package com.dailymate.events.repository;

import com.dailymate.events.entity.LocalEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocalEventRepository extends JpaRepository<LocalEvent, String> {
}
