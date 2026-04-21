import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppLayout from '../components/layout/AppLayout';
import Card from '../components/ui/Card';
import { getTodayMeal } from '../api/mealApi';
import { getUnits, setMyUnit } from '../api/unitApi';
import styles from '../features/design/SetupPage.module.css';

export default function UnitSelectPage() {
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [selectedId, setSelectedId] = useState(null);
  const [units, setUnits] = useState([]);
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    let isMounted = true;

    async function loadInitialData() {
      try {
        const [unitList, todayMeal] = await Promise.all([
          getUnits(),
          getTodayMeal().catch(() => null),
        ]);

        if (!isMounted) return;
        setUnits(unitList ?? []);

        const matchedUnit = (unitList ?? []).find((unit) => unit.unitName === todayMeal?.unitName);
        if (matchedUnit?.id) {
          setSelectedId(matchedUnit.id);
          return;
        }

        if ((unitList ?? []).length > 0) {
          setSelectedId(unitList[0].id);
        }
      } catch (error) {
        if (!isMounted) return;
        setErrorMessage(error.message || '부대 목록을 불러오지 못했습니다.');
      }
    }

    loadInitialData();
    return () => {
      isMounted = false;
    };
  }, []);

  const filtered = useMemo(
    () =>
      units.filter((item) => {
        const branch = item.branchType ?? '';
        const region = item.regionName ?? '';
        const name = item.unitName ?? '';
        return name.includes(query) || branch.includes(query) || region.includes(query);
      }),
    [query, units]
  );

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
      <input className={styles.search} placeholder="부대명/지역 검색" value={query} onChange={(e) => setQuery(e.target.value)} />
      <div className={styles.chips}><span className={styles.activeChip}>최근 선택</span><span>추천</span><span>육군</span><span>해병</span></div>
      {filtered.map((unit) => (
        <Card key={unit.id} className={`${styles.selectCard} ${selectedId === unit.id ? styles.selected : ''}`}>
          <button type="button" onClick={() => setSelectedId(unit.id)}>
            <p>{unit.unitName}</p>
            <small>{unit.branchType} · {unit.regionName}</small>
          </button>
        </Card>
      ))}
      {errorMessage ? <p>{errorMessage}</p> : null}
      <button type="button" className={styles.primary} onClick={handleSelectUnit} disabled={!selectedId || submitting}>
        {submitting ? '저장 중...' : '이 식단으로 부대 선택'}
      </button>
    </AppLayout>
  );
}
