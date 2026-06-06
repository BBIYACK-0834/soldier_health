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
        const results = await searchFoods(trimmed);
        setFoods(results ?? []);
        if (!results?.length) setMessage('검색 결과가 없습니다. 다른 음식명으로 입력해보세요.');
      } catch {
        setFoods([]);
        setMessage('음식 검색에 실패했습니다. 잠시 후 다시 시도해주세요.');
      } finally {
        setLoading(false);
      }
    }, 250);

    return () => window.clearTimeout(timer);
  }, [keyword]);

  const selectedIds = useMemo(() => new Set(selected.map((food) => food.id)), [selected]);

  const selectedSummary = useMemo(() => selected.reduce((sum, food) => ({
    calories: sum.calories + (food.calories || 0),
    carbG: sum.carbG + (food.carbG || 0),
    proteinG: sum.proteinG + (food.proteinG || 0),
    fatG: sum.fatG + (food.fatG || 0),
  }), { calories: 0, carbG: 0, proteinG: 0, fatG: 0 }), [selected]);

  const toggleFood = (food) => {
    setSelected((prev) => (prev.some((item) => item.id === food.id) ? prev.filter((item) => item.id !== food.id) : [...prev, food]));
  };

  const saveSelected = async () => {
    if (!selected.length) return;
    try {
      setSaving(true);
      await addMealFoods(mealType, selected.map((food) => food.id));
      navigate('/diet');
    } catch {
      setMessage('선택한 음식을 식단에 추가하지 못했습니다. 다시 시도해주세요.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <AppLayout title={`${selectedMeal} 음식 추가`} subtitle="엑셀 식품 DB에서 검색한 음식의 칼로리·탄단지를 식단에 반영하세요." showBottomNav={false}>
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
          <Card key={food.id}>
            <div className={styles.searchFoodCard}>
              <div>
                <strong>{food.foodName}</strong>
                <p>{food.calories ?? 0} kcal · 탄 {formatGram(food.carbG)} · 단 {formatGram(food.proteinG)} · 지 {formatGram(food.fatG)}</p>
                <small>{food.category || '분류 없음'} · {food.servingUnit || '기준량 없음'}{food.matchedName && food.matchedName !== food.foodName ? ` · 검색매칭 ${food.matchedName}` : ''}</small>
              </div>
              <button type="button" onClick={() => toggleFood(food)}>{selectedIds.has(food.id) ? '✓' : '+'}</button>
            </div>
          </Card>
        ))}
      </div>

      <button type="button" className={screen.primaryButton} onClick={saveSelected} disabled={!selected.length || saving}>
        {saving ? '추가 중...' : `${selectedMeal}에 선택한 음식 ${selected.length}개 추가`}
      </button>
    </AppLayout>
  );
}
