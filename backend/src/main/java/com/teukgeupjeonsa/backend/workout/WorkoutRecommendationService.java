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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WorkoutRecommendationService {

    private static final Set<EquipmentTag> ALWAYS_AVAILABLE = EnumSet.of(EquipmentTag.BODYWEIGHT);

    private final UserRepository userRepository;
    private final UserEquipmentRepository userEquipmentRepository;

    @Transactional(readOnly = true)
    public WorkoutDtos.WorkoutRecommendationResponse getTodayRecommendation(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        int days = normalizeWorkoutDays(user.getWorkoutDaysPerWeek());
        WorkoutLevel level = user.getWorkoutLevel() == null ? WorkoutLevel.BEGINNER : user.getWorkoutLevel();
        GoalType goal = user.getGoalType() == null ? GoalType.GENERAL_FITNESS : user.getGoalType();
        Set<EquipmentTag> availableEquipment = detectAvailableEquipment(user);

        if (goal == GoalType.FITNESS_TEST) {
            return buildFitnessTestRoutine(level);
        }

        List<RoutineTemplate> templates = getRoutineTemplates(days);
        int seed = LocalDate.now().getDayOfYear();
        RoutineTemplate template = templates.get(seed % templates.size());
        List<ExerciseCandidate> pool = getEligibleExercises(availableEquipment, template, level, goal, seed);
        int targetCount = targetExerciseCount(level, template.bodyParts());

        List<WorkoutDtos.WorkoutExercise> exercises = new ArrayList<>();
        exercises.add(warmup(template));

        List<ExerciseCandidate> selected = selectBalancedExercises(pool, template, targetCount, seed);
        for (int i = 0; i < selected.size(); i++) {
            ExerciseCandidate main = selected.get(i);
            ExerciseCandidate alt = findAlternative(pool, selected, main, i, seed);
            exercises.add(toExercise(main, goal, alt.name(), level));
        }

        if (goal == GoalType.CUT) {
            exercises.add(cardioFinisher(level));
        }
        exercises.add(cooldown(template));

        return WorkoutDtos.WorkoutRecommendationResponse.builder()
                .routineType(template.routineType())
                .todayFocus(template.focus())
                .exercises(exercises)
                .note("운동별 필수 기구 태그를 매칭한 뒤, 목표/숙련도/분할 부위/동작 패턴 균형 점수로 추천")
                .build();
    }

    private WorkoutDtos.WorkoutRecommendationResponse buildFitnessTestRoutine(WorkoutLevel level) {
        int mainSets = level == WorkoutLevel.BEGINNER ? 4 : 5;
        List<WorkoutDtos.WorkoutExercise> exercises = List.of(
                WorkoutDtos.WorkoutExercise.builder()
                        .name("3km 목표 페이스 조깅/인터벌")
                        .category("뜀걸음 · 특급전사")
                        .sets(1)
                        .reps("12분 30초 목표 페이스까지 점진 단축")
                        .durationSeconds(900)
                        .restSeconds(60)
                        .intensity("High")
                        .requiredEquipment("러닝 코스")
                        .recommendationReason("특급전사 3km 12분 30초 목표 달성을 위한 주 운동")
                        .alternative("400m 반복주 6~8회")
                        .build(),
                WorkoutDtos.WorkoutExercise.builder()
                        .name("푸시업")
                        .category("푸시업 · 특급전사")
                        .sets(mainSets)
                        .reps("목표 72개를 향해 세트당 최대반복")
                        .durationSeconds(60)
                        .restSeconds(60)
                        .intensity("High")
                        .requiredEquipment("없음")
                        .recommendationReason("특급전사 팔굽혀펴기 목표 72개를 위한 특이성 훈련")
                        .alternative("무릎 푸시업 또는 템포 푸시업")
                        .build(),
                WorkoutDtos.WorkoutExercise.builder()
                        .name("윗몸일으키기")
                        .category("윗몸 · 특급전사")
                        .sets(mainSets)
                        .reps("목표 86개를 향해 세트당 최대반복")
                        .durationSeconds(60)
                        .restSeconds(60)
                        .intensity("High")
                        .requiredEquipment("매트")
                        .recommendationReason("특급전사 윗몸일으키기 목표 86개를 위한 특이성 훈련")
                        .alternative("크런치 또는 발 고정 윗몸일으키기")
                        .build()
        );

        return WorkoutDtos.WorkoutRecommendationResponse.builder()
                .routineType("특급전사 3종 집중 루틴")
                .todayFocus("윗몸 86개 · 푸시업 72개 · 3km 12분 30초")
                .exercises(exercises)
                .note("특급전사는 윗몸/푸시업/뜀걸음 3종만 메인으로 구성")
                .build();
    }

    private WorkoutDtos.WorkoutExercise cardioFinisher(WorkoutLevel level) {
        return WorkoutDtos.WorkoutExercise.builder()
                .name("감량 유산소 피니셔")
                .category("유산소 · 다이어트")
                .sets(1)
                .reps(level == WorkoutLevel.BEGINNER ? "15분 빠른 걷기" : "20분 인터벌 러닝")
                .durationSeconds(level == WorkoutLevel.BEGINNER ? 900 : 1200)
                .restSeconds(0)
                .intensity(level == WorkoutLevel.BEGINNER ? "Medium" : "High")
                .requiredEquipment("러닝 코스 또는 트레드밀")
                .recommendationReason("벌크업과 동일한 근력 분할을 유지하되 다이어트 목표에서만 유산소를 추가")
                .alternative("실내 자전거 또는 버피 저강도 변형")
                .build();
    }

    private Set<EquipmentTag> detectAvailableEquipment(User user) {
        Set<EquipmentTag> available = EnumSet.copyOf(ALWAYS_AVAILABLE);
        userEquipmentRepository.findByUser(user).stream()
                .map(UserEquipment::getEquipment)
                .filter(equipment -> equipment != null && equipment.getName() != null)
                .map(equipment -> equipment.getName().toLowerCase())
                .forEach(name -> available.addAll(tagsForEquipmentName(name)));
        return available;
    }

    private Set<EquipmentTag> tagsForEquipmentName(String name) {
        Set<EquipmentTag> tags = EnumSet.noneOf(EquipmentTag.class);
        if (containsAny(name, "철봉", "풀업", "턱걸")) tags.add(EquipmentTag.PULLUP_BAR);
        if (containsAny(name, "평행봉", "딥스")) tags.add(EquipmentTag.PARALLEL_BAR);
        if (containsAny(name, "벤치")) tags.add(EquipmentTag.BENCH);
        if (containsAny(name, "덤벨")) tags.add(EquipmentTag.DUMBBELL);
        if (containsAny(name, "바벨", "원판", "플레이트")) tags.add(EquipmentTag.BARBELL);
        if (containsAny(name, "ez바", "ez 바")) tags.add(EquipmentTag.EZ_BAR);
        if (containsAny(name, "스미스")) tags.add(EquipmentTag.SMITH_MACHINE);
        if (containsAny(name, "케이블", "크로스오버")) tags.add(EquipmentTag.CABLE);
        if (containsAny(name, "랫풀다운")) tags.add(EquipmentTag.LAT_PULLDOWN);
        if (containsAny(name, "시티드 로우", "로우 머신")) tags.add(EquipmentTag.ROW_MACHINE);
        if (containsAny(name, "티바", "t바")) tags.add(EquipmentTag.T_BAR_ROW);
        if (containsAny(name, "백 익스텐션")) tags.add(EquipmentTag.BACK_EXTENSION);
        if (containsAny(name, "체스트 프레스")) tags.add(EquipmentTag.CHEST_PRESS);
        if (containsAny(name, "펙덱", "팩덱")) tags.add(EquipmentTag.PEC_DECK);
        if (containsAny(name, "숄더 프레스")) tags.add(EquipmentTag.SHOULDER_PRESS);
        if (containsAny(name, "레터럴 레이즈 머신")) tags.add(EquipmentTag.LATERAL_RAISE_MACHINE);
        if (containsAny(name, "리어델트")) tags.add(EquipmentTag.REAR_DELT_MACHINE);
        if (containsAny(name, "레그프레스", "레그 프레스")) tags.add(EquipmentTag.LEG_PRESS);
        if (containsAny(name, "핵스쿼트", "핵 스쿼트")) tags.add(EquipmentTag.HACK_SQUAT);
        if (containsAny(name, "레그 익스텐션")) tags.add(EquipmentTag.LEG_EXTENSION);
        if (containsAny(name, "레그 컬")) tags.add(EquipmentTag.LEG_CURL);
        if (containsAny(name, "힙 어브덕션", "힙 어덕션")) tags.add(EquipmentTag.HIP_MACHINE);
        if (containsAny(name, "카프 레이즈")) tags.add(EquipmentTag.CALF_MACHINE);
        if (containsAny(name, "케틀벨")) tags.add(EquipmentTag.KETTLEBELL);
        if (containsAny(name, "밴드")) tags.add(EquipmentTag.BAND);
        if (containsAny(name, "박스", "스텝")) tags.add(EquipmentTag.BOX);
        if (containsAny(name, "trx")) tags.add(EquipmentTag.TRX);
        if (containsAny(name, "링")) tags.add(EquipmentTag.RINGS);
        if (containsAny(name, "메디신볼", "슬램볼")) tags.add(EquipmentTag.MEDICINE_BALL);
        if (containsAny(name, "프리처")) tags.add(EquipmentTag.PREACHER_CURL);
        if (containsAny(name, "암 컬", "트라이셉스")) tags.add(EquipmentTag.ARM_MACHINE);
        return tags;
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private int normalizeWorkoutDays(Integer days) {
        if (days == null) {
            return 3;
        }
        return Math.max(1, Math.min(days, 6));
    }

    private int targetExerciseCount(WorkoutLevel level, List<BodyPart> bodyParts) {
        boolean fullBody = bodyParts.size() > 2;
        if (fullBody) {
            return level == WorkoutLevel.BEGINNER ? 5 : level == WorkoutLevel.NOVICE ? 6 : 7;
        }
        return level == WorkoutLevel.BEGINNER ? 6 : level == WorkoutLevel.NOVICE ? 7 : 8;
    }

    private List<RoutineTemplate> getRoutineTemplates(int days) {
        if (days <= 3) {
            return List.of(
                    new RoutineTemplate("주 3회 전신 루틴", "전신 A · 하체+가슴+등", List.of(BodyPart.LEGS, BodyPart.CHEST, BodyPart.BACK), List.of(MovementPattern.SQUAT, MovementPattern.HORIZONTAL_PUSH, MovementPattern.VERTICAL_PULL, MovementPattern.HINGE, MovementPattern.CORE)),
                    new RoutineTemplate("주 3회 전신 루틴", "전신 B · 등+어깨+팔", List.of(BodyPart.BACK, BodyPart.SHOULDERS, BodyPart.ARMS), List.of(MovementPattern.HORIZONTAL_PULL, MovementPattern.VERTICAL_PUSH, MovementPattern.BICEPS, MovementPattern.TRICEPS, MovementPattern.CORE)),
                    new RoutineTemplate("주 3회 전신 루틴", "전신 C · 하체+푸시+풀", List.of(BodyPart.LEGS, BodyPart.CHEST, BodyPart.BACK, BodyPart.SHOULDERS), List.of(MovementPattern.SQUAT, MovementPattern.HORIZONTAL_PUSH, MovementPattern.HORIZONTAL_PULL, MovementPattern.VERTICAL_PUSH, MovementPattern.CONDITIONING))
            );
        }

        if (days == 4) {
            return List.of(
                    new RoutineTemplate("주 4회 상하체 분할", "상체 Push · 가슴/어깨/삼두", List.of(BodyPart.CHEST, BodyPart.SHOULDERS, BodyPart.ARMS), List.of(MovementPattern.HORIZONTAL_PUSH, MovementPattern.VERTICAL_PUSH, MovementPattern.FLY, MovementPattern.LATERAL_RAISE, MovementPattern.TRICEPS)),
                    new RoutineTemplate("주 4회 상하체 분할", "하체 Quad · 스쿼트/레그프레스", List.of(BodyPart.LEGS), List.of(MovementPattern.SQUAT, MovementPattern.LUNGE, MovementPattern.KNEE_EXTENSION, MovementPattern.CALVES, MovementPattern.CORE)),
                    new RoutineTemplate("주 4회 상하체 분할", "상체 Pull · 등/이두/후면어깨", List.of(BodyPart.BACK, BodyPart.SHOULDERS, BodyPart.ARMS), List.of(MovementPattern.VERTICAL_PULL, MovementPattern.HORIZONTAL_PULL, MovementPattern.REAR_DELT, MovementPattern.BICEPS, MovementPattern.CORE)),
                    new RoutineTemplate("주 4회 상하체 분할", "하체 Posterior · 둔근/햄스트링", List.of(BodyPart.LEGS), List.of(MovementPattern.HINGE, MovementPattern.HIP_THRUST, MovementPattern.HAMSTRING_CURL, MovementPattern.LUNGE, MovementPattern.CALVES))
            );
        }

        return List.of(
                new RoutineTemplate("5분할 세분화 루틴", "등 · 광배/승모/후면사슬", List.of(BodyPart.BACK), List.of(MovementPattern.VERTICAL_PULL, MovementPattern.HORIZONTAL_PULL, MovementPattern.HINGE, MovementPattern.REAR_DELT, MovementPattern.CORE)),
                new RoutineTemplate("5분할 세분화 루틴", "가슴 · 상부/중부/하부", List.of(BodyPart.CHEST), List.of(MovementPattern.HORIZONTAL_PUSH, MovementPattern.INCLINE_PUSH, MovementPattern.FLY, MovementPattern.DIP, MovementPattern.TRICEPS)),
                new RoutineTemplate("5분할 세분화 루틴", "어깨 · 전면/측면/후면", List.of(BodyPart.SHOULDERS), List.of(MovementPattern.VERTICAL_PUSH, MovementPattern.LATERAL_RAISE, MovementPattern.REAR_DELT, MovementPattern.SHRUG, MovementPattern.CORE)),
                new RoutineTemplate("5분할 세분화 루틴", "팔 · 이두/삼두/전완", List.of(BodyPart.ARMS), List.of(MovementPattern.BICEPS, MovementPattern.TRICEPS, MovementPattern.FOREARM, MovementPattern.DIP, MovementPattern.CORE)),
                new RoutineTemplate("5분할 세분화 루틴", "하체 · 대퇴/둔근/햄스트링/종아리", List.of(BodyPart.LEGS), List.of(MovementPattern.SQUAT, MovementPattern.HINGE, MovementPattern.LUNGE, MovementPattern.HAMSTRING_CURL, MovementPattern.CALVES))
        );
    }

    private List<ExerciseCandidate> getEligibleExercises(Set<EquipmentTag> availableEquipment, RoutineTemplate template, WorkoutLevel level, GoalType goal, int seed) {
        return exerciseLibrary().stream()
                .filter(candidate -> template.bodyParts().contains(candidate.bodyPart()) || candidate.bodyPart() == BodyPart.CORE)
                .filter(candidate -> template.preferredPatterns().contains(candidate.pattern()) || candidate.bodyPart() == BodyPart.CORE)
                .filter(candidate -> level.ordinal() >= candidate.minLevel().ordinal())
                .filter(candidate -> availableEquipment.containsAll(candidate.requiredEquipment()))
                .sorted(Comparator
                        .comparingInt((ExerciseCandidate candidate) -> recommendationScore(candidate, availableEquipment, template, goal))
                        .reversed()
                        .thenComparing(candidate -> Math.floorMod(candidate.name().hashCode() + seed, 1000)))
                .toList();
    }

    private int recommendationScore(ExerciseCandidate candidate, Set<EquipmentTag> availableEquipment, RoutineTemplate template, GoalType goal) {
        int score = candidate.priority();
        if (template.preferredPatterns().contains(candidate.pattern())) score += 24;
        if (goal == GoalType.BULK && candidate.loadProfile() == LoadProfile.HEAVY) score += 16;
        if ((goal == GoalType.CUT || goal == GoalType.FITNESS_TEST) && candidate.loadProfile() == LoadProfile.CONDITIONING) score += 14;
        if (goal == GoalType.MAINTAIN && candidate.loadProfile() == LoadProfile.MODERATE) score += 10;
        if (candidate.requiredEquipment().isEmpty()) score += availableEquipment.size() > 1 ? 2 : 10;
        score += candidate.requiredEquipment().size() * 3;
        return score;
    }

    private List<ExerciseCandidate> selectBalancedExercises(List<ExerciseCandidate> pool, RoutineTemplate template, int targetCount, int seed) {
        List<ExerciseCandidate> selected = new ArrayList<>();
        Set<String> selectedNames = new HashSet<>();
        Set<MovementPattern> selectedPatterns = EnumSet.noneOf(MovementPattern.class);

        for (MovementPattern pattern : template.preferredPatterns()) {
            if (selected.size() >= targetCount) break;
            pool.stream()
                    .filter(candidate -> candidate.pattern() == pattern)
                    .filter(candidate -> !selectedNames.contains(candidate.name()))
                    .findFirst()
                    .ifPresent(candidate -> {
                        selected.add(candidate);
                        selectedNames.add(candidate.name());
                        selectedPatterns.add(candidate.pattern());
                    });
        }

        for (ExerciseCandidate candidate : pool) {
            if (selected.size() >= targetCount) break;
            if (selectedNames.contains(candidate.name())) continue;
            if (selectedPatterns.contains(candidate.pattern()) && selectedPatterns.size() < template.preferredPatterns().size()) continue;
            selected.add(candidate);
            selectedNames.add(candidate.name());
            selectedPatterns.add(candidate.pattern());
        }

        if (selected.size() < targetCount) {
            List<ExerciseCandidate> fallback = exerciseLibrary().stream()
                    .filter(candidate -> candidate.requiredEquipment().isEmpty())
                    .sorted(Comparator.comparing(candidate -> Math.floorMod(candidate.name().hashCode() + seed, 1000)))
                    .toList();
            for (ExerciseCandidate candidate : fallback) {
                if (selected.size() >= targetCount) break;
                if (!selectedNames.contains(candidate.name())) {
                    selected.add(candidate);
                    selectedNames.add(candidate.name());
                }
            }
        }

        return selected;
    }

    private ExerciseCandidate findAlternative(List<ExerciseCandidate> pool, List<ExerciseCandidate> selected, ExerciseCandidate main, int index, int seed) {
        Set<String> selectedNames = selected.stream()
                .map(ExerciseCandidate::name)
                .collect(java.util.stream.Collectors.toSet());
        return pool.stream()
                .filter(candidate -> !candidate.name().equals(main.name()))
                .filter(candidate -> !selectedNames.contains(candidate.name()))
                .filter(candidate -> candidate.bodyPart() == main.bodyPart())
                .filter(candidate -> candidate.pattern() == main.pattern())
                .findFirst()
                .orElseGet(() -> pool.get(Math.floorMod(seed + index + selected.size(), pool.size())));
    }

    private WorkoutDtos.WorkoutExercise warmup(RoutineTemplate template) {
        String name = template.bodyParts().contains(BodyPart.LEGS)
                ? "고관절/흉추 가동성 + 맨몸 스쿼트 워밍업"
                : "흉추/견갑 가동성 + 밴드 풀어파트 워밍업";
        return WorkoutDtos.WorkoutExercise.builder()
                .name(name)
                .category("워밍업")
                .sets(1)
                .reps("8-10분")
                .durationSeconds(600)
                .restSeconds(20)
                .intensity("Low")
                .requiredEquipment("없음")
                .recommendationReason("주 운동 전 관절 가동 범위와 체온을 올리기 위한 공통 워밍업")
                .alternative("가벼운 걷기")
                .build();
    }

    private WorkoutDtos.WorkoutExercise cooldown(RoutineTemplate template) {
        String alternative = template.bodyParts().contains(BodyPart.LEGS)
                ? "햄스트링/둔근/종아리 스트레칭"
                : "가슴/광배/후면어깨 정적 스트레칭";
        return WorkoutDtos.WorkoutExercise.builder()
                .name("정리운동/호흡 회복")
                .category("쿨다운")
                .sets(1)
                .reps("5-8분")
                .durationSeconds(420)
                .restSeconds(0)
                .intensity("Low")
                .requiredEquipment("없음")
                .recommendationReason("심박을 낮추고 다음 운동을 위한 회복을 돕는 마무리")
                .alternative(alternative)
                .build();
    }

    private WorkoutDtos.WorkoutExercise toExercise(ExerciseCandidate candidate, GoalType goal, String alternative, WorkoutLevel level) {
        String reps = switch (goal) {
            case BULK -> candidate.loadProfile() == LoadProfile.HEAVY ? "5-8회" : "8-12회";
            case CUT, FITNESS_TEST -> candidate.loadProfile() == LoadProfile.CONDITIONING ? "30-45초" : "12-20회";
            case MAINTAIN, GENERAL_FITNESS -> "8-15회";
        };
        int sets = level == WorkoutLevel.BEGINNER ? 3 : 4;
        int restSeconds = candidate.loadProfile() == LoadProfile.HEAVY ? 90 : candidate.loadProfile() == LoadProfile.CONDITIONING ? 30 : 45;
        String intensity = level == WorkoutLevel.INTERMEDIATE && candidate.loadProfile() != LoadProfile.MOBILITY ? "High" : "Medium";

        return WorkoutDtos.WorkoutExercise.builder()
                .name(candidate.name())
                .category(candidate.category() + " · " + candidate.equipmentLabel())
                .sets(sets)
                .reps(reps)
                .durationSeconds(45)
                .restSeconds(restSeconds)
                .intensity(intensity)
                .requiredEquipment(candidate.requiredEquipmentLabel())
                .recommendationReason(candidate.reason())
                .alternative(alternative)
                .build();
    }

    private List<ExerciseCandidate> exerciseLibrary() {
        List<ExerciseCandidate> library = new ArrayList<>();

        add(library, "풀업", BodyPart.BACK, MovementPattern.VERTICAL_PULL, "등", "철봉", "철봉 보유 시 광배근 수직 당기기 주운동으로 추천", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 88, req(EquipmentTag.PULLUP_BAR));
        add(library, "밴드 어시스트 풀업", BodyPart.BACK, MovementPattern.VERTICAL_PULL, "등", "철봉+밴드", "철봉과 밴드가 있으면 풀업 볼륨을 안전하게 확보", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 84, req(EquipmentTag.PULLUP_BAR, EquipmentTag.BAND));
        add(library, "와이드그립 랫풀다운", BodyPart.BACK, MovementPattern.VERTICAL_PULL, "등", "랫풀다운 머신", "랫풀다운 머신 보유 시 풀업 대체 수직 당기기", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 90, req(EquipmentTag.LAT_PULLDOWN));
        add(library, "언더그립 랫풀다운", BodyPart.BACK, MovementPattern.VERTICAL_PULL, "등/이두", "랫풀다운 머신", "랫풀다운 머신으로 하부 광배와 이두 참여를 강화", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 82, req(EquipmentTag.LAT_PULLDOWN));
        add(library, "시티드 케이블 로우", BodyPart.BACK, MovementPattern.HORIZONTAL_PULL, "등", "시티드 로우 머신", "로우 머신 보유 시 중부등 수평 당기기 주운동", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 90, req(EquipmentTag.ROW_MACHINE));
        add(library, "바벨 벤트오버 로우", BodyPart.BACK, MovementPattern.HORIZONTAL_PULL, "등", "바벨", "바벨 보유 시 등 두께와 후면사슬 안정성 강화", WorkoutLevel.NOVICE, LoadProfile.HEAVY, 88, req(EquipmentTag.BARBELL));
        add(library, "덤벨 원암 로우", BodyPart.BACK, MovementPattern.HORIZONTAL_PULL, "등", "덤벨+벤치", "덤벨과 벤치가 있으면 좌우 비대칭 보완 로우로 추천", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 86, req(EquipmentTag.DUMBBELL, EquipmentTag.BENCH));
        add(library, "티바 로우", BodyPart.BACK, MovementPattern.HORIZONTAL_PULL, "등", "티바 로우", "티바 로우 보유 시 고중량 수평 당기기로 추천", WorkoutLevel.NOVICE, LoadProfile.HEAVY, 86, req(EquipmentTag.T_BAR_ROW));
        add(library, "스트레이트 암 풀다운", BodyPart.BACK, MovementPattern.VERTICAL_PULL, "광배", "케이블 머신", "케이블 보유 시 팔 개입을 줄인 광배 고립 운동", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 78, req(EquipmentTag.CABLE));
        add(library, "데드리프트", BodyPart.BACK, MovementPattern.HINGE, "등/후면사슬", "바벨", "바벨 보유 및 중급 이상이면 후면사슬 고중량 운동으로 추천", WorkoutLevel.INTERMEDIATE, LoadProfile.HEAVY, 80, req(EquipmentTag.BARBELL));
        add(library, "백 익스텐션", BodyPart.BACK, MovementPattern.HINGE, "척추기립근/둔근", "백 익스텐션 벤치", "백 익스텐션 벤치 보유 시 허리와 둔근 보강", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 74, req(EquipmentTag.BACK_EXTENSION));
        add(library, "밴드 로우", BodyPart.BACK, MovementPattern.HORIZONTAL_PULL, "등", "저항밴드", "밴드만 있어도 가능한 수평 당기기 대체 운동", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 70, req(EquipmentTag.BAND));
        add(library, "인버티드 로우", BodyPart.BACK, MovementPattern.HORIZONTAL_PULL, "등", "철봉", "낮은 철봉이 있으면 맨몸 등 운동으로 추천", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 72, req(EquipmentTag.PULLUP_BAR));

        add(library, "푸쉬업", BodyPart.CHEST, MovementPattern.HORIZONTAL_PUSH, "가슴", "맨몸", "기구가 없어도 가능한 기본 수평 밀기", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 76, req());
        add(library, "바벨 벤치프레스", BodyPart.CHEST, MovementPattern.HORIZONTAL_PUSH, "가슴", "바벨+벤치", "바벨과 벤치 보유 시 가슴 고중량 주운동으로 추천", WorkoutLevel.NOVICE, LoadProfile.HEAVY, 94, req(EquipmentTag.BARBELL, EquipmentTag.BENCH));
        add(library, "스미스 머신 벤치프레스", BodyPart.CHEST, MovementPattern.HORIZONTAL_PUSH, "가슴", "스미스 머신+벤치", "스미스와 벤치 보유 시 안정적인 프레스 대체", WorkoutLevel.BEGINNER, LoadProfile.HEAVY, 88, req(EquipmentTag.SMITH_MACHINE, EquipmentTag.BENCH));
        add(library, "덤벨 벤치프레스", BodyPart.CHEST, MovementPattern.HORIZONTAL_PUSH, "가슴", "덤벨+벤치", "덤벨과 벤치 보유 시 가동범위 큰 프레스", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 90, req(EquipmentTag.DUMBBELL, EquipmentTag.BENCH));
        add(library, "인클라인 덤벨 프레스", BodyPart.CHEST, MovementPattern.INCLINE_PUSH, "상부가슴", "덤벨+인클라인 벤치", "덤벨과 인클라인 벤치 보유 시 상부가슴 강화", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 88, req(EquipmentTag.DUMBBELL, EquipmentTag.BENCH));
        add(library, "인클라인 바벨 벤치프레스", BodyPart.CHEST, MovementPattern.INCLINE_PUSH, "상부가슴", "바벨+인클라인 벤치", "바벨과 인클라인 벤치 보유 시 상부가슴 고중량 운동", WorkoutLevel.NOVICE, LoadProfile.HEAVY, 86, req(EquipmentTag.BARBELL, EquipmentTag.BENCH));
        add(library, "케이블 크로스오버", BodyPart.CHEST, MovementPattern.FLY, "가슴", "케이블 머신", "케이블 보유 시 장력 유지가 좋은 가슴 플라이", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 82, req(EquipmentTag.CABLE));
        add(library, "펙덱 플라이", BodyPart.CHEST, MovementPattern.FLY, "가슴", "펙덱 머신", "펙덱 머신 보유 시 초보자도 안정적인 가슴 고립", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 84, req(EquipmentTag.PEC_DECK));
        add(library, "체스트 프레스 머신", BodyPart.CHEST, MovementPattern.HORIZONTAL_PUSH, "가슴", "체스트 프레스 머신", "체스트 프레스 머신 보유 시 안정적인 프레스", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 86, req(EquipmentTag.CHEST_PRESS));
        add(library, "딥스", BodyPart.CHEST, MovementPattern.DIP, "가슴/삼두", "평행봉", "평행봉 보유 시 하부가슴과 삼두 보강", WorkoutLevel.NOVICE, LoadProfile.MODERATE, 80, req(EquipmentTag.PARALLEL_BAR));
        add(library, "디클라인 푸쉬업", BodyPart.CHEST, MovementPattern.INCLINE_PUSH, "상부가슴", "벤치", "벤치 보유 시 맨몸 상부가슴 대체", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 72, req(EquipmentTag.BENCH));

        add(library, "덤벨 숄더프레스", BodyPart.SHOULDERS, MovementPattern.VERTICAL_PUSH, "어깨", "덤벨", "덤벨 보유 시 어깨 수직 밀기 기본 운동", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 88, req(EquipmentTag.DUMBBELL));
        add(library, "바벨 오버헤드 프레스", BodyPart.SHOULDERS, MovementPattern.VERTICAL_PUSH, "어깨", "바벨", "바벨 보유 시 전면/측면어깨 고중량 운동", WorkoutLevel.NOVICE, LoadProfile.HEAVY, 88, req(EquipmentTag.BARBELL));
        add(library, "숄더 프레스 머신", BodyPart.SHOULDERS, MovementPattern.VERTICAL_PUSH, "어깨", "숄더 프레스 머신", "머신 보유 시 안정적인 어깨 프레스", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 86, req(EquipmentTag.SHOULDER_PRESS));
        add(library, "파이크 푸쉬업", BodyPart.SHOULDERS, MovementPattern.VERTICAL_PUSH, "어깨", "맨몸", "기구가 없을 때 가능한 수직 밀기 대체", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 72, req());
        add(library, "덤벨 레터럴 레이즈", BodyPart.SHOULDERS, MovementPattern.LATERAL_RAISE, "측면어깨", "덤벨", "덤벨 보유 시 측면어깨 볼륨 확보", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 86, req(EquipmentTag.DUMBBELL));
        add(library, "케이블 레터럴 레이즈", BodyPart.SHOULDERS, MovementPattern.LATERAL_RAISE, "측면어깨", "케이블 머신", "케이블 보유 시 지속 장력으로 측면어깨 고립", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 84, req(EquipmentTag.CABLE));
        add(library, "레터럴 레이즈 머신", BodyPart.SHOULDERS, MovementPattern.LATERAL_RAISE, "측면어깨", "레터럴 레이즈 머신", "전용 머신 보유 시 자세 흔들림을 줄여 추천", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 82, req(EquipmentTag.LATERAL_RAISE_MACHINE));
        add(library, "페이스 풀", BodyPart.SHOULDERS, MovementPattern.REAR_DELT, "후면어깨/견갑", "케이블 머신", "케이블 보유 시 후면어깨와 견갑 안정화", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 84, req(EquipmentTag.CABLE));
        add(library, "덤벨 리어델트 플라이", BodyPart.SHOULDERS, MovementPattern.REAR_DELT, "후면어깨", "덤벨", "덤벨 보유 시 후면어깨 보강", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 80, req(EquipmentTag.DUMBBELL));
        add(library, "덤벨 슈러그", BodyPart.SHOULDERS, MovementPattern.SHRUG, "승모", "덤벨", "덤벨 보유 시 승모근 보강", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 74, req(EquipmentTag.DUMBBELL));
        add(library, "밴드 외회전", BodyPart.SHOULDERS, MovementPattern.REAR_DELT, "회전근개", "저항밴드", "밴드 보유 시 어깨 안정화 보조운동", WorkoutLevel.BEGINNER, LoadProfile.MOBILITY, 70, req(EquipmentTag.BAND));

        add(library, "덤벨 컬", BodyPart.ARMS, MovementPattern.BICEPS, "이두", "덤벨", "덤벨 보유 시 이두 기본 운동", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 84, req(EquipmentTag.DUMBBELL));
        add(library, "해머 컬", BodyPart.ARMS, MovementPattern.BICEPS, "상완근/전완", "덤벨", "덤벨 보유 시 상완근과 전완 보강", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 82, req(EquipmentTag.DUMBBELL));
        add(library, "바벨 컬", BodyPart.ARMS, MovementPattern.BICEPS, "이두", "바벨", "바벨 보유 시 이두 고중량 운동", WorkoutLevel.NOVICE, LoadProfile.HEAVY, 80, req(EquipmentTag.BARBELL));
        add(library, "EZ바 컬", BodyPart.ARMS, MovementPattern.BICEPS, "이두", "EZ바", "EZ바 보유 시 손목 부담을 줄인 이두 운동", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 82, req(EquipmentTag.EZ_BAR));
        add(library, "케이블 컬", BodyPart.ARMS, MovementPattern.BICEPS, "이두", "케이블 머신", "케이블 보유 시 일정 장력 이두 운동", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 80, req(EquipmentTag.CABLE));
        add(library, "프리처 컬", BodyPart.ARMS, MovementPattern.BICEPS, "이두", "프리처 컬 머신", "프리처 장비 보유 시 반동을 줄인 이두 고립", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 78, req(EquipmentTag.PREACHER_CURL));
        add(library, "케이블 푸시다운", BodyPart.ARMS, MovementPattern.TRICEPS, "삼두", "케이블 머신", "케이블 보유 시 삼두 기본 고립 운동", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 84, req(EquipmentTag.CABLE));
        add(library, "스컬 크러셔", BodyPart.ARMS, MovementPattern.TRICEPS, "삼두", "EZ바+벤치", "EZ바와 벤치 보유 시 장두 중심 삼두 운동", WorkoutLevel.NOVICE, LoadProfile.MODERATE, 80, req(EquipmentTag.EZ_BAR, EquipmentTag.BENCH));
        add(library, "오버헤드 덤벨 익스텐션", BodyPart.ARMS, MovementPattern.TRICEPS, "삼두", "덤벨", "덤벨 보유 시 삼두 장두 보강", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 78, req(EquipmentTag.DUMBBELL));
        add(library, "벤치 딥스", BodyPart.ARMS, MovementPattern.TRICEPS, "삼두", "벤치", "벤치 보유 시 맨몸 삼두 운동", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 72, req(EquipmentTag.BENCH));
        add(library, "덤벨 리스트 컬", BodyPart.ARMS, MovementPattern.FOREARM, "전완", "덤벨", "덤벨 보유 시 전완 보강", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 70, req(EquipmentTag.DUMBBELL));
        add(library, "밴드 트라이셉스 익스텐션", BodyPart.ARMS, MovementPattern.TRICEPS, "삼두", "저항밴드", "밴드 보유 시 케이블 대체 삼두 운동", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 70, req(EquipmentTag.BAND));
        add(library, "다이아몬드 푸쉬업", BodyPart.ARMS, MovementPattern.TRICEPS, "삼두/가슴", "맨몸", "기구가 없을 때 삼두 중심 맨몸 운동", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 68, req());

        add(library, "백 스쿼트", BodyPart.LEGS, MovementPattern.SQUAT, "대퇴사두/둔근", "바벨+랙", "바벨과 랙 보유 시 하체 고중량 주운동", WorkoutLevel.NOVICE, LoadProfile.HEAVY, 94, req(EquipmentTag.BARBELL));
        add(library, "스미스 머신 스쿼트", BodyPart.LEGS, MovementPattern.SQUAT, "대퇴사두/둔근", "스미스 머신", "스미스 보유 시 안정적인 스쿼트 대체", WorkoutLevel.BEGINNER, LoadProfile.HEAVY, 86, req(EquipmentTag.SMITH_MACHINE));
        add(library, "레그프레스", BodyPart.LEGS, MovementPattern.SQUAT, "대퇴사두/둔근", "레그프레스", "레그프레스 보유 시 허리 부담을 줄인 하체 주운동", WorkoutLevel.BEGINNER, LoadProfile.HEAVY, 92, req(EquipmentTag.LEG_PRESS));
        add(library, "핵스쿼트", BodyPart.LEGS, MovementPattern.SQUAT, "대퇴사두", "핵스쿼트 머신", "핵스쿼트 보유 시 대퇴사두 중심 머신 운동", WorkoutLevel.BEGINNER, LoadProfile.HEAVY, 88, req(EquipmentTag.HACK_SQUAT));
        add(library, "덤벨 고블릿 스쿼트", BodyPart.LEGS, MovementPattern.SQUAT, "하체", "덤벨", "덤벨 보유 시 초보자 친화 스쿼트 패턴", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 82, req(EquipmentTag.DUMBBELL));
        add(library, "맨몸 스쿼트", BodyPart.LEGS, MovementPattern.SQUAT, "하체", "맨몸", "기구가 없을 때 기본 하체 운동", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 70, req());
        add(library, "루마니안 데드리프트", BodyPart.LEGS, MovementPattern.HINGE, "햄스트링/둔근", "바벨", "바벨 보유 시 햄스트링과 둔근 힌지 운동", WorkoutLevel.NOVICE, LoadProfile.HEAVY, 88, req(EquipmentTag.BARBELL));
        add(library, "덤벨 루마니안 데드리프트", BodyPart.LEGS, MovementPattern.HINGE, "햄스트링/둔근", "덤벨", "덤벨 보유 시 힌지 패턴 대체", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 84, req(EquipmentTag.DUMBBELL));
        add(library, "케틀벨 스윙", BodyPart.LEGS, MovementPattern.HINGE, "둔근/후면사슬", "케틀벨", "케틀벨 보유 시 파워와 컨디셔닝을 함께 추천", WorkoutLevel.NOVICE, LoadProfile.CONDITIONING, 82, req(EquipmentTag.KETTLEBELL));
        add(library, "힙 쓰러스트", BodyPart.LEGS, MovementPattern.HIP_THRUST, "둔근", "바벨+벤치", "바벨과 벤치 보유 시 둔근 주운동", WorkoutLevel.BEGINNER, LoadProfile.HEAVY, 86, req(EquipmentTag.BARBELL, EquipmentTag.BENCH));
        add(library, "불가리안 스플릿 스쿼트", BodyPart.LEGS, MovementPattern.LUNGE, "하체/둔근", "벤치", "벤치 보유 시 좌우 하체 균형 보강", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 82, req(EquipmentTag.BENCH));
        add(library, "덤벨 워킹 런지", BodyPart.LEGS, MovementPattern.LUNGE, "하체", "덤벨", "덤벨 보유 시 런지 패턴 강도 상승", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 80, req(EquipmentTag.DUMBBELL));
        add(library, "워킹 런지", BodyPart.LEGS, MovementPattern.LUNGE, "하체", "맨몸", "기구가 없어도 가능한 한쪽 다리 패턴", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 70, req());
        add(library, "레그 익스텐션", BodyPart.LEGS, MovementPattern.KNEE_EXTENSION, "대퇴사두", "레그 익스텐션", "레그 익스텐션 보유 시 대퇴사두 고립", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 80, req(EquipmentTag.LEG_EXTENSION));
        add(library, "라잉 레그 컬", BodyPart.LEGS, MovementPattern.HAMSTRING_CURL, "햄스트링", "레그 컬", "레그 컬 보유 시 햄스트링 고립", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 80, req(EquipmentTag.LEG_CURL));
        add(library, "힙 어브덕션", BodyPart.LEGS, MovementPattern.HIP_THRUST, "중둔근", "힙 머신", "힙 어브덕션 머신 보유 시 둔근 보조운동", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 72, req(EquipmentTag.HIP_MACHINE));
        add(library, "스탠딩 카프 레이즈", BodyPart.LEGS, MovementPattern.CALVES, "종아리", "카프 머신", "카프 머신 보유 시 종아리 고립", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 78, req(EquipmentTag.CALF_MACHINE));
        add(library, "카프 레이즈", BodyPart.LEGS, MovementPattern.CALVES, "종아리", "맨몸", "기구 없이 가능한 종아리 보강", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 66, req());
        add(library, "박스 점프", BodyPart.LEGS, MovementPattern.CONDITIONING, "하체/파워", "박스", "박스 보유 시 하체 파워와 체력 강화", WorkoutLevel.NOVICE, LoadProfile.CONDITIONING, 74, req(EquipmentTag.BOX));

        add(library, "플랭크", BodyPart.CORE, MovementPattern.CORE, "코어", "맨몸", "모든 루틴에 적용 가능한 기본 코어 안정화", WorkoutLevel.BEGINNER, LoadProfile.MOBILITY, 64, req());
        add(library, "데드버그", BodyPart.CORE, MovementPattern.CORE, "코어", "맨몸", "초보자도 안전한 코어 안정화", WorkoutLevel.BEGINNER, LoadProfile.MOBILITY, 62, req());
        add(library, "행잉 니 레이즈", BodyPart.CORE, MovementPattern.CORE, "코어", "철봉", "철봉 보유 시 복근과 그립 보강", WorkoutLevel.NOVICE, LoadProfile.MODERATE, 70, req(EquipmentTag.PULLUP_BAR));
        add(library, "케이블 팔로프 프레스", BodyPart.CORE, MovementPattern.CORE, "코어", "케이블 머신", "케이블 보유 시 항회전 코어 운동", WorkoutLevel.BEGINNER, LoadProfile.MODERATE, 68, req(EquipmentTag.CABLE));
        add(library, "버피", BodyPart.CORE, MovementPattern.CONDITIONING, "전신", "맨몸", "감량/체력 목표에서 기구 없이 가능한 고강도 전신 운동", WorkoutLevel.BEGINNER, LoadProfile.CONDITIONING, 66, req());
        add(library, "마운틴 클라이머", BodyPart.CORE, MovementPattern.CONDITIONING, "코어/유산소", "맨몸", "체력 목표에서 코어와 심폐를 함께 자극", WorkoutLevel.BEGINNER, LoadProfile.CONDITIONING, 64, req());

        return library;
    }

    private void add(List<ExerciseCandidate> library, String name, BodyPart bodyPart, MovementPattern pattern,
                     String category, String equipmentLabel, String reason, WorkoutLevel minLevel,
                     LoadProfile loadProfile, int priority, Set<EquipmentTag> requiredEquipment) {
        library.add(new ExerciseCandidate(name, bodyPart, pattern, category, equipmentLabel, reason,
                minLevel, loadProfile, priority, requiredEquipment, equipmentLabel(requiredEquipment)));
    }

    private Set<EquipmentTag> req(EquipmentTag... tags) {
        if (tags.length == 0) {
            return EnumSet.noneOf(EquipmentTag.class);
        }
        return EnumSet.copyOf(List.of(tags));
    }

    private String equipmentLabel(Set<EquipmentTag> tags) {
        if (tags.isEmpty()) {
            return "없음";
        }
        return tags.stream()
                .map(EquipmentTag::label)
                .collect(java.util.stream.Collectors.joining(" + "));
    }

    private enum EquipmentTag {
        BODYWEIGHT("맨몸"), PULLUP_BAR("철봉"), PARALLEL_BAR("평행봉/딥스"), BENCH("벤치"),
        DUMBBELL("덤벨"), BARBELL("바벨/원판"), EZ_BAR("EZ바"), SMITH_MACHINE("스미스 머신"),
        CABLE("케이블 머신"), LAT_PULLDOWN("랫풀다운 머신"), ROW_MACHINE("시티드 로우 머신"), T_BAR_ROW("티바 로우"),
        BACK_EXTENSION("백 익스텐션 벤치"), CHEST_PRESS("체스트 프레스 머신"), PEC_DECK("펙덱 머신"),
        SHOULDER_PRESS("숄더 프레스 머신"), LATERAL_RAISE_MACHINE("레터럴 레이즈 머신"), REAR_DELT_MACHINE("리어델트 머신"),
        LEG_PRESS("레그프레스"), HACK_SQUAT("핵스쿼트 머신"), LEG_EXTENSION("레그 익스텐션"), LEG_CURL("레그 컬"),
        HIP_MACHINE("힙 어브덕션/어덕션"), CALF_MACHINE("카프 레이즈 머신"), KETTLEBELL("케틀벨"), BAND("저항밴드"),
        BOX("플라이오/스텝 박스"), TRX("TRX"), RINGS("짐 링"), MEDICINE_BALL("메디신볼/슬램볼"),
        PREACHER_CURL("프리처 컬 머신"), ARM_MACHINE("팔 운동 머신");

        private final String label;

        EquipmentTag(String label) {
            this.label = label;
        }

        private String label() {
            return label;
        }
    }

    private enum BodyPart { BACK, CHEST, SHOULDERS, ARMS, LEGS, CORE }

    private enum MovementPattern {
        VERTICAL_PULL, HORIZONTAL_PULL, HORIZONTAL_PUSH, INCLINE_PUSH, VERTICAL_PUSH, FLY, DIP,
        LATERAL_RAISE, REAR_DELT, SHRUG, BICEPS, TRICEPS, FOREARM, SQUAT, HINGE, HIP_THRUST,
        LUNGE, KNEE_EXTENSION, HAMSTRING_CURL, CALVES, CORE, CONDITIONING
    }

    private enum LoadProfile { HEAVY, MODERATE, CONDITIONING, MOBILITY }

    private record RoutineTemplate(String routineType, String focus, List<BodyPart> bodyParts, List<MovementPattern> preferredPatterns) {
    }

    private record ExerciseCandidate(
            String name,
            BodyPart bodyPart,
            MovementPattern pattern,
            String category,
            String equipmentLabel,
            String reason,
            WorkoutLevel minLevel,
            LoadProfile loadProfile,
            int priority,
            Set<EquipmentTag> requiredEquipment,
            String requiredEquipmentLabel
    ) {
    }
}
