import { NavLink } from 'react-router-dom';
import styles from './MobileShell.module.css';

const defaultTabs = [
  { to: '/', label: '홈' },
  { to: '/workout', label: '운동' },
  { to: '/nutrition', label: '식단' },
  { to: '/community', label: '커뮤니티' },
  { to: '/profile', label: '프로필' },
];

export default function MobileShell({ title, children, actions, tabs = defaultTabs }) {
  return (
    <div className={styles.shell}>
      <header className={styles.header}>
        <h1>{title}</h1>
        {actions ? <div className={styles.actions}>{actions}</div> : null}
      </header>
      <main className={styles.main}>{children}</main>
      {tabs?.length ? (
        <nav className={styles.bottomNav}>
          {tabs.map((tab) => (
            <NavLink
              key={tab.to}
              to={tab.to}
              className={({ isActive }) => `${styles.tabItem} ${isActive ? styles.activeTab : ''}`}
            >
              {tab.label}
            </NavLink>
          ))}
        </nav>
      ) : null}
    </div>
  );
}
