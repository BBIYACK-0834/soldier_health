package com.teukgeupjeonsa.backend.workout;

import com.teukgeupjeonsa.backend.equipment.UserEquipment;
import com.teukgeupjeonsa.backend.equipment.UserEquipmentRepository;
import com.teukgeupjeonsa.backend.user.GoalType;
import com.teukgeupjeonsa.backend.user.User;
import com.teukgeupjeonsa.backend.user.UserRepository;
import com.teukgeupjeonsa.backend.user.WorkoutLevel;
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
        String routineType = days <= 3 ? "주 3회 전신 루틴" : (days == 4 ? "상하체 분할 루틴" : "5분할 루틴");

        Set<String> equipmentNames = userEquipmentRepository.findByUser(user).stream()
                .map(UserEquipment::getEquipment)
                .filter(e -> e != null)
                .map(e -> e.getName().toLowerCase())
                .collect(java.util.stream.Collectors.toSet());

        DayOfWeek today = LocalDate.now().getDayOfWeek();
        WorkoutLevel level = user.getWorkoutLevel() == null ? WorkoutLevel.BEGINNER : user.getWorkoutLevel();
        String focus = switch (today) {
            case MONDAY, THURSDAY -> "하체/코어";
            case TUESDAY, FRIDAY -> "가슴/등/어깨";
            default -> "전신 컨디셔닝";
        };

        List<String> pool = getExercisePool(equipmentNames, focus);
        int targetCount = level == WorkoutLevel.BEGINNER ? 5 : level == WorkoutLevel.NOVICE ? 6 : 7;
        int seed = LocalDate.now().getDayOfYear();

        List<WorkoutDtos.WorkoutExercise> exercises = new ArrayList<>();
        exercises.add(WorkoutDtos.WorkoutExercise.builder().name("다이나믹 스트레칭").sets(1).reps("8-10분").alternative("가벼운 걷기").build());

        for (int i = 0; i < targetCount; i++) {
            String main = pool.get((seed + i) % pool.size());
            String alt = pool.get((seed + i + 5) % pool.size());
            exercises.add(ex(main, user.getGoalType(), alt));
        }

        exercises.add(WorkoutDtos.WorkoutExercise.builder().name("정리운동/호흡 회복").sets(1).reps("5-8분").alternative("폼롤러 이완").build());

        return WorkoutDtos.WorkoutRecommendationResponse.builder()
                .routineType(routineType)
                .todayFocus(focus)
                .exercises(exercises)
                .note("목표/운동수준/보유기구 기반 확장 운동 라이브러리 추천")
                .build();
    }

    private List<String> getExercisePool(Set<String> equipmentNames, String focus) {
        boolean hasBarbell = equipmentNames.stream().anyMatch(name -> name.contains("바벨"));
        boolean hasDumbbell = equipmentNames.stream().anyMatch(name -> name.contains("덤벨"));
        boolean hasKettlebell = equipmentNames.stream().anyMatch(name -> name.contains("케틀벨") || name.contains("kettlebell"));
        boolean hasBand = equipmentNames.stream().anyMatch(name -> name.contains("밴드") || name.contains("band"));
        boolean hasPullup = equipmentNames.stream().anyMatch(name -> name.contains("턱걸") || name.contains("풀업") || name.contains("철봉"));
        boolean hasBench = equipmentNames.stream().anyMatch(name -> name.contains("벤치"));

        List<String> pool = new ArrayList<>();

        if (focus.contains("하체")) {
            pool.addAll(List.of("백 스쿼트", "루마니안 데드리프트", "불가리안 스플릿 스쿼트", "워킹 런지", "힙 쓰러스트", "카프 레이즈", "플랭크", "사이드 플랭크", "데드버그", "버드독"));
        } else if (focus.contains("가슴")) {
            pool.addAll(List.of("벤치프레스", "인클라인 프레스", "푸쉬업", "덤벨 로우", "풀업", "숄더 프레스", "레터럴 레이즈", "페이스 풀", "행잉 니 레이즈", "러시안 트위스트"));
        } else {
            pool.addAll(List.of("버피", "마운틴 클라이머", "점핑 스쿼트", "스텝업", "푸쉬업", "인버티드 로우", "런지", "플랭크 잭", "슈퍼맨", "바이시클 크런치"));
        }

        if (hasBarbell) {
            pool.addAll(List.of("바벨 벤치프레스", "바벨 로우", "프론트 스쿼트", "오버헤드 프레스", "데드리프트", "바벨 런지"));
        }
        if (hasDumbbell) {
            pool.addAll(List.of("덤벨 고블릿 스쿼트", "덤벨 벤치프레스", "덤벨 숄더프레스", "덤벨 루마니안 데드리프트", "덤벨 원암 로우", "덤벨 스내치"));
        }
        if (hasKettlebell) {
            pool.addAll(List.of("케틀벨 스윙", "터키시 겟업", "케틀벨 클린앤프레스", "케틀벨 고블릿 런지"));
        }
        if (hasBand) {
            pool.addAll(List.of("밴드 풀어파트", "밴드 로우", "밴드 스쿼트", "밴드 팔로프 프레스"));
        }
        if (hasPullup) {
            pool.addAll(List.of("풀업", "친업", "행잉 레그레이즈", "철봉 네거티브"));
        }
        if (hasBench) {
            pool.addAll(List.of("벤치 딥스", "인클라인 푸쉬업", "벤치 스텝업"));
        }

        if (pool.isEmpty()) {
            pool.addAll(List.of("푸쉬업", "스쿼트", "런지", "플랭크", "버피", "마운틴 클라이머"));
        }

        return pool;
    }

    private WorkoutDtos.WorkoutExercise ex(String name, GoalType goal, String alt) {
        GoalType safeGoal = goal == null ? GoalType.GENERAL_FITNESS : goal;
        String reps = switch (safeGoal) {
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
