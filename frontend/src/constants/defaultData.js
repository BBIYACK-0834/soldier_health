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
  { id: 'push-up', exerciseName: '푸시업', category: '상체', durationSeconds: 35, restSeconds: 25, sets: 4, reps: '가능한 만큼', caloriesBurned: 0, intensity: 'Medium' },
  { id: 'air-squat', exerciseName: '맨몸 스쿼트', category: '하체', durationSeconds: 40, restSeconds: 20, sets: 4, reps: '15~20회', caloriesBurned: 0, intensity: 'Medium' },
  { id: 'plank', exerciseName: '플랭크', category: '코어', durationSeconds: 45, restSeconds: 20, sets: 3, reps: '버티기', caloriesBurned: 0, intensity: 'Medium' },
  { id: 'mountain-climber', exerciseName: '마운틴 클라이머', category: '코어/유산소', durationSeconds: 35, restSeconds: 25, sets: 3, reps: '빠르게', caloriesBurned: 0, intensity: 'High' },
  { id: 'walking-lunge', exerciseName: '워킹 런지', category: '하체', durationSeconds: 40, restSeconds: 25, sets: 3, reps: '좌우 반복', caloriesBurned: 0, intensity: 'Medium' },
  { id: 'burpee', exerciseName: '버피', category: '전신', durationSeconds: 30, restSeconds: 30, sets: 3, reps: '정확한 자세', caloriesBurned: 0, intensity: 'High' },
  { id: 'side-plank', exerciseName: '사이드 플랭크', category: '코어', durationSeconds: 30, restSeconds: 20, sets: 2, reps: '좌/우', caloriesBurned: 0, intensity: 'Medium' },
  { id: 'cooldown-breath', exerciseName: '호흡 정리운동', category: '쿨다운', durationSeconds: 60, restSeconds: 0, sets: 1, reps: '천천히', caloriesBurned: 0, intensity: 'Low' },
];

export const emptyWorkout = {
  todayFocus: '',
  routineType: '',
  exercises: [],
};
