import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import AppLayout from '../../components/layout/AppLayout';
import Card from '../../components/ui/Card';
import { addMealFoods, searchFoods } from '../../api/nutritionApi';
import styles from './DietPage.module.css';
import screen from '../../components/ui/Screen.module.css';

const mealLabels = {
  breakfast: '아침',
  lunch: '점심',
  dinner: '저녁',
  snack: '간식',
};

function formatGram(value) {
  return `${(Number(value) || 0).toLocaleString(undefined, { maximumFractionDigits: 1 })}g`;
}

export default function DietAddPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const mealType = mealLabels[searchParams.get('meal')] ? searchParams.get('meal') : 'snack';
  const selectedMeal = mealLabels[mealType] ?? '식단';
  const [keyword, setKeyword] = useState('');
  const [selected, setSelected] = useState([]);
  const [foods, setFoods] = useState([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState('');

  useEffect(() => {
    const trimmed = keyword.trim();
    if (trimmed.length < 1) {
      setFoods([]);
      setMessage('원하는 음식을 입력하면 엑셀 식품 DB에서 비슷한 음식을 찾아드려요.');
      return undefined;
    }

    const timer = window.setTimeout(async () => {
      try {
        setLoading(true);
        setMessage('');
        const response = await searchFoods(trimmed);
        const results = Array.isArray(response) ? response : (response?.results ?? []);
        setFoods(results);
        if (!results.length) setMessage('검색 결과가 없습니다. 다른 음식명으로 입력해보세요.');
      } catch {
        setFoods([]);
        setMessage('음식 검색에 실패했습니다. 잠시 후 다시 시도해주세요.');
      } finally {
        setLoading(false);
      }
    }, 400);

    return () => window.clearTimeout(timer);
  }, [keyword]);

  const selectedIds = useMemo(() => new Set(selected.map((food) => food.foodMasterId ?? food.id)), [selected]);

  const selectedSummary = useMemo(() => selected.reduce((sum, food) => ({
    calories: sum.calories + (food.calculated?.calories || food.calories || 0),
    carbG: sum.carbG + (food.calculated?.carbG || food.carbG || 0),
    proteinG: sum.proteinG + (food.calculated?.proteinG || food.proteinG || 0),
    fatG: sum.fatG + (food.calculated?.fatG || food.fatG || 0),
  }), { calories: 0, carbG: 0, proteinG: 0, fatG: 0 }), [selected]);

  const enrichFood = (food, servingGram = food.defaultServingGram ?? 100) => {
    const scale = (Number(servingGram) || 0) / 100;
    return {
      ...food,
      servingGram: Number(servingGram) || 0,
      calculated: {
        calories: Math.round((food.kcalPer100g ?? food.calories ?? 0) * scale),
        carbG: Math.round((food.carbohydratePer100g ?? food.carbG ?? 0) * scale * 10) / 10,
        proteinG: Math.round((food.proteinPer100g ?? food.proteinG ?? 0) * scale * 10) / 10,
        fatG: Math.round((food.fatPer100g ?? food.fatG ?? 0) * scale * 10) / 10,
      },
    };
  };

  const toggleFood = (food) => {
    const id = food.foodMasterId ?? food.id;
    setSelected((prev) => (prev.some((item) => (item.foodMasterId ?? item.id) === id) ? prev.filter((item) => (item.foodMasterId ?? item.id) !== id) : [...prev, enrichFood(food)]));
  };

  const updateServingGram = (foodId, servingGram) => {
    setSelected((prev) => prev.map((food) => ((food.foodMasterId ?? food.id) === foodId ? enrichFood(food, servingGram) : food)));
  };

  const saveSelected = async () => {
    if (!selected.length) return;
    try {
      setSaving(true);
      const servingGramByFoodId = Object.fromEntries(selected.map((food) => [food.foodMasterId ?? food.id, food.servingGram ?? food.defaultServingGram ?? 100]));
      await addMealFoods(mealType, selected.map((food) => food.foodMasterId ?? food.id), servingGramByFoodId);
      navigate('/diet', { replace: true, state: { mealUpdatedAt: Date.now(), mealType } });
    } catch {
      setMessage('선택한 음식을 식단에 추가하지 못했습니다. 다시 시도해주세요.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <AppLayout
      title={`${selectedMeal} 음식 추가`}
      subtitle="음식을 선택한 뒤 아래 완료 버튼을 누르면 식단에 바로 반영됩니다."
      showBottomNav={false}
      headerAction={<button type="button" className={styles.closeButton} onClick={() => navigate('/diet', { replace: true })}>닫기</button>}
    >
      <span className={styles.mealContext}>{selectedMeal}에 추가할 음식을 검색해주세요.</span>
      <input className={screen.input} placeholder="예: 짜장면, 닭가슴살, 우유" value={keyword} onChange={(e) => setKeyword(e.target.value)} autoFocus />

      <Card>
        <p className={styles.totalTitle}>선택한 음식 {selected.length}개</p>
        <div className={styles.selectedSummary}>
          <span>{selectedSummary.calories.toLocaleString()} kcal</span>
          <span>탄 {formatGram(selectedSummary.carbG)}</span>
          <span>단 {formatGram(selectedSummary.proteinG)}</span>
          <span>지 {formatGram(selectedSummary.fatG)}</span>
        </div>
      </Card>

      {message ? <p className={styles.base}>{message}</p> : null}
      {loading ? <p className={styles.base}>검색 중...</p> : null}

      <div className={screen.list}>
        {foods.map((food) => (
          <Card key={food.foodMasterId ?? food.id}>
            <div className={styles.searchFoodCard}>
              <div>
                <strong>{food.representativeName ?? food.foodName}</strong>
                <p>100g당 {food.kcalPer100g ?? food.calories ?? 0} kcal · 탄 {formatGram(food.carbohydratePer100g ?? food.carbG)} · 단 {formatGram(food.proteinPer100g ?? food.proteinG)} · 지 {formatGram(food.fatPer100g ?? food.fatG)}</p>
                <small>{food.displayCategory ?? food.category ?? '분류 없음'} · 기본 {formatGram(food.defaultServingGram ?? 100)} · 하위 원본 데이터 {food.matchedAliasCount ?? 1}개 기반</small>
              </div>
              <button
                type="button"
                aria-label={`${food.representativeName ?? food.foodName} ${selectedIds.has(food.foodMasterId ?? food.id) ? '선택 해제' : '선택'}`}
                onClick={() => toggleFood(food)}
              >{selectedIds.has(food.foodMasterId ?? food.id) ? '선택됨' : '선택'}</button>
            </div>
            {selectedIds.has(food.foodMasterId ?? food.id) ? (
              <div className={styles.servingEditor}>
                <label>제공량(g)</label>
                <input
                  type="number"
                  min="0"
                  value={selected.find((item) => (item.foodMasterId ?? item.id) === (food.foodMasterId ?? food.id))?.servingGram ?? food.defaultServingGram ?? 100}
                  onChange={(event) => updateServingGram(food.foodMasterId ?? food.id, event.target.value)}
                />
                <span>{selected.find((item) => (item.foodMasterId ?? item.id) === (food.foodMasterId ?? food.id))?.calculated?.calories ?? food.estimatedKcalForDefaultServing ?? 0} kcal</span>
              </div>
            ) : null}
          </Card>
        ))}
      </div>

      <div className={styles.addFooter}>
        <button type="button" className={screen.secondaryButton} onClick={() => navigate('/diet', { replace: true })} disabled={saving}>취소</button>
        <button type="button" className={screen.primaryButton} onClick={saveSelected} disabled={!selected.length || saving}>
          {saving ? '식단에 반영 중...' : selected.length ? `${selected.length}개 추가하고 식단으로 돌아가기` : '추가할 음식을 선택해주세요'}
        </button>
      </div>
    </AppLayout>
  );
}
