import styles from './TabSwitcher.module.css';

export default function TabSwitcher({ tabs, value, onChange }) {
  return (
    <div className={styles.wrap}>
      {tabs.map((tab) => (
        <button
          key={tab.value}
          type="button"
          className={`${styles.tab} ${value === tab.value ? styles.active : ''}`}
          onClick={() => onChange(tab.value)}
        >
          {tab.label}
        </button>
      ))}
    </div>
  );
}
