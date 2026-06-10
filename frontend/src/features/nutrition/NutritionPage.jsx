import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppLayout from '../../components/layout/AppLayout';
import Card from '../../components/ui/Card';
import ProgressBar from '../../components/ui/ProgressBar';
import MacroBox from '../../components/ui/MacroBox';
import { getTodayMeal } from '../../api/mealApi';
import { getTodayMealNutritionDetails, getTodayNutrition } from '../../api/nutritionApi';
import { getMyUnit } from '../../api/unitApi';
import { emptyDashboardSummary, emptyMealDay } from '../../constants/defaultData';
import styles from './DietPage.module.css';

const mealSections = [
  { key: 'breakfastRaw', label: '아침', addKey: 'breakfast' },
  { key: 'lunchRaw', label: '점심', addKey: 'lunch' },
  { key: 'dinnerRaw', label: '저녁', addKey: 'dinner' },
  { key: 'snackRaw', label: '간식', addKey: 'snack' },
];

function hasMealMenuData(meal, detail) {
  if (detail?.meals?.some((section) => section.items?.length > 0)) return true;
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

function formatGram(value) {
  return `${(Number(value) || 0).toLocaleString(undefined, { maximumFractionDigits: 1 })}g`;
}

function parseRawMealItems(rawMenu) {
  if (!rawMenu) return [];
  return String(rawMenu)
    .split(/[,/\n]/)
    .map((item) => item.trim())
    .filter(Boolean);
}

export default function NutritionPage() {
  const navigate = useNavigate();
  const [nutrition, setNutrition] = useState(emptyDashboardSummary);
  const [meal, setMeal] = useState(emptyMealDay);
  const [mealDetails, setMealDetails] = useState({ meals: [] });
  const [unit, setUnit] = useState(null);
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    let mounted = true;

    async function loadBasicDietData() {
      setLoading(true);
      setErrorMessage('');

      const [nutritionResult, mealResult, unitResult] = await Promise.allSettled([
        getTodayNutrition(),
        getTodayMeal(),
        getMyUnit(),
      ]);

      if (!mounted) return;

      if (nutritionResult.status === 'fulfilled') {
        setNutrition(nutritionResult.value ? { ...emptyDashboardSummary, ...nutritionResult.value } : emptyDashboardSummary);
      } else {
        setNutrition(emptyDashboardSummary);
      }

      if (mealResult.status === 'fulfilled') {
        setMeal(mealResult.value ? { ...emptyMealDay, ...mealResult.value } : emptyMealDay);
      } else {
        setMeal(emptyMealDay);
      }

      setUnit(unitResult.status === 'fulfilled' ? unitResult.value ?? null : null);

      if ([nutritionResult, mealResult, unitResult].some((result) => result.status === 'rejected')) {
        setErrorMessage('일부 식단 데이터를 불러오지 못했습니다. 가능한 정보부터 먼저 표시합니다.');
      }

      setLoading(false);
    }

    async function loadMealDetails() {
      setDetailLoading(true);
      try {
        const detailData = await getTodayMealNutritionDetails();
        if (!mounted) return;
        setMealDetails(detailData ?? { meals: [] });
        if (detailData) {
          setNutrition((prev) => ({
            ...prev,
            intakeCalories: detailData.totalCalories ?? prev.intakeCalories,
            intakeProteinG: detailData.totalProteinG ?? prev.intakeProteinG,
            intakeCarbG: detailData.totalCarbG ?? prev.intakeCarbG,
            intakeFatG: detailData.totalFatG ?? prev.intakeFatG,
          }));
        }
      } catch (error) {
        if (!mounted) return;
        setMealDetails({ meals: [] });
        setErrorMessage('음식별 영양소 계산이 지연되고 있습니다. 잠시 후 식단 화면을 다시 열어주세요.');
      } finally {
        if (mounted) setDetailLoading(false);
      }
    }

    loadBasicDietData().then(() => {
      if (mounted) loadMealDetails();
    });

    return () => {
      mounted = false;
    };
  }, []);

  const detailByMealType = useMemo(
    () => Object.fromEntries((mealDetails?.meals ?? []).map((detail) => [detail.mealType, detail])),
    [mealDetails]
  );

  const menuExists = hasMealMenuData(meal, mealDetails);
  const mealTotalKcal = Number.isFinite(mealDetails?.totalCalories)
    ? mealDetails.totalCalories
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

  if (loading) {
    return (
      <AppLayout title="식단 기록" subtitle="날짜별 식단과 영양소를 확인하세요.">
        <Card>
          <p className={styles.base}>불러오는 중...</p>
        </Card>
      </AppLayout>
    );
  }

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
            ? '엑셀 식품 DB로 음식별 칼로리·탄수화물·단백질·지방을 계산하고, 직접 추가한 음식까지 반영했어요.'
            : '당일 식단 데이터가 없어 먹은 칼로리는 0으로 계산되었습니다.'}
        </small>
      </Card>

      {mealSections.map((section) => {
        const detail = detailByMealType[section.addKey];
        const items = detail?.items ?? [];
        const rawItems = section.key === 'snackRaw' ? [] : parseRawMealItems(meal?.[section.key]);
        const mealKcal = Number.isFinite(detail?.calories) ? detail.calories : section.key === 'snackRaw' ? 0 : meal?.[`${section.key.replace('Raw', 'Kcal')}`];
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
                <p>{section.label} 영양소 합계 · 탄 {formatGram(detail?.carbG)} · 단 {formatGram(detail?.proteinG)} · 지 {formatGram(detail?.fatG)}</p>
                {items.map((item, index) => (
                  <div key={`${item.foodName}-${item.id ?? index}`} className={styles.nutritionItem}>
                    <div className={styles.itemHeader}>
                      <strong>{item.foodName}</strong>
                      <span>{formatKcal(item.calories)} · {item.calorieSharePct ?? 0}%</span>
                    </div>
                    {item.matchedFoodName && item.matchedFoodName !== item.foodName ? <small>DB 매칭: {item.matchedFoodName}</small> : null}
                    <div className={styles.nutrientLine}>
                      <span>탄 {formatGram(item.carbG)}</span>
                      <span>단 {formatGram(item.proteinG)}</span>
                      <span>지 {formatGram(item.fatG)}</span>
                      {item.addedByUser ? <em>추가됨</em> : null}
                    </div>
                  </div>
                ))}
              </div>
            ) : rawItems.length > 0 ? (
              <div className={styles.extraWrap}>
                <p>{detailLoading ? '음식별 영양소 계산 전 부대 식단을 먼저 표시합니다.' : '부대 식단'}</p>
                {rawItems.map((item, index) => (
                  <div key={`${section.key}-${item}-${index}`} className={styles.item}>
                    <span>{item}</span>
                  </div>
                ))}
              </div>
            ) : (
              <p className={styles.base}>{section.label === '간식' ? '추가한 간식이 아직 없습니다.' : '선택 부대의 당일 식단 데이터가 아직 없습니다.'}</p>
            )}
          </Card>
        );
      })}

      {detailLoading ? <p className={styles.base}>음식별 영양소를 계산하는 중입니다...</p> : null}
      {errorMessage ? <p className={styles.base}>{errorMessage}</p> : null}
    </AppLayout>
  );
}
