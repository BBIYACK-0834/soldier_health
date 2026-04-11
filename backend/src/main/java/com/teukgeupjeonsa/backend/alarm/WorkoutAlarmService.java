package com.teukgeupjeonsa.backend.alarm;

import com.teukgeupjeonsa.backend.user.User;
import com.teukgeupjeonsa.backend.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkoutAlarmService {

    private final WorkoutAlarmRepository workoutAlarmRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AlarmDtos.AlarmResponse> getMyAlarms(Long userId) {
        User user = getUser(userId);
        return workoutAlarmRepository.findByUserOrderByHourAscMinuteAsc(user).stream().map(this::toResponse).toList();
    }

    @Transactional
    public AlarmDtos.AlarmResponse create(Long userId, AlarmDtos.AlarmSaveRequest request) {
        User user = getUser(userId);
        WorkoutAlarm alarm = WorkoutAlarm.builder()
                .user(user)
                .enabled(request.getEnabled())
                .hour(request.getHour())
                .minute(request.getMinute())
                .repeatDaysJson(request.getRepeatDaysJson())
                .label(request.getLabel())
                .build();
        return toResponse(workoutAlarmRepository.save(alarm));
    }

    @Transactional
    public AlarmDtos.AlarmResponse update(Long userId, Long id, AlarmDtos.AlarmSaveRequest request) {
        User user = getUser(userId);
        WorkoutAlarm alarm = workoutAlarmRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("알람을 찾을 수 없습니다."));
        if (!alarm.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("다른 사용자의 알람입니다.");
        }
        alarm.setEnabled(request.getEnabled());
        alarm.setHour(request.getHour());
        alarm.setMinute(request.getMinute());
        alarm.setRepeatDaysJson(request.getRepeatDaysJson());
        alarm.setLabel(request.getLabel());
        return toResponse(alarm);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        User user = getUser(userId);
        WorkoutAlarm alarm = workoutAlarmRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("알람을 찾을 수 없습니다."));
        if (!alarm.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("다른 사용자의 알람입니다.");
        }
        workoutAlarmRepository.delete(alarm);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
    }

    private AlarmDtos.AlarmResponse toResponse(WorkoutAlarm alarm) {
        return AlarmDtos.AlarmResponse.builder()
                .id(alarm.getId())
                .enabled(alarm.getEnabled())
                .hour(alarm.getHour())
                .minute(alarm.getMinute())
                .repeatDaysJson(alarm.getRepeatDaysJson())
                .label(alarm.getLabel())
                .build();
    }
}
