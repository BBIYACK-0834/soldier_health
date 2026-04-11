import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import MobileShell from '../components/layout/MobileShell';
import styles from '../features/onboarding/OnboardingForm.module.css';
import { getUnits, setMyUnit } from '../api/unitApi';
import { getEquipments, saveMyEquipments } from '../api/equipmentApi';
import { updateGoals, updateProfile } from '../api/userApi';

const weekDays = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'];

export default function OnboardingPage() {
  const navigate = useNavigate();
  const [units, setUnits] = useState([]);
  const [equipments, setEquipments] = useState([]);
  const [selectedDays, setSelectedDays] = useState(['MON', 'WED', 'FRI']);
  const [selectedEquipmentIds, setSelectedEquipmentIds] = useState([]);
  const [form, setForm] = useState({
    nickname: '',
    heightCm: 175,
    weightKg: 70,
    goalType: 'GENERAL_FITNESS',
    workoutLevel: 'BEGINNER',
    branchType: 'ARMY',
    workoutDaysPerWeek: 3,
    preferredWorkoutMinutes: 50,
    unitId: '',
  });

  useEffect(() => {
    (async () => {
      const [unitRows, equipmentRows] = await Promise.all([getUnits(), getEquipments()]);
      setUnits(unitRows || []);
      setEquipments(equipmentRows || []);
      if (unitRows?.length) setForm((prev) => ({ ...prev, unitId: unitRows[0].id }));
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
    await updateProfile({ nickname: form.nickname || '특급전사', heightCm: Number(form.heightCm), weightKg: Number(form.weightKg) });
    await updateGoals({
      goalType: form.goalType,
      workoutLevel: form.workoutLevel,
      branchType: form.branchType,
      workoutDaysPerWeek: Number(form.workoutDaysPerWeek),
      preferredWorkoutMinutes: Number(form.preferredWorkoutMinutes),
      workoutAvailableDays: selectedDays,
    });
    if (form.unitId) {
      await setMyUnit(Number(form.unitId));
    }
    await saveMyEquipments({ equipmentIds: selectedEquipmentIds, customEquipmentNames: [] });
    navigate('/');
  };

  return (
    <MobileShell title="초기 설정">
      <form className={styles.section} onSubmit={onSubmit}>
        <h2 className={styles.title}>기본 정보</h2>
        <div className={styles.row}>
          <input
            className={styles.input}
            placeholder="닉네임"
            value={form.nickname}
            onChange={(e) => setForm((prev) => ({ ...prev, nickname: e.target.value }))}
          />
          <select
            className={styles.select}
            value={form.branchType}
            onChange={(e) => setForm((prev) => ({ ...prev, branchType: e.target.value }))}
          >
            <option value="ARMY">ARMY</option>
            <option value="NAVY">NAVY</option>
            <option value="AIR_FORCE">AIR_FORCE</option>
            <option value="MARINES">MARINES</option>
            <option value="ETC">ETC</option>
          </select>
        </div>

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

        <h2 className={styles.title}>목표/운동 설정</h2>
        <div className={styles.row}>
          <select
            className={styles.select}
            value={form.goalType}
            onChange={(e) => setForm((prev) => ({ ...prev, goalType: e.target.value }))}
          >
            <option value="BULK">BULK</option>
            <option value="CUT">CUT</option>
            <option value="MAINTAIN">MAINTAIN</option>
            <option value="FITNESS_TEST">FITNESS_TEST</option>
            <option value="GENERAL_FITNESS">GENERAL_FITNESS</option>
          </select>
          <select
            className={styles.select}
            value={form.workoutLevel}
            onChange={(e) => setForm((prev) => ({ ...prev, workoutLevel: e.target.value }))}
          >
            <option value="BEGINNER">BEGINNER</option>
            <option value="NOVICE">NOVICE</option>
            <option value="INTERMEDIATE">INTERMEDIATE</option>
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
            placeholder="주 운동 횟수"
          />
          <input
            className={styles.input}
            type="number"
            min="10"
            max="180"
            value={form.preferredWorkoutMinutes}
            onChange={(e) => setForm((prev) => ({ ...prev, preferredWorkoutMinutes: e.target.value }))}
            placeholder="운동 가능 시간(분)"
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
              {day}
            </button>
          ))}
        </div>

        <h2 className={styles.title}>부대 선택</h2>
        <select
          className={styles.select}
          value={form.unitId}
          onChange={(e) => setForm((prev) => ({ ...prev, unitId: e.target.value }))}
        >
          {units.map((unit) => (
            <option value={unit.id} key={unit.id}>
              {unit.unitName} ({unit.regionName || '미상'})
            </option>
          ))}
        </select>

        <h2 className={styles.title}>보유 기구 선택</h2>
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

        <button className={styles.submit} type="submit">
          설정 저장 후 홈으로 이동
        </button>
      </form>
    </MobileShell>
  );
}
