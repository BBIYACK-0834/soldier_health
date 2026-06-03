import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppLayout from '../../components/layout/AppLayout';
import Card from '../../components/ui/Card';
import ProgressBar from '../../components/ui/ProgressBar';
import MacroBox from '../../components/ui/MacroBox';
import { getTodayMeal } from '../../api/mealApi';
import { getTodayNutrition } from '../../api/nutritionApi';
import { getMyUnit } from '../../api/unitApi';
import { emptyDashboardSummary, emptyMealDay } from '../../constants/defaultData';
import styles from './DietPage.module.css';

const mealSections = [
  { key: 'breakfastRaw', label: '아침', addKey: 'breakfast' },
  { key: 'lunchRaw', label: '점심', addKey: 'lunch' },
  { key: 'dinnerRaw', label: '저녁', addKey: 'dinner' },
  { key: 'snackRaw', label: '간식', addKey: 'snack' },
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
  const navigate = useNavigate();
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

  const eatenKcal = menuExists ? mealTotalKcal : 0;
  const targetKcal = Number.isFinite(nutrition?.targetCalories) ? nutrition.targetCalories : 0;
  const remainingKcal = Math.max(targetKcal - eatenKcal, 0);

  const macroData = useMemo(
    () => [
      { label: '탄수화물', intake: nutrition?.intakeCarbG ?? 0, target: nutrition?.targetCarbG ?? 0, color: '#50739a', tone: '#dfe5ef' },
      { label: '단백질', intake: nutrition?.intakeProteinG ?? 0, target: nutrition?.targetProteinG ?? 0, color: '#6f8f55', tone: '#e4e9de' },
      { label: '지방', intake: nutrition?.intakeFatG ?? 0, target: nutrition?.targetFatG ?? 0, color: '#d28a2c', tone: '#efe2cf' },
    ],
    [nutrition]
  );

  const headerAction = (
    <div className={styles.headerActions}>
      <span className={styles.unitBadge}>{unit?.unitName || '부대 미선택'}</span>
      <span className={styles.calendar}>🗓️</span>
    </div>
  );

  return (
    <AppLayout title="식단 기록" subtitle="날짜별 식단과 영양소를 확인하세요." headerAction={headerAction}>
      <Card>
        <p className={styles.totalTitle}>칼로리 현황</p>
        <div className={styles.calorieSummary}>
          <div>
            <span>필요 칼로리</span>
            <strong>{targetKcal.toLocaleString()} kcal</strong>
          </div>
          <div>
            <span>먹은 칼로리</span>
            <strong>{eatenKcal.toLocaleString()} kcal</strong>
          </div>
        </div>
        <p className={styles.base}>{menuExists ? `앞으로 ${remainingKcal.toLocaleString()} kcal 더 먹을 수 있어요.` : '선택 부대의 당일 식단 데이터가 아직 없습니다.'}</p>
        <div className={styles.macroGrid}>
          {macroData.map((macro) => (
            <MacroBox key={macro.label} label={macro.label} intake={macro.intake} target={macro.target} color={macro.color} tone={macro.tone} />
          ))}
        </div>
        <ProgressBar value={eatenKcal} max={targetKcal || 1} />
        <small>
          {menuExists
            ? '필요 칼로리와 선택 부대 식단 기준 먹은 칼로리를 구분해서 계산했어요. 실제 섭취량이 다르면 끼니별 음식 추가로 수정할 수 있어요.'
            : '당일 식단 데이터가 없어 먹은 칼로리는 0으로 계산되었습니다.'}
        </small>
      </Card>

      {mealSections.map((section) => {
        const items = section.key === 'snackRaw' ? [] : parseMeal(meal?.[section.key]);
        const kcalKey = `${section.key.replace('Raw', 'Kcal')}`;
        const mealKcal = section.key === 'snackRaw' ? 0 : meal?.[kcalKey];
        return (
          <Card key={section.key}>
            <div className={styles.row}>
              <div>
                <h3>{section.label}</h3>
                <span className={styles.mealKcal}>{formatKcal(mealKcal)}</span>
              </div>
              <button type="button" onClick={() => navigate(`/diet/add?meal=${section.addKey}`)}>{section.label} 추가</button>
            </div>
            {items.length > 0 ? (
              <div className={styles.extraWrap}>
                {items.map((item) => <div key={item} className={styles.item}><span>{item}</span></div>)}
              </div>
            ) : (
              <p className={styles.base}>{section.label === '간식' ? '추가한 간식이 아직 없습니다.' : '선택 부대의 당일 식단 데이터가 아직 없습니다.'}</p>
            )}
          </Card>
        );
      })}

      {loading ? <p className={styles.base}>불러오는 중...</p> : null}
      {errorMessage ? <p className={styles.base}>{errorMessage}</p> : null}
    </AppLayout>
  );
}
