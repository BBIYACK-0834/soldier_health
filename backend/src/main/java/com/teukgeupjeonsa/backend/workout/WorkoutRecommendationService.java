package com.teukgeupjeonsa.backend.workout;

import com.teukgeupjeonsa.backend.equipment.UserEquipment;
import com.teukgeupjeonsa.backend.equipment.UserEquipmentRepository;
import com.teukgeupjeonsa.backend.user.GoalType;
import com.teukgeupjeonsa.backend.user.User;
import com.teukgeupjeonsa.backend.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WorkoutRecommendationService {

    private final UserRepository userRepository;
    private final UserEquipmentRepository userEquipmentRepository;

    @Transactional(readOnly = true)
    public WorkoutDtos.WorkoutRecommendationResponse getTodayRecommendation(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        int days = user.getWorkoutDaysPerWeek() == null ? 3 : user.getWorkoutDaysPerWeek();
        String routineType = days <= 3 ? "주 3회 전신 루틴" : (days == 4 ? "상하체 분할 루틴" : "분할 루틴");

        Set<String> equipmentNames = userEquipmentRepository.findByUser(user).stream()
                .map(UserEquipment::getEquipment)
                .filter(e -> e != null)
                .map(e -> e.getName().toLowerCase())
                .collect(java.util.stream.Collectors.toSet());

        DayOfWeek today = LocalDate.now().getDayOfWeek();
        String focus = (today.getValue() % 2 == 0) ? "하체/코어" : "상체/전신";

        List<WorkoutDtos.WorkoutExercise> exercises = new ArrayList<>();
        if (equipmentNames.stream().anyMatch(name -> name.contains("바벨"))) {
            exercises.add(ex("바벨 스쿼트", user.getGoalType(), "덤벨 스쿼트"));
            exercises.add(ex("바벨 벤치프레스", user.getGoalType(), "푸쉬업"));
            exercises.add(ex("바벨 로우", user.getGoalType(), "덤벨 로우"));
        } else if (equipmentNames.stream().anyMatch(name -> name.contains("덤벨"))) {
            exercises.add(ex("덤벨 스쿼트", user.getGoalType(), "맨몸 스쿼트"));
            exercises.add(ex("덤벨 벤치프레스", user.getGoalType(), "푸쉬업"));
            exercises.add(ex("덤벨 로우", user.getGoalType(), "턱걸이"));
        } else {
            exercises.add(ex("푸쉬업", user.getGoalType(), "무릎 푸쉬업"));
            exercises.add(ex("스쿼트", user.getGoalType(), "런지"));
            exercises.add(ex("플랭크", user.getGoalType(), "마운틴 클라이머"));
        }

        if (days >= 4) {
            exercises.add(WorkoutDtos.WorkoutExercise.builder().name("유산소 20분").sets(1).reps("20min").alternative("버피 5세트").build());
        }

        return WorkoutDtos.WorkoutRecommendationResponse.builder()
                .routineType(routineType)
                .todayFocus(focus)
                .exercises(exercises)
                .note("목표/주당횟수/보유기구 기반 규칙 추천")
                .build();
    }

    private WorkoutDtos.WorkoutExercise ex(String name, GoalType goal, String alt) {
        String reps = switch (goal) {
            case BULK -> "6-12회";
            case CUT, FITNESS_TEST -> "12-20회";
            case MAINTAIN, GENERAL_FITNESS -> "8-15회";
        };
        return WorkoutDtos.WorkoutExercise.builder()
                .name(name)
                .sets(4)
                .reps(reps)
                .alternative(alt)
                .build();
    }
}
