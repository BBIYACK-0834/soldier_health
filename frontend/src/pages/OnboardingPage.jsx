import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import MobileShell from '../components/layout/MobileShell';
import styles from '../features/onboarding/OnboardingForm.module.css';
import { getUnits, setMyUnit } from '../api/unitApi';
import { getEquipments, saveMyEquipments } from '../api/equipmentApi';
import { updateGoals, updateProfile } from '../api/userApi';
import {
  GOAL_TYPE_LABELS,
  WORKOUT_LEVEL_LABELS,
  WEEKDAY_LABELS,
  toLabel,
} from '../constants/labels';

const weekDays = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'];

export default function OnboardingPage() {
  const navigate = useNavigate();
  const [units, setUnits] = useState([]);
  const [equipments, setEquipments] = useState([]);
  const [selectedDays, setSelectedDays] = useState(['MON', 'WED', 'FRI']);
  const [selectedEquipmentIds, setSelectedEquipmentIds] = useState([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [loadingUnit, setLoadingUnit] = useState(true);
  const [unitLoadError, setUnitLoadError] = useState('');
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
          setForm((prev) => ({ ...prev, unitId: unitRows[0].id }));
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

  const onSubmit = async (e) => {
    e.preventDefault();

    if (!form.heightCm || !form.weightKg || !form.unitId) return;

    setIsSubmitting(true);
    try {
      await updateProfile({
        heightCm: Number(form.heightCm),
        weightKg: Number(form.weightKg),
      });
      await updateGoals({
        goalType: form.goalType,
        workoutLevel: form.workoutLevel,
        branchType: units.find((unit) => Number(unit.id) === Number(form.unitId))?.branchType || 'ARMY',
        workoutDaysPerWeek: Number(form.workoutDaysPerWeek),
        preferredWorkoutMinutes: Number(form.preferredWorkoutMinutes),
        workoutAvailableDays: selectedDays,
      });
      await setMyUnit(Number(form.unitId));
      await saveMyEquipments({ equipmentIds: selectedEquipmentIds, customEquipmentNames: [] });
      navigate('/', { replace: true });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <MobileShell title="초기 설정">
      <form className={styles.section} onSubmit={onSubmit}>
        <h2 className={styles.title}>1) 신체 정보</h2>
        <div className={styles.row}>
          <input
            className={styles.input}
            type="number"
            value={form.heightCm}
            onChange={(e) => setForm((prev) => ({ ...prev, heightCm: e.target.value }))}
            placeholder="키(cm)"
          />
          <input
            className={styles.input}
            type="number"
            value={form.weightKg}
            onChange={(e) => setForm((prev) => ({ ...prev, weightKg: e.target.value }))}
            placeholder="몸무게(kg)"
          />
        </div>

        <h2 className={styles.title}>2) 운동 설정</h2>
        <div className={styles.row}>
          <select
            className={styles.select}
            value={form.goalType}
            onChange={(e) => setForm((prev) => ({ ...prev, goalType: e.target.value }))}
          >
            {goalOptions.map((goal) => (
              <option key={goal} value={goal}>{toLabel(GOAL_TYPE_LABELS, goal)}</option>
            ))}
          </select>
          <select
            className={styles.select}
            value={form.workoutLevel}
            onChange={(e) => setForm((prev) => ({ ...prev, workoutLevel: e.target.value }))}
          >
            {levelOptions.map((level) => (
              <option key={level} value={level}>{toLabel(WORKOUT_LEVEL_LABELS, level)}</option>
            ))}
          </select>
        </div>

        <div className={styles.row}>
          <input
            className={styles.input}
            type="number"
            min="1"
            max="7"
            value={form.workoutDaysPerWeek}
            onChange={(e) => setForm((prev) => ({ ...prev, workoutDaysPerWeek: e.target.value }))}
            placeholder="주당 운동 횟수"
          />
          <input
            className={styles.input}
            type="number"
            min="10"
            max="180"
            value={form.preferredWorkoutMinutes}
            onChange={(e) => setForm((prev) => ({ ...prev, preferredWorkoutMinutes: e.target.value }))}
            placeholder="운동 시간(분)"
          />
        </div>

        <h2 className={styles.title}>운동 가능 요일</h2>
        <div className={styles.chips}>
          {weekDays.map((day) => (
            <button
              key={day}
              type="button"
              className={`${styles.chip} ${selectedDays.includes(day) ? styles.active : ''}`}
              onClick={() => toggleDay(day)}
            >
              {toLabel(WEEKDAY_LABELS, day)}
            </button>
          ))}
        </div>

        <h2 className={styles.title}>3) 부대 선택</h2>
        {loadingUnit ? <p className={styles.info}>부대 목록을 불러오는 중입니다.</p> : null}
        {unitLoadError ? <p className={styles.error}>{unitLoadError}</p> : null}
        <select
          className={styles.select}
          value={form.unitId}
          onChange={(e) => setForm((prev) => ({ ...prev, unitId: e.target.value }))}
          disabled={loadingUnit || units.length === 0}
        >
          {units.length === 0 ? <option value="">선택 가능한 부대가 없습니다.</option> : null}
          {units.map((unit) => (
            <option value={unit.id} key={unit.id}>
              {unit.unitName} ({unit.regionName || '지역 미상'})
            </option>
          ))}
        </select>

        <h2 className={styles.title}>4) 보유 기구 선택</h2>
        <div className={styles.chips}>
          {equipments.map((eq) => (
            <button
              key={eq.id}
              type="button"
              className={`${styles.chip} ${selectedEquipmentIds.includes(eq.id) ? styles.active : ''}`}
              onClick={() => toggleEquipment(eq.id)}
            >
              {eq.name}
            </button>
          ))}
        </div>

        <button className={styles.submit} type="submit" disabled={isSubmitting || loadingUnit || units.length === 0}>
          {isSubmitting ? '저장 중입니다...' : '초기 설정 저장 후 홈으로 이동'}
        </button>
      </form>
    </MobileShell>
  );
}
