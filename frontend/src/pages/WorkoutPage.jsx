import { useEffect, useMemo, useState } from 'react';
import MobileShell from '../components/layout/MobileShell';
import { getTodayWorkoutRecommendation } from '../api/workoutApi';
import { getMyAlarms } from '../api/alarmApi';
import { WEEKDAY_LABELS, toLabel } from '../constants/labels';
import styles from '../features/home/HomeCards.module.css';

function formatRepeatDays(raw) {
  if (!raw) return '반복 없음';
  return raw
    .split(',')
    .map((day) => toLabel(WEEKDAY_LABELS, day.trim(), day.trim()))
    .join(', ');
}

export default function WorkoutPage() {
  const [workout, setWorkout] = useState(null);
  const [nextAlarm, setNextAlarm] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [completedExercises, setCompletedExercises] = useState({});

  useEffect(() => {
    (async () => {
      const [workoutData, alarms] = await Promise.all([
        getTodayWorkoutRecommendation().catch(() => null),
        getMyAlarms().catch(() => []),
      ]);
      setWorkout(workoutData);
      const enabled = (alarms || []).filter((alarm) => alarm.enabled);
      setNextAlarm(enabled.length ? enabled[0] : null);
      setIsLoading(false);
    })();
  }, []);

  const exercises = workout?.exercises || [];

  const completedCount = useMemo(
    () => exercises.filter((exercise) => completedExercises[exercise.name]).length,
    [completedExercises, exercises]
  );

  const progressPercent = exercises.length ? Math.round((completedCount / exercises.length) * 100) : 0;

  const toggleExerciseDone = (name) => {
    setCompletedExercises((prev) => ({ ...prev, [name]: !prev[name] }));
  };

  return (
    <MobileShell title="운동">
      <section className={styles.card}>
        <div className={styles.sectionHead}>
          <div>
            <h3>오늘 루틴</h3>
            <p className={styles.subtitle}>추천 루틴을 진행해보세요.</p>
          </div>
        </div>
        {isLoading ? <p className={styles.muted}>운동 데이터를 불러오는 중입니다...</p> : null}
        {!isLoading && !workout ? <p className={styles.muted}>오늘 운동 데이터가 없습니다.</p> : null}
        {workout ? (
          <>
            <p className={styles.item}><span className={styles.label}>루틴</span>{workout.todayFocus || workout.routineType || '루틴 정보 없음'}</p>
            <p className={styles.item}><span className={styles.label}>진행률</span><span className={styles.strongNumber}>{progressPercent}%</span> ({completedCount}/{exercises.length || 0})</p>
            <p className={styles.subText}>{nextAlarm ? `다음 알람 ${String(nextAlarm.hour).padStart(2, '0')}:${String(nextAlarm.minute).padStart(2, '0')} · ${formatRepeatDays(nextAlarm.repeatDaysJson)}` : '설정된 운동 알람이 없습니다'}</p>
            <div className={styles.exerciseList}>
              {exercises.map((exercise, idx) => {
                const done = Boolean(completedExercises[exercise.name]);
                return (
                  <button
                    type="button"
                    key={`${exercise.name}-${idx}`}
                    className={`${styles.exerciseRow} ${done ? styles.exerciseDone : ''}`}
                    onClick={() => toggleExerciseDone(exercise.name)}
                  >
                    <span>{exercise.name} · {exercise.sets}세트</span>
                    <span>{done ? '완료' : '미완료'}</span>
                  </button>
                );
              })}
            </div>
          </>
        ) : null}
      </section>
    </MobileShell>
  );
}
