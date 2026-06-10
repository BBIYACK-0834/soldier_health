export const emptyUser = {
  id: null,
  email: '',
  nickname: '사용자',
  rank: '',
  dischargeDate: '',
  promotionDate: '',
  unitId: null,
  unitName: '',
  streakDays: 0,
  totalWeightLoss: 0,
  targetWeight: 0,
  targetDate: '',
  goalType: 'GENERAL_FITNESS',
  workoutLevel: '',
  workoutDaysPerWeek: 0,
  preferredWorkoutMinutes: 0,
};

export const emptyDashboardSummary = {
  date: '',
  intakeCalories: 0,
  targetCalories: 0,
  remainingCalories: 0,
  intakeCarbG: 0,
  targetCarbG: 0,
  intakeProteinG: 0,
  targetProteinG: 0,
  intakeFatG: 0,
  targetFatG: 0,
  weeklyExercise: { completed: 0, target: 0 },
  recommendation: '',
};

export const emptyMealDay = {
  mealDate: '',
  sourceName: '',
  breakfastRaw: '',
  breakfastKcal: 0,
  lunchRaw: '',
  lunchKcal: 0,
  dinnerRaw: '',
  dinnerKcal: 0,
  totalKcal: 0,
};

export const emptyNotificationSettings = {
  mealReminder: false,
  exerciseReminder: false,
  weightReminder: false,
  waterReminder: false,
};

