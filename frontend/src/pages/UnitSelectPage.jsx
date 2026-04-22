import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppLayout from '../components/layout/AppLayout';
import Card from '../components/ui/Card';
import { findUnitsByMeal, getUnits, searchUnits, setMyUnit } from '../api/unitApi';
import styles from '../features/design/SetupPage.module.css';

function formatDate(date) {
  return date.toISOString().slice(0, 10);
}

function toPercent(score) {
  return `${Math.round((score ?? 0) * 100)}%`;
}

export default function UnitSelectPage() {
  const navigate = useNavigate();

  const [mode, setMode] = useState('meal');
  const [selectedId, setSelectedId] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const [units, setUnits] = useState([]);
  const [query, setQuery] = useState('');
  const [loadingUnits, setLoadingUnits] = useState(false);

  const [mealDate, setMealDate] = useState(formatDate(new Date()));
  const [breakfast, setBreakfast] = useState('');
  const [lunch, setLunch] = useState('');
  const [dinner, setDinner] = useState('');
  const [mealCandidates, setMealCandidates] = useState([]);
  const [mealSearchState, setMealSearchState] = useState('idle');

  useEffect(() => {
    let isMounted = true;

    async function loadUnits() {
      try {
        setLoadingUnits(true);
        const unitList = await getUnits();
        if (!isMounted) return;
        setUnits(unitList ?? []);
      } catch (error) {
        if (!isMounted) return;
        setErrorMessage(error.message || '부대 목록을 불러오지 못했습니다.');
      } finally {
        if (isMounted) {
          setLoadingUnits(false);
        }
      }
    }

    loadUnits();
    return () => {
      isMounted = false;
    };
  }, []);

  useEffect(() => {
    if (mode !== 'name') return;

    let isMounted = true;
    const timer = setTimeout(async () => {
      try {
        setLoadingUnits(true);
        const list = query.trim() ? await searchUnits(query.trim()) : await getUnits();
        if (!isMounted) return;
        setUnits(list ?? []);
      } catch (error) {
        if (!isMounted) return;
        setErrorMessage(error.message || '부대 검색에 실패했습니다.');
      } finally {
        if (isMounted) {
          setLoadingUnits(false);
        }
      }
    }, 250);

    return () => {
      isMounted = false;
      clearTimeout(timer);
    };
  }, [mode, query]);

  const nameCandidates = useMemo(() => units ?? [], [units]);

  const handleFindByMeal = async () => {
    if (!breakfast.trim() && !lunch.trim() && !dinner.trim()) {
      setErrorMessage('아침/점심/저녁 중 기억나는 식단을 하나 이상 입력해주세요.');
      return;
    }

    try {
      setErrorMessage('');
      setMealSearchState('loading');
      setSelectedId(null);
      const result = await findUnitsByMeal({
        date: mealDate,
        breakfast,
        lunch,
        dinner,
      });
      const candidates = result ?? [];
      setMealCandidates(candidates);
      setMealSearchState(candidates.length > 0 ? 'success' : 'empty');
      if (candidates[0]?.unitId) {
        setSelectedId(candidates[0].unitId);
      }
    } catch (error) {
      setMealSearchState('error');
      setErrorMessage(error.message || '식단 기반 부대 찾기에 실패했습니다.');
    }
  };

  const handleSelectUnit = async () => {
    if (!selectedId) return;

    try {
      setSubmitting(true);
      setErrorMessage('');
      await setMyUnit(selectedId);
      navigate('/setup/equipment');
    } catch (error) {
      setErrorMessage(error.message || '부대 선택 저장에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AppLayout title="부대 식단 선택" subtitle="소속 부대를 선택해주세요." showBottomNav={false}>
      <div className={styles.modeSwitch}>
        <button type="button" className={mode === 'meal' ? styles.activeChip : ''} onClick={() => setMode('meal')}>
          식단으로 찾기
        </button>
        <button type="button" className={mode === 'name' ? styles.activeChip : ''} onClick={() => setMode('name')}>
          부대명으로 찾기
        </button>
      </div>

      {mode === 'meal' ? (
        <>
          <Card className={styles.mealFinderCard}>
            <p className={styles.helperTitle}>오늘 먹은 식단으로 소속 부대를 찾아보세요</p>
            <small className={styles.helperText}>아침/점심/저녁 중 기억나는 식단만 입력해도 돼요. 가장 비슷한 식단의 부대를 추천해드릴게요.</small>
            <div className={styles.formGrid}>
              <label>
                날짜
                <input type="date" value={mealDate} onChange={(e) => setMealDate(e.target.value)} className={styles.search} />
              </label>
              <label>
                아침
                <input
                  className={styles.search}
                  placeholder="예: 우유 계란말이 김치"
                  value={breakfast}
                  onChange={(e) => setBreakfast(e.target.value)}
                />
              </label>
              <label>
                점심
                <input
                  className={styles.search}
                  placeholder="예: 닭볶음탕 두부조림 깍두기"
                  value={lunch}
                  onChange={(e) => setLunch(e.target.value)}
                />
              </label>
              <label>
                저녁
                <input
                  className={styles.search}
                  placeholder="예: 돼지고기볶음 된장국 김치"
                  value={dinner}
                  onChange={(e) => setDinner(e.target.value)}
                />
              </label>
            </div>
            <button type="button" className={styles.secondary} onClick={handleFindByMeal}>
              이 식단으로 부대 찾기
            </button>
          </Card>

          {mealSearchState === 'idle' ? <p className={styles.infoText}>식단을 입력하면 유사도가 높은 부대 후보를 보여드려요.</p> : null}
          {mealSearchState === 'loading' ? <p className={styles.infoText}>식단을 비교해 부대 후보를 찾는 중이에요...</p> : null}
          {mealSearchState === 'empty' ? (
            <p className={styles.infoText}>해당 날짜의 일치 후보가 없어요. 날짜를 바꾸거나 부대명 검색을 이용해보세요.</p>
          ) : null}

          {mealCandidates.map((unit) => (
            <Card key={unit.unitId} className={`${styles.selectCard} ${selectedId === unit.unitId ? styles.selected : ''}`}>
              <button type="button" onClick={() => setSelectedId(unit.unitId)}>
                <div className={styles.cardHeaderRow}>
                  <p>{unit.unitName}</p>
                  <span className={styles.scoreBadge}>유사도 {toPercent(unit.matchScore)}</span>
                </div>
                <small>{unit.branchType} · {unit.regionName}</small>
                <small className={styles.matchMealText}>일치 끼니: {unit.matchedMeals?.length ? unit.matchedMeals.join(', ') : '없음'}</small>
                <small className={styles.matchMealText}>
                  아침 {toPercent(unit.mealMatchDetail?.breakfastScore)} · 점심 {toPercent(unit.mealMatchDetail?.lunchScore)} · 저녁 {toPercent(unit.mealMatchDetail?.dinnerScore)}
                </small>
                <div className={styles.previewBox}>
                  <small>아침: {unit.mealPreview?.breakfast || '-'}</small>
                  <small>점심: {unit.mealPreview?.lunch || '-'}</small>
                  <small>저녁: {unit.mealPreview?.dinner || '-'}</small>
                </div>
              </button>
            </Card>
          ))}
        </>
      ) : (
        <>
          <input className={styles.search} placeholder="부대명/지역 검색" value={query} onChange={(e) => setQuery(e.target.value)} />
          {loadingUnits ? <p className={styles.infoText}>부대를 불러오는 중이에요...</p> : null}
          {!loadingUnits && nameCandidates.length === 0 ? <p className={styles.infoText}>검색 결과가 없어요.</p> : null}
          {nameCandidates.map((unit) => (
            <Card key={unit.id} className={`${styles.selectCard} ${selectedId === unit.id ? styles.selected : ''}`}>
              <button type="button" onClick={() => setSelectedId(unit.id)}>
                <p>{unit.unitName}</p>
                <small>{unit.branchType} · {unit.regionName}</small>
              </button>
            </Card>
          ))}
        </>
      )}

      {errorMessage ? <p className={styles.errorText}>{errorMessage}</p> : null}
      <button type="button" className={styles.primary} onClick={handleSelectUnit} disabled={!selectedId || submitting}>
        {submitting ? '저장 중...' : '이 부대로 선택'}
      </button>
    </AppLayout>
  );
}
