import BottomNav from '../ui/BottomNav';
import styles from './AppLayout.module.css';

export default function AppLayout({ title, subtitle, children, showBottomNav = true, headerAction }) {
  return (
    <div className={styles.viewport}>
      <div className={styles.phone}>
        {(title || subtitle || headerAction) && (
          <header className={styles.header}>
            <div>
              {title ? <h1>{title}</h1> : null}
              {subtitle ? <p>{subtitle}</p> : null}
            </div>
            {headerAction ? <div>{headerAction}</div> : null}
          </header>
        )}
        <main className={styles.content}>{children}</main>
        {showBottomNav ? <BottomNav /> : null}
      </div>
    </div>
  );
}
