import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppLayout from '../components/layout/AppLayout';
import Card from '../components/ui/Card';
import { findUnitsBySelectedMenus, getMealMenuCandidates, getUnits, searchUnits, setMyUnit } from '../api/unitApi';
import styles from '../features/design/SetupPage.module.css';

function formatDate(date) {
  return date.toISOString().slice(0, 10);
}

function mealLabel(mealType) {
  if (mealType === 'breakfast') return '아침';
  if (mealType === 'lunch') return '점심';
  return '저녁';
}

function toPercent(score) {
  return `${Math.round((score ?? 0) * 100)}%`;
}

export default function UnitSelectPage() {
  const navigate = useNavigate();

  const [mode, setMode] = useState('menu');
  const [selectedId, setSelectedId] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const [mealDate, setMealDate] = useState(formatDate(new Date()));
  const [mealType, setMealType] = useState('dinner');
  const [menuCandidates, setMenuCandidates] = useState([]);
  const [selectedMenus, setSelectedMenus] = useState([]);
  const [matchResult, setMatchResult] = useState({ candidateCount: 0, units: [] });
  const [loadingMenus, setLoadingMenus] = useState(false);
  const [loadingMatches, setLoadingMatches] = useState(false);

  const [units, setUnits] = useState([]);
  const [query, setQuery] = useState('');
  const [loadingUnits, setLoadingUnits] = useState(false);

  useEffect(() => {
    let mounted = true;

    async function fetchCandidates() {
      try {
        setLoadingMenus(true);
        setErrorMessage('');
        setSelectedId(null);
        setSelectedMenus([]);
        setMatchResult({ candidateCount: 0, units: [] });
        const response = await getMealMenuCandidates({ date: mealDate, mealType });
        if (!mounted) return;
        setMenuCandidates(response?.menus ?? []);
      } catch (error) {
        if (!mounted) return;
        setMenuCandidates([]);
        setErrorMessage(error.message || '메뉴 후보를 불러오지 못했습니다.');
      } finally {
        if (mounted) setLoadingMenus(false);
      }
    }

    if (mode === 'menu') {
      fetchCandidates();
    }

    return () => {
      mounted = false;
    };
  }, [mealDate, mealType, mode]);

  useEffect(() => {
    let mounted = true;

    async function loadNameCandidates() {
      try {
        setLoadingUnits(true);
        const list = query.trim() ? await searchUnits(query.trim()) : await getUnits();
        if (!mounted) return;
        setUnits(list ?? []);
      } catch (error) {
        if (!mounted) return;
        setErrorMessage(error.message || '부대 검색에 실패했습니다.');
      } finally {
        if (mounted) setLoadingUnits(false);
      }
    }

    if (mode !== 'name') return;
    const timer = setTimeout(loadNameCandidates, 250);

    return () => {
      mounted = false;
      clearTimeout(timer);
    };
  }, [mode, query]);

  useEffect(() => {
    let mounted = true;

    async function fetchMatches() {
      if (selectedMenus.length === 0) {
        setMatchResult({ candidateCount: menuCandidates.length, units: [] });
        return;
      }

      try {
        setLoadingMatches(true);
        setErrorMessage('');
        setSelectedId(null);
        const response = await findUnitsBySelectedMenus({
          date: mealDate,
          mealType,
          selectedMenus,
        });
        if (!mounted) return;
        setMatchResult(response ?? { candidateCount: 0, units: [] });
      } catch (error) {
        if (!mounted) return;
        setMatchResult({ candidateCount: 0, units: [] });
        setErrorMessage(error.message || '메뉴 기반 후보 찾기에 실패했습니다.');
      } finally {
        if (mounted) setLoadingMatches(false);
      }
    }

    if (mode === 'menu') {
      fetchMatches();
    }

    return () => {
      mounted = false;
    };
  }, [selectedMenus, mealDate, mealType, mode, menuCandidates.length]);

  const toggleMenu = (menuName) => {
    setSelectedMenus((prev) => (prev.includes(menuName) ? prev.filter((item) => item !== menuName) : [...prev, menuName]));
  };

  const nameCandidates = useMemo(() => units ?? [], [units]);

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

  const currentCandidateCount = selectedMenus.length === 0 ? menuCandidates.length : matchResult.candidateCount;

  return (
    <AppLayout title="식단표 찾기" subtitle="메뉴 선택으로 내 부대를 찾아보세요." showBottomNav={false}>
      <div className={styles.modeSwitch}>
        <button type="button" className={mode === 'menu' ? styles.activeChip : ''} onClick={() => setMode('menu')}>
          메뉴로 찾기
        </button>
        <button type="button" className={mode === 'name' ? styles.activeChip : ''} onClick={() => setMode('name')}>
          직접 부대명으로 찾기
        </button>
      </div>

      {mode === 'menu' ? (
        <>
          <div className={styles.stepIndicator}>
            <span className={styles.stepActive}>1. 끼니 선택</span>
            <span>2. 메뉴 선택</span>
            <span>3. 부대 선택</span>
          </div>

          <Card className={styles.mealFinderCard}>
            <div className={styles.inlineTwo}>
              <label>
                날짜
                <input type="date" value={mealDate} onChange={(e) => setMealDate(e.target.value)} className={styles.search} />
              </label>
              <div>
                <small className={styles.mealTypeLabel}>끼니 선택</small>
                <div className={styles.chips}>
                  {['breakfast', 'lunch', 'dinner'].map((type) => (
                    <button key={type} type="button" className={mealType === type ? styles.activeChip : ''} onClick={() => setMealType(type)}>
                      {mealLabel(type)}
                    </button>
                  ))}
                </div>
              </div>
            </div>

            <p className={styles.helperTitle}>
              <span className={styles.pointText}>오늘 {mealLabel(mealType)}</span>에 나온 메뉴를 찾아주세요!
            </p>
            <small className={styles.helperText}>고르신 메뉴를 기반으로 관련된 식단표를 찾아드릴게요.</small>
            <p className={styles.candidateCount}>식단표 후보 수: {currentCandidateCount}개</p>
          </Card>

          {loadingMenus ? <p className={styles.infoText}>메뉴 후보를 불러오는 중이에요...</p> : null}
          {!loadingMenus && menuCandidates.length === 0 ? (
            <p className={styles.infoText}>해당 날짜/끼니의 메뉴 후보가 적어요. 날짜나 끼니를 바꿔보세요.</p>
          ) : null}

          <div className={styles.menuGrid}>
            {menuCandidates.map((menuName) => (
              <button
                key={menuName}
                type="button"
                className={`${styles.menuChip} ${selectedMenus.includes(menuName) ? styles.menuChipSelected : ''}`}
                onClick={() => toggleMenu(menuName)}
              >
                {menuName}
              </button>
            ))}
          </div>

          {selectedMenus.length === 0 ? <p className={styles.infoText}>메뉴를 1개 이상 선택하면 관련 부대 후보를 추천해드려요.</p> : null}
          {selectedMenus.length > 0 && currentCandidateCount > 5 ? (
            <p className={styles.infoText}>메뉴를 1~2개 더 선택하면 더 정확하게 찾을 수 있어요.</p>
          ) : null}
          {loadingMatches ? <p className={styles.infoText}>선택한 메뉴로 후보를 좁히는 중이에요...</p> : null}
          {!loadingMatches && selectedMenus.length > 0 && (matchResult.units?.length ?? 0) === 0 ? (
            <p className={styles.infoText}>일치하는 후보가 없어요. 메뉴 선택을 조정해보세요.</p>
          ) : null}

          {(matchResult.units ?? []).map((unit) => (
            <Card key={unit.unitId} className={`${styles.selectCard} ${selectedId === unit.unitId ? styles.selected : ''}`}>
              <button type="button" onClick={() => setSelectedId(unit.unitId)}>
                <div className={styles.cardHeaderRow}>
                  <p>{unit.unitName}</p>
                  <span className={styles.scoreBadge}>일치도 {toPercent(unit.matchScore)}</span>
                </div>
                <small>
                  {unit.branchType} · {unit.regionName}
                </small>
                <small className={styles.matchMealText}>일치 메뉴 {unit.matchCount}개</small>
                <small className={styles.matchMealText}>메뉴 미리보기: {unit.mealPreview || '-'}</small>
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
                <small>
                  {unit.branchType} · {unit.regionName}
                </small>
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
