import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import MobileShell from '../components/layout/MobileShell';
import { getTodayMeal } from '../api/mealApi';
import { getTodayNutritionRecommendation } from '../api/nutritionApi';
import styles from '../features/home/HomeCards.module.css';

export default function NutritionPage() {
  const navigate = useNavigate();
  const [meal, setMeal] = useState(null);
  const [nutrition, setNutrition] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    (async () => {
      const [mealData, nutritionData] = await Promise.all([
        getTodayMeal().catch(() => null),
        getTodayNutritionRecommendation().catch(() => null),
      ]);
      setMeal(mealData);
      setNutrition(nutritionData);
      setIsLoading(false);
    })();
  }, []);

  const summary = nutrition?.summary;
  const proteinGap = summary?.remainingProteinG ?? 0;
  const recommendedSupplement = nutrition?.pxSuggestions?.[0] || '프로틴바';

  return (
    <MobileShell title="식단">
      <section className={styles.card}>
        <h3>오늘 식단</h3>
        {isLoading ? <p className={styles.muted}>식단 정보를 불러오는 중입니다...</p> : null}
        {!isLoading ? (
          <div className={styles.subInfoBox}>
            <p className={styles.subText}>조식: {meal?.breakfastRaw || '정보 없음'}</p>
            <p className={styles.subText}>중식: {meal?.lunchRaw || '정보 없음'}</p>
            <p className={styles.subText}>석식: {meal?.dinnerRaw || '정보 없음'}</p>
          </div>
        ) : null}
      </section>

      <section className={styles.card}>
        <h3>칼로리/영양 현황</h3>
        <p className={styles.item}><span className={styles.label}>섭취 칼로리</span>{summary?.intakeCalories ?? 0}kcal</p>
        <p className={styles.item}><span className={styles.label}>목표 칼로리</span>{summary?.targetCalories ?? 0}kcal</p>
        <p className={styles.item}><span className={styles.label}>남은 칼로리</span>{summary?.remainingCalories ?? 0}kcal</p>
        <p className={styles.item}><span className={styles.label}>단백질 부족</span><span className={styles.strongNumber}>{proteinGap}g</span></p>
      </section>

      <section className={styles.card}>
        <div className={styles.sectionHead}>
          <div>
            <h3>추천 보충</h3>
            <p className={styles.subtitle}>부족 영양 기준 추천</p>
          </div>
          <button type="button" className={styles.secondaryButton} onClick={() => navigate('/community')}>
            전우 추천 보기
          </button>
        </div>
        <p className={styles.item}><span className={styles.label}>추천</span>{recommendedSupplement}</p>
        <p className={styles.subText}>{nutrition?.recommendationText || '추천 정보가 없습니다.'}</p>
      </section>
    </MobileShell>
  );
}
