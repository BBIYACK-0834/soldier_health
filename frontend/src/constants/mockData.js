export const mockUser = {
  id: 'user-1',
  email: 'soldier@army.mil',
  nickname: '이병 김다이어트',
  profileImageUrl: 'https://api.dicebear.com/9.x/thumbs/svg?seed=soldier-1',
  rank: '일병',
  enlistmentDate: '2025-05-19',
  dischargeDate: '2026-11-18',
  nextPromotionDate: '2026-02-19',
  daysUntilDischarge: 168,
  unitId: 1,
  unitName: '제1보병사단',
  streakDays: 14,
  totalWeightLoss: -1.8,
  targetWeight: 64,
  targetDate: '2024.09.01',
  goalType: 'diet',
};

export const mockDashboardSummary = {
  date: '2024.06.01',
  intakeCalories: 1520,
  targetCalories: 2000,
  remainingCalories: 480,
  intakeCarbG: 180,
  targetCarbG: 300,
  intakeProteinG: 80,
  targetProteinG: 120,
  intakeFatG: 36,
  targetFatG: 60,
  weeklyExercise: { completed: 3, target: 4 },
  recommendation: '수분 섭취는 근 손실 방지에 중요해요. 하루 2L 이상 물을 마셔보세요.',
};

export const mockMealDay = {
  mealDate: '2024-06-01',
  sourceName: '제1보병사단 병영식당',
  breakfastRaw: '쌀밥, 소고기국, 콩자반, 김치',
  breakfastKcal: 380,
  lunchRaw: '쌀밥, 닭갈비, 시금치나물, 배추김치',
  lunchKcal: 620,
  dinnerRaw: '쌀밥, 두부구이, 제육볶음, 깍두기',
  dinnerKcal: 520,
  totalKcal: 1520,
};

export const mockFoods = [
  { id: 'food-1', foodName: '바나나 1개', calories: 93, carbg: 23, proteing: 1, fatg: 0, servingUnit: '1개', isFavorite: true },
  { id: 'food-2', foodName: '삶은 계란 1개', calories: 70, carbg: 1, proteing: 6, fatg: 5, servingUnit: '1개', isFavorite: false },
  { id: 'food-3', foodName: '닭가슴살 100g', calories: 165, carbg: 0, proteing: 31, fatg: 4, servingUnit: '100g', isFavorite: true },
  { id: 'food-4', foodName: '프로틴 쉐이크 1스쿱', calories: 120, carbg: 3, proteing: 24, fatg: 2, servingUnit: '1스쿱', isFavorite: false },
  { id: 'food-5', foodName: '아몬드 10알', calories: 70, carbg: 2, proteing: 3, fatg: 6, servingUnit: '10알', isFavorite: false },
];

export const mockUnits = [
  { id: 1, unitId: 1, unitName: '제1보병사단', unitCode: 'ARMY-001', regionName: '경기', branchType: 'ARMY', dataSourceKey: 'DS_TB_MNDT_DATEBYMLSVC_1' },
  { id: 2, unitId: 2, unitName: '제2보병사단', unitCode: 'ARMY-002', regionName: '강원', branchType: 'ARMY', dataSourceKey: 'DS_TB_MNDT_DATEBYMLSVC_2' },
  { id: 3, unitId: 3, unitName: '제3보병사단', unitCode: 'ARMY-003', regionName: '강원', branchType: 'ARMY', dataSourceKey: 'DS_TB_MNDT_DATEBYMLSVC_3' },
  { id: 4, unitId: 4, unitName: '5기갑여단', unitCode: 'ARMY-005', regionName: '경기', branchType: 'ARMY', dataSourceKey: 'DS_TB_MNDT_DATEBYMLSVC_5' },
];

export const mockEquipments = [
  { id: 1, name: '러닝머신', category: 'cardio', icon: '🏃' },
  { id: 2, name: '사이클', category: 'cardio', icon: '🚴' },
  { id: 3, name: '스텝밀', category: 'cardio', icon: '🪜' },
  { id: 4, name: '로잉머신', category: 'cardio', icon: '🚣' },
  { id: 5, name: '덤벨', category: 'strength', icon: '🏋️' },
  { id: 6, name: '바벨', category: 'strength', icon: '🏋️' },
  { id: 7, name: '벤치프레스', category: 'strength', icon: '🛏️' },
  { id: 8, name: '랫풀다운', category: 'strength', icon: '💪' },
  { id: 9, name: '케이블머신', category: 'strength', icon: '🎛️' },
  { id: 10, name: '숄더프레스', category: 'strength', icon: '🏋️' },
  { id: 11, name: '스미스머신', category: 'strength', icon: '🏗️' },
  { id: 12, name: '맨몸', category: 'bodyweight', icon: '🤸' },
];

export const mockExercises = [
  { id: 'exercise-1', exerciseName: '푸시업', durationMinutes: 20, caloriesBurned: 60, sets: 3, reps: 20 },
  { id: 'exercise-2', exerciseName: '스쿼트', durationMinutes: 30, caloriesBurned: 90, sets: 3, reps: 50 },
  { id: 'exercise-3', exerciseName: '플랭크', durationMinutes: 10, caloriesBurned: 40, sets: 3, reps: 1 },
  { id: 'exercise-4', exerciseName: '러닝', durationMinutes: 20, caloriesBurned: 150 },
];

export const mockDatasets = [
  { id: 1, unitName: '제1보병사단', title: '오후 체단 128명 루틴', userCount: 128, tags: ['러닝머신 2', '스미스머신 1', '덤벨 세트'], exercises: mockExercises.slice(0, 3) },
  { id: 2, unitName: '제7기동군단', title: '보병대대 근력 루틴', userCount: 98, tags: ['케이블머신', '벤치프레스', '밀리터리프레스'], exercises: mockExercises.slice(1) },
];

export const mockPosts = [
  { id: 'post-1', authorId: 'user-2', authorNickname: '훈련병_123', unitName: '제1보병사단', title: 'PT 후 단백질 쉐이크 추천해요!', content: '가성비 좋고 맛있는 제품 공유합니다 💪', category: 'tip', likeCount: 24, commentCount: 6, createdAt: '2024.06.01' },
  { id: 'post-2', authorId: 'user-3', authorNickname: '이병_파이팅', unitName: '제1보병사단', title: '부대 식단으로 -3kg 성공!', content: '아침은 든든하게, 간식은 줄였더니 효과가 있네요.', category: 'weightLoss', likeCount: 18, commentCount: 3, createdAt: '2024.05.28' },
  { id: 'post-3', authorId: 'user-4', authorNickname: '상병_벌크업', unitName: '제2보병사단', title: '군대에서 벌크업 하는 현실적인 방법', content: 'PX 제품이랑 식단 조합 공유합니다.', category: 'exercise', likeCount: 15, commentCount: 5, createdAt: '2024.05.25' },
];

export const mockNotificationSettings = {
  mealReminder: true,
  exerciseReminder: true,
  weightReminder: false,
  waterReminder: true,
};
