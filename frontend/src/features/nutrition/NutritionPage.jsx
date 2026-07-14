import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppLayout from '../../components/layout/AppLayout';
import Card from '../../components/ui/Card';
import ProgressBar from '../../components/ui/ProgressBar';
import MacroBox from '../../components/ui/MacroBox';
import { getTodayMeal } from '../../api/mealApi';
import { getTodayNutritionOverview, saveTodayMealConsumption } from '../../api/nutritionApi';
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

function nutritionSourceLabel(source) {
  return ({
    DAILY_UNIT_MENU: '해당 날짜 부대 식단',
    UNIT_MENU_PROFILE: '부대 식단 중앙값',
    GLOBAL_MENU_PROFILE: '전체 부대 중앙값',
    OVERRIDE_EXACT: '검증 식품 DB',
    FOOD_EXACT: '식품 DB',
    ALIAS_EXACT: '식품 DB 별칭',
    COMPOSITE_ESTIMATE: '재료 기반 추정',
    STANDARD_RICE_REFERENCE: '표준 백미밥 기준',
    USER_ADDED: '직접 추가',
    UNAVAILABLE: '정보 없음',
  })[source] || source || '정보 없음';
}

const unitOnlyPattern = /^(?:\d+(?:\.\d+)?\s*)?(?:ml|mL|ML|g|kg|캔|팩|병|개|봉)$|^\d+(?:\.\d+)?$/i;
const softDrinkProducts = ['코카콜라', '콜라', '칠성사이다', '사이다', '환타', '펩시'];

function compact(value) {
  return String(value || '').replace(/\s+/g, '');
}

function isGenericSoftDrink(value) {
  return ['청량음료', '탄산음료'].includes(compact(value));
}

function isSoftDrinkProduct(value) {
  const normalized = compact(value);
  return softDrinkProducts.some((keyword) => normalized.includes(keyword));
}

function normalizeMealItem(value) {
  let item = String(value || '')
    .replace(/\(\s*\d{1,2}\s*\)/g, ' ')
    .replace(/[()]+/g, ' ');
  ['부대계약', '부대 계약', '계약', '연간', '후식', '제공'].forEach((word) => {
    item = item.replaceAll(word, ' ');
  });
  item = item.replace(/\s+/g, ' ').trim();
  if (item.startsWith('우유 ') && item.includes('백색우유')) {
    item = item.slice(item.indexOf('백색우유'));
  }
  if (item.startsWith('청량음료 ') || item.startsWith('탄산음료 ')) {
    const withoutGeneric = item.replace(/^(청량음료|탄산음료)\s+/, '');
    if (isSoftDrinkProduct(withoutGeneric)) item = withoutGeneric;
  }
  return item;
}

function hasOpenParenthesis(value) {
  let balance = 0;
  String(value || '').split('').forEach((char) => {
    if (char === '(') balance += 1;
    if (char === ')') balance -= 1;
  });
  return balance > 0;
}

function shouldAttach(previous, line) {
  const trimmed = line.trim();
  return hasOpenParenthesis(previous)
    || unitOnlyPattern.test(trimmed)
    || ['연간', '부대계약', '부대 계약'].includes(trimmed)
    || (trimmed.includes(')') && /^[^가-힣A-Za-z0-9]*[가-힣A-Za-z0-9\s]*(?:\)+)$/.test(trimmed))
    || (isGenericSoftDrink(previous) && isSoftDrinkProduct(trimmed));
}

function pushMealItem(items, value) {
  const item = normalizeMealItem(value);
  if (!item || unitOnlyPattern.test(item)) return;
  if (items.length > 0 && isGenericSoftDrink(items[items.length - 1]) && isSoftDrinkProduct(item)) {
    items[items.length - 1] = item;
    return;
  }
  if (!isGenericSoftDrink(item)) items.push(item);
}

function parseRawMealItems(rawMenu) {
  if (!rawMenu) return [];
  const items = [];
  String(rawMenu).replace(/\r\n?/g, '\n').split(',').forEach((part) => {
    let current = '';
    part.split('\n').forEach((rawLine) => {
      const line = rawLine.replace(/\(\s*\d{1,2}\s*\)/g, ' ').replace(/\s+/g, ' ').trim();
      if (!line) return;
      if (!current) {
        current = line;
      } else if (shouldAttach(current, line)) {
        current = `${current} ${line}`;
      } else {
        pushMealItem(items, current);
        current = line;
      }
    });
    pushMealItem(items, current);
  });
  return [...new Set(items)];
}

