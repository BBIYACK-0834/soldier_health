import { useEffect, useMemo, useState } from 'react';
import AppLayout from '../../components/layout/AppLayout';
import Card from '../../components/ui/Card';
import ProgressBar from '../../components/ui/ProgressBar';
import MacroBox from '../../components/ui/MacroBox';
import { getTodayMeal } from '../../api/mealApi';
import { getTodayNutrition } from '../../api/nutritionApi';
import { getMyUnit } from '../../api/unitApi';
import { emptyDashboardSummary, emptyMealDay } from '../../constants/defaultData';
import styles from './DietPage.module.css';

const mealLabels = [
  { key: 'breakfastRaw', label: '아침' },
  { key: 'lunchRaw', label: '점심' },
  { key: 'dinnerRaw', label: '저녁' },
];

function parseMeal(raw) {
  if (!raw) return [];
  return raw.split(/[,/\n]/).map((s) => s.trim()).filter(Boolean);
}

function hasMealMenuData(meal) {
  if (!meal) return false;
  return Boolean(
    meal.breakfastRaw ||
    meal.lunchRaw ||
    meal.dinnerRaw ||
    (Number.isFinite(meal.breakfastKcal) && meal.breakfastKcal > 0) ||
    (Number.isFinite(meal.lunchKcal) && meal.lunchKcal > 0) ||
    (Number.isFinite(meal.dinnerKcal) && meal.dinnerKcal > 0) ||
    (Number.isFinite(meal.totalKcal) && meal.totalKcal > 0)
  );
}

function formatKcal(value) {
  if (!Number.isFinite(value)) return '칼로리 정보 없음';
  return `${value.toLocaleString()} kcal`;
}

export default function NutritionPage() {
  const [nutrition, setNutrition] = useState(emptyDashboardSummary);
  const [meal, setMeal] = useState(emptyMealDay);
  const [unit, setUnit] = useState(null);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    let mounted = true;
    async function load() {
      try {
        setLoading(true);
        const [nutritionData, mealData, unitData] = await Promise.all([
          getTodayNutrition(),
          getTodayMeal(),
          getMyUnit(),
        ]);
        if (!mounted) return;
        setNutrition(nutritionData ? { ...emptyDashboardSummary, ...nutritionData } : emptyDashboardSummary);
        setMeal(mealData ? { ...emptyMealDay, ...mealData } : emptyMealDay);
        setUnit(unitData ?? null);
      } catch (error) {
        if (!mounted) return;
        setNutrition(emptyDashboardSummary);
        setMeal(emptyMealDay);
        setUnit(null);
        setErrorMessage('식단 데이터를 불러오지 못했습니다. 선택 부대의 실제 식단이 연결되면 표시됩니다.');
      } finally {
        if (mounted) setLoading(false);
      }
    }

    load();
    return () => {
      mounted = false;
    };
  }, []);

  const menuExists = hasMealMenuData(meal);
  const mealTotalKcal = Number.isFinite(meal?.totalKcal)
    ? meal.totalKcal
    : [meal?.breakfastKcal, meal?.lunchKcal, meal?.dinnerKcal]
        .filter((value) => Number.isFinite(value))
        .reduce((sum, value) => sum + value, 0);

  const totalKcal = menuExists ? mealTotalKcal : 0;
  const targetKcal = Number.isFinite(nutrition?.targetCalories) ? nutrition.targetCalories : null;

  const macroData = useMemo(
    () => [
      { label: '탄수화물', intake: nutrition?.intakeCarbG ?? 0, target: nutrition?.targetCarbG ?? 0, color: '#50739a', tone: '#dfe5ef' },
      { label: '단백질', intake: nutrition?.intakeProteinG ?? 0, target: nutrition?.targetProteinG ?? 0, color: '#6f8f55', tone: '#e4e9de' },
      { label: '지방', intake: nutrition?.intakeFatG ?? 0, target: nutrition?.targetFatG ?? 0, color: '#d28a2c', tone: '#efe2cf' },
    ],
    [nutrition]
  );

  return (
    <AppLayout title="식단 기록" subtitle="날짜별 식단과 영양소를 확인하세요." headerAction={<span className={styles.calendar}>🗓️</span>}>
      <Card>
        <p className={styles.totalTitle}>총 섭취 칼로리</p>
        <p className={styles.totalKcal}>{`${totalKcal.toLocaleString()} kcal`}</p>
        <p className={styles.base}>{menuExists ? '선택 부대의 오늘 식단 기준' : '선택 부대의 당일 식단 데이터가 아직 없습니다.'}</p>
        <div className={styles.macroGrid}>
          {macroData.map((macro) => (
            <MacroBox key={macro.label} label={macro.label} intake={macro.intake} target={macro.target} color={macro.color} tone={macro.tone} />
          ))}
        </div>
        <ProgressBar value={totalKcal ?? 0} max={targetKcal || 1} />
        <small>
          {menuExists
            ? '선택 부대의 오늘 식단을 기본 섭취량으로 계산했어요. 실제 섭취량이 다르면 끼니별로 수정할 수 있어요.'
            : '당일 식단 데이터가 없어 섭취량은 0으로 계산되었습니다.'}
        </small>
      </Card>

      <Card>
        <div className={styles.row}><strong>선택 부대</strong><button type="button" onClick={() => window.location.assign('/diet/add')}>음식 추가</button></div>
        <p className={styles.fixed}>{unit?.unitName || '선택된 부대가 없습니다'}</p>
      </Card>

      {[...mealLabels, { key: 'snackRaw', label: '간식' }].map((section) => {
        const items = section.key === 'snackRaw' ? [] : parseMeal(meal?.[section.key]);
        const kcalKey = `${section.key.replace('Raw', 'Kcal')}`;
        const mealKcal = section.key === 'snackRaw' ? 0 : meal?.[kcalKey];
        return (
          <Card key={section.key}>
            <div className={styles.row}>
              <h3>{section.label}</h3>
              <span className={styles.mealKcal}>{formatKcal(mealKcal)}</span>
            </div>
            {items.length > 0 ? (
              <div className={styles.extraWrap}>
                {items.map((item) => <div key={item} className={styles.item}><span>{item}</span></div>)}
              </div>
            ) : (
              <p className={styles.base}>선택 부대의 당일 식단 데이터가 아직 없습니다.</p>
            )}
          </Card>
        );
      })}

      {loading ? <p className={styles.base}>불러오는 중...</p> : null}
      {errorMessage ? <p className={styles.base}>{errorMessage}</p> : null}
    </AppLayout>
  );
}
