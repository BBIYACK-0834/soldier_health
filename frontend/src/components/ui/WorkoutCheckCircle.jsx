import styles from './WorkoutCheckCircle.module.css';

export default function WorkoutCheckCircle({ checked, label }) {
  return (
    <div className={styles.wrap}>
      <div className={`${styles.circle} ${checked ? styles.checked : ''}`}>{checked ? '✓' : ''}</div>
      <span>{label}</span>
    </div>
  );
}
