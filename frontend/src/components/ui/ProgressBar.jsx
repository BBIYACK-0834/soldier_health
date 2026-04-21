import styles from './ProgressBar.module.css';

export default function ProgressBar({ value, max, color = '#536739' }) {
  const safeMax = Math.max(1, max || 1);
  const percent = Math.min(100, Math.max(0, Math.round((value / safeMax) * 100)));
  return (
    <div className={styles.track}>
      <div className={styles.fill} style={{ width: `${percent}%`, background: color }} />
    </div>
  );
}
