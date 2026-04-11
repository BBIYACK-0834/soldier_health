package com.teukgeupjeonsa.backend.alarm;

import com.teukgeupjeonsa.backend.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkoutAlarmRepository extends JpaRepository<WorkoutAlarm, Long> {
    List<WorkoutAlarm> findByUserOrderByHourAscMinuteAsc(User user);
}
