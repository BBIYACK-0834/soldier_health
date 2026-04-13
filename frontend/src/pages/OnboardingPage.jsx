import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import MobileShell from '../components/layout/MobileShell';
import styles from '../features/onboarding/OnboardingForm.module.css';
import { getUnits, searchUnits, setMyUnit } from '../api/unitApi';
import {
  applyGymDataset,
  createUnitGymDataset,
  getEquipments,
  getUnitGymDatasets,
  saveMyEquipments,
} from '../api/equipmentApi';
import { updateGoals, updateProfile } from '../api/userApi';
import {
  BRANCH_TYPE_LABELS,
  GOAL_TYPE_LABELS,
  WEEKDAY_LABELS,
  WORKOUT_LEVEL_LABELS,
  toLabel,
} from '../constants/labels';

const weekDays = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'];

export default function OnboardingPage() {
  const navigate = useNavigate();
  const [units, setUnits] = useState([]);
  const [equipments, setEquipments] = useState([]);
  const [selectedDays, setSelectedDays] = useState(['MON', 'WED', 'FRI']);
  const [selectedEquipmentIds, setSelectedEquipmentIds] = useState([]);
  const [customEquipmentText, setCustomEquipmentText] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [loadingUnit, setLoadingUnit] = useState(true);
  const [unitLoadError, setUnitLoadError] = useState('');
  const [unitKeyword, setUnitKeyword] = useState('');
  const [gymDatasets, setGymDatasets] = useState([]);
  const [selectedDatasetId, setSelectedDatasetId] = useState('');
  const [newDatasetName, setNewDatasetName] = useState('');
  const [datasetNotice, setDatasetNotice] = useState('');
  const [form, setForm] = useState({
    heightCm: 175,
    weightKg: 70,
    goalType: 'GENERAL_FITNESS',
    workoutLevel: 'BEGINNER',
    workoutDaysPerWeek: 3,
    preferredWorkoutMinutes: 50,
    unitId: '',
  });

  const goalOptions = useMemo(() => Object.keys(GOAL_TYPE_LABELS), []);
  const levelOptions = useMemo(() => Object.keys(WORKOUT_LEVEL_LABELS), []);

  useEffect(() => {
    (async () => {
      try {
        setLoadingUnit(true);
        const [unitRows, equipmentRows] = await Promise.all([getUnits(), getEquipments()]);
        setUnits(unitRows || []);
        setEquipments(equipmentRows || []);
        if (unitRows?.length) {
          setForm((prev) => ({ ...prev, unitId: String(unitRows[0].id) }));
          setUnitLoadError('');
        } else {
          setUnitLoadError('선택 가능한 부대가 없습니다. 관리자에게 문의해주세요.');
        }
      } catch {
        setUnitLoadError('부대 목록을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.');
      } finally {
        setLoadingUnit(false);
      }
    })();
  }, []);

  useEffect(() => {
    if (loadingUnit) return;

    const timer = setTimeout(async () => {
      try {
        if (!unitKeyword.trim()) {
          const allUnits = await getUnits();
          setUnits(allUnits || []);
          return;
        }

        const searched = await searchUnits(unitKeyword.trim());
        setUnits(searched || []);
      } catch {
        setUnitLoadError('부대 검색에 실패했습니다. 잠시 후 다시 시도해주세요.');
      }
    }, 300);

    return () => clearTimeout(timer);
  }, [loadingUnit, unitKeyword]);

  useEffect(() => {
    if (!units.length) return;
    if (!units.some((unit) => String(unit.id) === String(form.unitId))) {
      setForm((prev) => ({ ...prev, unitId: String(units[0].id) }));
    }
  }, [form.unitId, units]);

  useEffect(() => {
    if (!form.unitId) return;
    (async () => {
      const datasets = await getUnitGymDatasets(Number(form.unitId)).catch(() => []);
      setGymDatasets(datasets || []);
      if ((datasets || []).length) {
        setSelectedDatasetId(String(datasets[0].id));
      }
    })();
  }, [form.unitId]);

  const toggleDay = (day) => {
    setSelectedDays((prev) =>
      prev.includes(day) ? prev.filter((value) => value !== day) : [...prev, day]
    );
  };

  const toggleEquipment = (id) => {
    setSelectedEquipmentIds((prev) =>
      prev.includes(id) ? prev.filter((value) => value !== id) : [...prev, id]
    );
  };

  const handleApplyDataset = async () => {
    if (!selectedDatasetId) return;

    const applied = await applyGymDataset(Number(selectedDatasetId)).catch(() => []);
    const matchingIds = (applied || [])
      .map((item) => item.id)
      .filter((id) => equipments.some((eq) => eq.id === id));
    const customOnly = (applied || [])
      .filter((item) => !equipments.some((eq) => eq.id === item.id))
      .map((item) => item.name);

    setSelectedEquipmentIds(matchingIds);
    setCustomEquipmentText(customOnly.join(', '));
    setDatasetNotice('선택한 부대 헬스장 데이터셋을 내 기구 선택에 반영했습니다.');
  };

  const handleSaveDataset = async () => {
    if (!form.unitId) return;
    const customNames = customEquipmentText
      .split(',')
      .map((v) => v.trim())
      .filter(Boolean);

    await createUnitGymDataset(Number(form.unitId), {
      datasetName: newDatasetName || '이용자 등록 데이터셋',
      description: '이용자가 편집한 부대 헬스장 기구 구성',
      equipmentIds: selectedEquipmentIds,
      customEquipmentNames: customNames,
    });

    const refreshed = await getUnitGymDatasets(Number(form.unitId)).catch(() => []);
    setGymDatasets(refreshed || []);
    setDatasetNotice('현재 선택한 기구로 부대 공용 데이터셋을 저장했습니다.');
    setNewDatasetName('');
  };

  const onSubmit = async (e) => {
    e.preventDefault();

    if (!form.heightCm || !form.weightKg || !form.unitId || selectedDays.length === 0) {
      return;
    }

    setIsSubmitting(true);
    try {
      await updateProfile({
        heightCm: Number(form.heightCm),
        weightKg: Number(form.weightKg),
      });

      const selectedUnit = units.find((unit) => Number(unit.id) === Number(form.unitId));

      await updateGoals({
        goalType: form.goalType,
        workoutLevel: form.workoutLevel,
        branchType: selectedUnit?.branchType || 'ARMY',
        workoutDaysPerWeek: Number(form.workoutDaysPerWeek),
        preferredWorkoutMinutes: Number(form.preferredWorkoutMinutes),
      });

      const customNames = customEquipmentText
        .split(',')
        .map((v) => v.trim())
        .filter(Boolean);

      await setMyUnit(Number(form.unitId));
      await saveMyEquipments({ equipmentIds: selectedEquipmentIds, customEquipmentNames: customNames });
      navigate('/', { replace: true });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <MobileShell title="초기 설정">
      <p className={styles.banner}>지금 설정하면 오늘부터 바로 강한 루틴으로 운동을 시작할 수 있습니다.</p>
      <form className={styles.section} onSubmit={onSubmit}>
        <h2 className={styles.title}>1) 신체 정보</h2>
        <div className={styles.row}>
          <input className={styles.input} type="number" value={form.heightCm} onChange={(e) => setForm((prev) => ({ ...prev, heightCm: e.target.value }))} placeholder="키(cm)" />
          <input className={styles.input} type="number" value={form.weightKg} onChange={(e) => setForm((prev) => ({ ...prev, weightKg: e.target.value }))} placeholder="몸무게(kg)" />
        </div>

        <h2 className={styles.title}>2) 목표 및 난이도</h2>
        <div className={styles.row}>
          <select className={styles.select} value={form.goalType} onChange={(e) => setForm((prev) => ({ ...prev, goalType: e.target.value }))}>
            {goalOptions.map((goal) => (
              <option key={goal} value={goal}>{toLabel(GOAL_TYPE_LABELS, goal)}</option>
            ))}
          </select>
          <select className={styles.select} value={form.workoutLevel} onChange={(e) => setForm((prev) => ({ ...prev, workoutLevel: e.target.value }))}>
            {levelOptions.map((level) => (
              <option key={level} value={level}>{toLabel(WORKOUT_LEVEL_LABELS, level)}</option>
            ))}
          </select>
        </div>

        <div className={styles.row}>
          <input className={styles.input} type="number" min="1" max="7" value={form.workoutDaysPerWeek} onChange={(e) => setForm((prev) => ({ ...prev, workoutDaysPerWeek: e.target.value }))} placeholder="주당 운동 횟수" />
          <input className={styles.input} type="number" min="10" max="180" value={form.preferredWorkoutMinutes} onChange={(e) => setForm((prev) => ({ ...prev, preferredWorkoutMinutes: e.target.value }))} placeholder="운동 시간(분)" />
        </div>

        <h2 className={styles.title}>3) 운동 가능 요일</h2>
        <div className={styles.chips}>
          {weekDays.map((day) => (
            <button key={day} type="button" className={`${styles.chip} ${selectedDays.includes(day) ? styles.active : ''}`} onClick={() => toggleDay(day)}>
              {toLabel(WEEKDAY_LABELS, day)}
            </button>
          ))}
        </div>

        <h2 className={styles.title}>4) 부대 선택</h2>
        {loadingUnit ? <p className={styles.info}>부대 목록을 불러오는 중입니다.</p> : null}
        {unitLoadError ? <p className={styles.error}>{unitLoadError}</p> : null}
        <input className={styles.input} value={unitKeyword} onChange={(e) => setUnitKeyword(e.target.value)} placeholder="부대명 검색 (예: 사단, 여단, 해군, 공군)" />
        <select className={styles.select} value={form.unitId} onChange={(e) => setForm((prev) => ({ ...prev, unitId: e.target.value }))} disabled={loadingUnit || units.length === 0}>
          {units.length === 0 ? <option value="">검색 결과가 없습니다.</option> : null}
          {units.map((unit) => (
            <option value={unit.id} key={unit.id}>
              [{toLabel(BRANCH_TYPE_LABELS, unit.branchType, '기타')}] {unit.unitName}
              {unit.regionName ? ` · ${unit.regionName}` : ''}
            </option>
          ))}
        </select>

        <h2 className={styles.title}>5) 부대별 헬스장 데이터셋</h2>
        <select className={styles.select} value={selectedDatasetId} onChange={(e) => setSelectedDatasetId(e.target.value)} disabled={!gymDatasets.length}>
          {gymDatasets.length === 0 ? <option value="">아직 등록된 데이터셋이 없습니다.</option> : null}
          {gymDatasets.map((dataset) => (
            <option key={dataset.id} value={dataset.id}>
              {dataset.datasetName} ({(dataset.equipments || []).length + (dataset.customEquipmentNames || []).length}개)
            </option>
          ))}
        </select>
        <div className={styles.row}>
          <button className={styles.submit} type="button" onClick={handleApplyDataset} disabled={!selectedDatasetId}>선택 데이터셋 적용</button>
          <button className={styles.submit} type="button" onClick={handleSaveDataset}>현재 선택으로 데이터셋 저장</button>
        </div>
        <input className={styles.input} value={newDatasetName} onChange={(e) => setNewDatasetName(e.target.value)} placeholder="새 데이터셋 이름(예: 1사단 실내체력단련장)" />
        {datasetNotice ? <p className={styles.info}>{datasetNotice}</p> : null}

        <h2 className={styles.title}>6) 보유 기구 선택</h2>
        <div className={styles.chips}>
          {equipments.map((eq) => (
            <button key={eq.id} type="button" className={`${styles.chip} ${selectedEquipmentIds.includes(eq.id) ? styles.active : ''}`} onClick={() => toggleEquipment(eq.id)}>
              {eq.name}
            </button>
          ))}
        </div>
        <input className={styles.input} value={customEquipmentText} onChange={(e) => setCustomEquipmentText(e.target.value)} placeholder="기타 기구 직접 입력 (쉼표로 구분)" />

        <button className={styles.submit} type="submit" disabled={isSubmitting || loadingUnit || units.length === 0}>
          {isSubmitting ? '저장 중입니다...' : '초기 설정 저장 후 홈으로 이동'}
        </button>
      </form>
    </MobileShell>
  );
}