export const exerciseCatalog = [
  { id: 'warmup-mobility', exerciseName: '전신 관절 가동성', category: '워밍업', durationSeconds: 45, restSeconds: 15, sets: 2, reps: '천천히', caloriesBurned: 0, intensity: 'Low' },
  { id: 'jumping-jack', exerciseName: '점핑 잭', category: '유산소', durationSeconds: 40, restSeconds: 20, sets: 3, reps: '반복', caloriesBurned: 0, intensity: 'Medium' },
  { id: 'push-up', exerciseName: '푸시업', category: '가슴 · 맨몸', durationSeconds: 35, restSeconds: 25, sets: 4, reps: '가능한 만큼', caloriesBurned: 0, intensity: 'Medium' },
  { id: 'decline-push-up', exerciseName: '디클라인 푸쉬업', category: '상부가슴 · 벤치', durationSeconds: 35, restSeconds: 25, sets: 3, reps: '10~15회', caloriesBurned: 0, intensity: 'Medium' },
  { id: 'dips', exerciseName: '딥스', category: '가슴/삼두 · 평행봉', durationSeconds: 35, restSeconds: 30, sets: 3, reps: '8~12회', caloriesBurned: 0, intensity: 'High' },
  { id: 'pull-up', exerciseName: '풀업', category: '등 · 철봉', durationSeconds: 35, restSeconds: 40, sets: 4, reps: '가능한 만큼', caloriesBurned: 0, intensity: 'High' },
  { id: 'chin-up', exerciseName: '친업', category: '등/이두 · 철봉', durationSeconds: 35, restSeconds: 40, sets: 3, reps: '가능한 만큼', caloriesBurned: 0, intensity: 'High' },
  { id: 'inverted-row', exerciseName: '인버티드 로우', category: '등 · 철봉', durationSeconds: 40, restSeconds: 30, sets: 3, reps: '8~15회', caloriesBurned: 0, intensity: 'Medium' },
  { id: 'pike-push-up', exerciseName: '파이크 푸쉬업', category: '어깨 · 맨몸', durationSeconds: 35, restSeconds: 30, sets: 3, reps: '8~12회', caloriesBurned: 0, intensity: 'Medium' },
  { id: 'lateral-raise', exerciseName: '덤벨 레터럴 레이즈', category: '측면어깨 · 덤벨', durationSeconds: 35, restSeconds: 25, sets: 4, reps: '12~20회', caloriesBurned: 0, intensity: 'Medium' },
  { id: 'rear-delt-fly', exerciseName: '덤벨 리어델트 플라이', category: '후면어깨 · 덤벨', durationSeconds: 35, restSeconds: 25, sets: 3, reps: '12~20회', caloriesBurned: 0, intensity: 'Medium' },
  { id: 'diamond-push-up', exerciseName: '다이아몬드 푸쉬업', category: '삼두 · 맨몸', durationSeconds: 35, restSeconds: 30, sets: 3, reps: '8~15회', caloriesBurned: 0, intensity: 'Medium' },
  { id: 'bench-dips', exerciseName: '벤치 딥스', category: '삼두 · 벤치', durationSeconds: 35, restSeconds: 30, sets: 3, reps: '10~15회', caloriesBurned: 0, intensity: 'Medium' },
  { id: 'dumbbell-curl', exerciseName: '덤벨 컬', category: '이두 · 덤벨', durationSeconds: 35, restSeconds: 25, sets: 3, reps: '10~15회', caloriesBurned: 0, intensity: 'Medium' },
  { id: 'hammer-curl', exerciseName: '해머 컬', category: '상완근/전완 · 덤벨', durationSeconds: 35, restSeconds: 25, sets: 3, reps: '10~15회', caloriesBurned: 0, intensity: 'Medium' },
  { id: 'air-squat', exerciseName: '맨몸 스쿼트', category: '하체 · 맨몸', durationSeconds: 40, restSeconds: 20, sets: 4, reps: '15~20회', caloriesBurned: 0, intensity: 'Medium' },
  { id: 'walking-lunge', exerciseName: '워킹 런지', category: '하체 · 맨몸', durationSeconds: 40, restSeconds: 25, sets: 3, reps: '좌우 반복', caloriesBurned: 0, intensity: 'Medium' },
  { id: 'bulgarian-split-squat', exerciseName: '불가리안 스플릿 스쿼트', category: '하체/둔근 · 벤치', durationSeconds: 40, restSeconds: 30, sets: 3, reps: '좌우 8~12회', caloriesBurned: 0, intensity: 'High' },
  { id: 'step-up', exerciseName: '스텝업', category: '하체/둔근 · 박스', durationSeconds: 40, restSeconds: 25, sets: 3, reps: '좌우 10~12회', caloriesBurned: 0, intensity: 'Medium' },
  { id: 'single-leg-bridge', exerciseName: '싱글레그 글루트 브릿지', category: '둔근/햄스트링 · 맨몸', durationSeconds: 35, restSeconds: 20, sets: 3, reps: '좌우 12~15회', caloriesBurned: 0, intensity: 'Medium' },
  { id: 'calf-raise', exerciseName: '카프 레이즈', category: '종아리 · 맨몸', durationSeconds: 35, restSeconds: 20, sets: 4, reps: '15~25회', caloriesBurned: 0, intensity: 'Medium' },
  { id: 'burpee', exerciseName: '버피', category: '전신', durationSeconds: 30, restSeconds: 30, sets: 3, reps: '정확한 자세', caloriesBurned: 0, intensity: 'High' },
  { id: 'mountain-climber', exerciseName: '마운틴 클라이머', category: '코어/유산소', durationSeconds: 35, restSeconds: 25, sets: 3, reps: '빠르게', caloriesBurned: 0, intensity: 'High' },
  { id: 'plank', exerciseName: '플랭크', category: '코어', durationSeconds: 45, restSeconds: 20, sets: 3, reps: '버티기', caloriesBurned: 0, intensity: 'Medium' },
  { id: 'side-plank', exerciseName: '사이드 플랭크', category: '코어', durationSeconds: 30, restSeconds: 20, sets: 2, reps: '좌/우', caloriesBurned: 0, intensity: 'Medium' },
  { id: 'dead-bug', exerciseName: '데드버그', category: '코어', durationSeconds: 35, restSeconds: 20, sets: 3, reps: '좌우 10~12회', caloriesBurned: 0, intensity: 'Low' },
  { id: 'bird-dog', exerciseName: '버드독', category: '코어/등', durationSeconds: 35, restSeconds: 20, sets: 3, reps: '좌우 10~12회', caloriesBurned: 0, intensity: 'Low' },
  { id: 'cooldown-breath', exerciseName: '호흡 정리운동', category: '쿨다운', durationSeconds: 60, restSeconds: 0, sets: 1, reps: '천천히', caloriesBurned: 0, intensity: 'Low' },
];

export const emptyWorkout = {
  todayFocus: '',
  routineType: '',
  exercises: [],
};