export default function NutritionPage() {
  const navigate = useNavigate();
  const [nutrition, setNutrition] = useState(emptyDashboardSummary);
  const [meal, setMeal] = useState(emptyMealDay);
  const [mealDetails, setMealDetails] = useState({ meals: [] });
  const [unit, setUnit] = useState(null);
  const [loading, setLoading] = useState(true);
  const [savingMealType, setSavingMealType] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    let mounted = true;

    async function loadDietData() {
      setLoading(true);
      setErrorMessage('');

      const [overviewResult, mealResult, unitResult] = await Promise.allSettled([
        getTodayNutritionOverview(),
        getTodayMeal(),
        getMyUnit(),
      ]);

      if (!mounted) return;

      if (overviewResult.status === 'fulfilled') {
        const overview = overviewResult.value;
        setNutrition({ ...emptyDashboardSummary, ...(overview?.summary ?? {}) });
        setMealDetails(overview?.mealDetails ?? { meals: [] });
      } else {
        setNutrition(emptyDashboardSummary);
        setMealDetails({ meals: [] });
      }

      if (mealResult.status === 'fulfilled') {
        setMeal(mealResult.value ? { ...emptyMealDay, ...mealResult.value } : emptyMealDay);
      } else {
        setMeal(emptyMealDay);
      }

      setUnit(unitResult.status === 'fulfilled' ? unitResult.value ?? null : null);

      if ([overviewResult, mealResult, unitResult].some((result) => result.status === 'rejected')) {
        setErrorMessage('일부 식단 데이터를 불러오지 못했습니다. 가능한 정보부터 먼저 표시합니다.');
      }

      setLoading(false);
    }

    loadDietData();

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

  async function updateConsumption(mealType, portionMultiplier) {
    setSavingMealType(mealType);
    setErrorMessage('');
    try {
      const detailData = await saveTodayMealConsumption(mealType, portionMultiplier);
      setMealDetails(detailData ?? { meals: [] });
      setNutrition((prev) => ({
        ...prev,
        intakeCalories: detailData?.totalCalories ?? prev.intakeCalories,
        intakeProteinG: detailData?.totalProteinG ?? prev.intakeProteinG,
        intakeCarbG: detailData?.totalCarbG ?? prev.intakeCarbG,
        intakeFatG: detailData?.totalFatG ?? prev.intakeFatG,
      }));
    } catch (error) {
      setErrorMessage(error?.message || '섭취량을 저장하지 못했습니다.');
    } finally {
      setSavingMealType('');
    }
  }

  if (loading) {
    return (
      <AppLayout title="식단 추정 기록" subtitle="군 급식 메뉴 기반 추정 영양소를 확인하세요.">
        <Card>
          <p className={styles.base}>불러오는 중...</p>
        </Card>
      </AppLayout>
    );
  }

  return (
    <AppLayout title="식단 추정 기록" subtitle="군 급식 메뉴 기반 추정 영양소를 확인하세요." headerAction={headerAction}>
      <Card>
        <p className={styles.totalTitle}>오늘 실제 섭취 현황</p>
        <div className={styles.calorieSummary}>
          <div>
            <span>필요 칼로리</span>
            <strong>{targetKcal.toLocaleString()} kcal</strong>
          </div>
          <div>
            <span>기록한 섭취 칼로리</span>
            <strong>{eatenKcal.toLocaleString()} kcal</strong>
          </div>
        </div>
        <p className={styles.base}>{menuExists ? `기록 기준으로 ${remainingKcal.toLocaleString()} kcal 정도 남았어요.` : '선택 부대의 당일 식단 데이터가 아직 없습니다.'}</p>
        <div className={styles.macroGrid}>
          {macroData.map((macro) => (
            <MacroBox key={macro.label} label={macro.label} intake={macro.intake} target={macro.target} color={macro.color} tone={macro.tone} />
          ))}
        </div>
        <ProgressBar value={eatenKcal} max={targetKcal || 1} />
        <small>
          {menuExists
            ? '칼로리는 군 급식 공식값이며, 탄수화물·단백질·지방은 식품 DB 기반 추정값입니다. 실제 배식량과 조리 방식에 따라 영양소는 달라질 수 있습니다.'
            : '당일 식단 데이터가 없어 추정 섭취 칼로리는 0으로 표시됩니다.'}
        </small>
      </Card>

      {mealSections.map((section) => {
        const detail = detailByMealType[section.addKey];
        const items = detail?.items ?? [];
        const rawItems = section.key === 'snackRaw' ? [] : parseRawMealItems(meal?.[section.key]);
        const officialKcal = Number.isFinite(detail?.officialCalorieKcal)
          ? detail.officialCalorieKcal
          : section.key === 'snackRaw' ? null : meal?.[`${section.key.replace('Raw', 'Kcal')}`];
        return (
            <Card key={section.key}>
            <div className={styles.row}>
              <div>
                <h3>{section.label}</h3>
                <div className={styles.mealKcal}>
                  {Number.isFinite(officialKcal) ? <span>공식 칼로리 {formatKcal(officialKcal)}</span> : <span>칼로리 정보 없음</span>}
                </div>
              </div>
              <button type="button" onClick={() => navigate(`/diet/add?meal=${section.addKey}`)}>{section.label} 추가</button>
            </div>
            {section.addKey !== 'snack' && (items.length > 0 || rawItems.length > 0) ? (
              <div className={styles.portionControls}>
                <span>실제 먹은 양</span>
                {[
                  [0, '안 먹음'],
                  [0.5, '절반'],
                  [1, '기본'],
                  [1.5, '많이'],
                ].map(([value, label]) => (
                  <button
                    type="button"
                    key={value}
                    disabled={savingMealType === section.addKey}
                    className={Number(detail?.consumptionMultiplier ?? 0) === value ? styles.portionActive : ''}
                    onClick={() => updateConsumption(section.addKey, value)}
                  >{label}</button>
                ))}
              </div>
            ) : null}
            {items.length > 0 ? (
              <div className={styles.extraWrap}>
                <p>{section.label} 제공 영양소 · 탄 {formatGram(detail?.carbG)} · 단 {formatGram(detail?.proteinG)} · 지 {formatGram(detail?.fatG)}</p>
                <p>섭취 반영 · {formatKcal(detail?.consumedCalories)} · 탄 {formatGram(detail?.consumedCarbG)} · 단 {formatGram(detail?.consumedProteinG)} · 지 {formatGram(detail?.consumedFatG)}</p>
                {items.map((item, index) => (
                  <div key={`${item.foodName}-${item.id ?? index}`} className={styles.nutritionItem}>
                    <div className={styles.itemHeader}>
                      <strong>{item.foodName}</strong>
                      <span>{Number.isFinite(item.calories) ? formatKcal(item.calories) : '공식 칼로리 정보 없음'}</span>
                    </div>
{item.matchedFoodName && item.matchedFoodName !== item.foodName ? <small>DB 매칭: {item.matchedFoodName}</small> : null}
                    {item.calorieSource ? (
                      <small className={styles.sourceLine}>
                        칼로리: {nutritionSourceLabel(item.calorieSource)} · 탄단지: {nutritionSourceLabel(item.macroSource)}
                      </small>
                    ) : null}
                    {item.matchType === 'COMPOSITE_ESTIMATE' || item.matchStatus === 'COMPOSITE_ESTIMATE' ? <small>복합 음식 추정</small> : null}
                    {item.confidence === 'LOW' ? <small>추정 낮음</small> : null}
                    {item.confidence === 'NONE' || item.matched === false ? <small>NO_MATCH · 매칭 필요</small> : null}
                    <div className={styles.nutrientLine}>
                      {item.matched === false ? null : (<>
                        <span>탄 {formatGram(item.carbG)}</span>
                        <span>단 {formatGram(item.proteinG)}</span>
                        <span>지 {formatGram(item.fatG)}</span>
                      </>)}
                      {item.addedByUser ? <em>추가됨</em> : null}
                    </div>
                  </div>
                ))}
              </div>
            ) : rawItems.length > 0 ? (
              <div className={styles.extraWrap}>
                <p>부대 식단</p>
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

      {errorMessage ? <p className={styles.base}>{errorMessage}</p> : null}
    </AppLayout>
  );
}
