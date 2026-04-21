import ProgressBar from './ProgressBar';
import styles from './MacroBox.module.css';

export default function MacroBox({ label, intake, target, color, tone }) {
  return (
    <div className={styles.box} style={{ background: tone }}>
      <p className={styles.label} style={{ color }}>{label}</p>
      <p className={styles.value}>{intake}g / {target}g</p>
      <ProgressBar value={intake} max={target} color={color} />
    </div>
  );
}
