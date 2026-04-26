import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppLayout from '../components/layout/AppLayout';
import Card from '../components/ui/Card';
import { findUnitsByMeal, getMealOptionsByDate, getUnits, searchUnits, setMyUnit } from '../api/unitApi';
import styles from '../features/design/SetupPage.module.css';

function formatDate(date) {
  return date.toISOString().slice(0, 10);
}

function toPercent(score) {
  return `${Math.round((score ?? 0) * 100)}%`;
}

const MEAL_TYPES = [
  { key: 'BREAKFAST', label: '아침', requestKey: 'breakfast' },
  { key: 'LUNCH', label: '점심', requestKey: 'lunch' },
  { key: 'DINNER', label: '저녁', requestKey: 'dinner' },
];

const EMPTY_SELECTED_MENUS = {
  BREAKFAST: [],
  LUNCH: [],
  DINNER: [],
};

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
  const [selectedMealType, setSelectedMealType] = useState('BREAKFAST');
  const [mealSearchKeyword, setMealSearchKeyword] = useState('');
  const [selectedMenuItemsByMeal, setSelectedMenuItemsByMeal] = useState(EMPTY_SELECTED_MENUS);
  const [menuOptions, setMenuOptions] = useState([]);
  const [loadingMealOptions, setLoadingMealOptions] = useState(false);
  const [mealCandidates, setMealCandidates] = useState([]);
  const [mealSearchState, setMealSearchState] = useState('idle');

  const currentMeal = MEAL_TYPES.find((mealType) => mealType.key === selectedMealType);
  const currentSelectedItems = selectedMenuItemsByMeal[selectedMealType] ?? [];

  const selectedCounts = MEAL_TYPES.map((mealType) => ({
    ...mealType,
    count: selectedMenuItemsByMeal[mealType.key]?.length ?? 0,
  }));

  const totalSelectedCount = selectedCounts.reduce((sum, mealType) => sum + mealType.count, 0);

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

  useEffect(() => {
    if (mode !== 'meal') return;

    let isMounted = true;
    const timer = setTimeout(async () => {
      try {
        setLoadingMealOptions(true);
        const options = await getMealOptionsByDate({
          date: mealDate,
          mealType: selectedMealType,
          keyword: mealSearchKeyword.trim() || undefined,
        });

        if (!isMounted) return;
        setMenuOptions(options ?? []);
      } catch (error) {
        if (!isMounted) return;
        setMenuOptions([]);
        setErrorMessage(error.message || '식단 메뉴를 불러오지 못했습니다.');
      } finally {
        if (isMounted) {
          setLoadingMealOptions(false);
        }
      }
    }, 180);

    return () => {
      isMounted = false;
      clearTimeout(timer);
    };
  }, [mode, mealDate, selectedMealType, mealSearchKeyword]);

  useEffect(() => {
    setSelectedMenuItemsByMeal(EMPTY_SELECTED_MENUS);
    setMealCandidates([]);
    setMealSearchState('idle');
    setSelectedId(null);
  }, [mealDate]);

  useEffect(() => {
    setMealSearchKeyword('');
  }, [selectedMealType]);

  const nameCandidates = useMemo(() => units ?? [], [units]);

  const toggleMenuItem = (item) => {
    setSelectedMenuItemsByMeal((prev) => {
      const currentItems = prev[selectedMealType] ?? [];
      const nextItems = currentItems.includes(item)
        ? currentItems.filter((menu) => menu !== item)
        : [...currentItems, item];

      return {
        ...prev,
        [selectedMealType]: nextItems,
      };
    });

    setMealCandidates([]);
    setMealSearchState('idle');
    setSelectedId(null);
  };

  const moveToMealType = (direction) => {
    const currentIndex = MEAL_TYPES.findIndex((mealType) => mealType.key === selectedMealType);
    const nextIndex = currentIndex + direction;

    if (nextIndex < 0 || nextIndex >= MEAL_TYPES.length) {
      return;
    }

    setSelectedMealType(MEAL_TYPES[nextIndex].key);
  };

  const handleFindByMeal = async () => {
    if (totalSelectedCount === 0) {
      setErrorMessage('아침, 점심, 저녁 중 기억나는 메뉴를 하나 이상 선택해주세요.');
      return;
    }

    try {
      setErrorMessage('');
      setMealSearchState('loading');
      setSelectedId(null);

      const payload = {
        date: mealDate,
        breakfast: selectedMenuItemsByMeal.BREAKFAST.length > 0
          ? selectedMenuItemsByMeal.BREAKFAST.join(' ')
          : null,
        lunch: selectedMenuItemsByMeal.LUNCH.length > 0
          ? selectedMenuItemsByMeal.LUNCH.join(' ')
          : null,
        dinner: selectedMenuItemsByMeal.DINNER.length > 0
          ? selectedMenuItemsByMeal.DINNER.join(' ')
          : null,
      };

      const result = await findUnitsByMeal(payload);
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
            <p className={styles.helperTitle}>아침·점심·저녁에서 기억나는 메뉴를 골라주세요</p>
            <small className={styles.helperText}>
              끼니별로 메뉴를 선택한 뒤 한 번에 비교해서 가장 비슷한 식단의 부대를 추천해드릴게요.
            </small>

            <div className={styles.formGrid}>
              <label>
                날짜
                <input type="date" value={mealDate} onChange={(e) => setMealDate(e.target.value)} className={styles.search} />
              </label>
            </div>

            <div className={styles.mealTypeButtons}>
              {selectedCounts.map((mealType) => (
                <button
                  key={mealType.key}
                  type="button"
                  className={selectedMealType === mealType.key ? styles.activeChip : ''}
                  onClick={() => setSelectedMealType(mealType.key)}
                >
                  {mealType.label}
                  {mealType.count > 0 ? ` ${mealType.count}` : ''}
                </button>
              ))}
            </div>

            <small className={styles.helperText}>
              현재 선택 중: {currentMeal?.label} · 선택한 메뉴 {currentSelectedItems.length}개
            </small>

            <input
              className={styles.search}
              placeholder={`${currentMeal?.label ?? '메뉴'} 메뉴 검색 (예: 김치, 국, 볶음)`}
              value={mealSearchKeyword}
              onChange={(e) => setMealSearchKeyword(e.target.value)}
            />

            {loadingMealOptions ? <p className={styles.infoText}>해당 날짜 메뉴를 불러오는 중이에요...</p> : null}

            {!loadingMealOptions && menuOptions.length > 0 ? (
              <div className={styles.menuGrid}>
                {menuOptions.map((item) => (
                  <button
                    key={item}
                    type="button"
                    className={`${styles.menuChip} ${currentSelectedItems.includes(item) ? styles.menuChipSelected : ''}`}
                    onClick={() => toggleMenuItem(item)}
                  >
                    {item}
                  </button>
                ))}
              </div>
            ) : null}

            {!loadingMealOptions && menuOptions.length === 0 ? (
              <p className={styles.infoText}>해당 조건의 메뉴가 없어요. 날짜/검색어를 조정해보세요.</p>
            ) : null}

            <div className={styles.previewBox}>
              <small>아침: {selectedMenuItemsByMeal.BREAKFAST.length > 0 ? selectedMenuItemsByMeal.BREAKFAST.join(', ') : '선택 안 함'}</small>
              <small>점심: {selectedMenuItemsByMeal.LUNCH.length > 0 ? selectedMenuItemsByMeal.LUNCH.join(', ') : '선택 안 함'}</small>
              <small>저녁: {selectedMenuItemsByMeal.DINNER.length > 0 ? selectedMenuItemsByMeal.DINNER.join(', ') : '선택 안 함'}</small>
            </div>

            <small className={styles.helperText}>전체 선택한 메뉴 수: {totalSelectedCount}개</small>

            <div className={styles.modeSwitch}>
              <button type="button" className={styles.secondary} onClick={() => moveToMealType(-1)} disabled={selectedMealType === 'BREAKFAST'}>
                이전 끼니
              </button>
              <button type="button" className={styles.secondary} onClick={() => moveToMealType(1)} disabled={selectedMealType === 'DINNER'}>
                다음 끼니
              </button>
            </div>

            <button type="button" className={styles.secondary} onClick={handleFindByMeal} disabled={totalSelectedCount === 0}>
              선택한 전체 식단으로 부대 찾기
            </button>
          </Card>

          {mealSearchState === 'idle' ? <p className={styles.infoText}>아침·점심·저녁 메뉴를 선택하면 유사도가 높은 부대 후보를 보여드려요.</p> : null}
          {mealSearchState === 'loading' ? <p className={styles.infoText}>식단을 비교해 부대 후보를 찾는 중이에요...</p> : null}
          {mealSearchState === 'empty' ? <p className={styles.infoText}>일치하는 후보가 없어요. 메뉴 선택을 조정해보세요.</p> : null}

          {mealCandidates.map((unit) => (
            <Card key={unit.unitId} className={`${styles.selectCard} ${selectedId === unit.unitId ? styles.selected : ''}`}>
              <button type="button" onClick={() => setSelectedId(unit.unitId)}>
                <div className={styles.cardHeaderRow}>
                  <p>{unit.unitName}</p>
                  <span className={styles.scoreBadge}>유사도 {toPercent(unit.matchScore)}</span>
                </div>

                <small>{unit.branchType} · {unit.regionName || '지역 미분류'}</small>
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