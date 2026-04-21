import { NavLink } from 'react-router-dom';
import styles from './BottomNav.module.css';

const tabs = [
  { to: '/home', label: '홈' },
  { to: '/diet', label: '식단' },
  { to: '/workout', label: '운동' },
  { to: '/community', label: '커뮤니티' },
  { to: '/mypage', label: '마이' },
];

export default function BottomNav() {
  return (
    <nav className={styles.nav}>
      {tabs.map((tab) => (
        <NavLink key={tab.to} to={tab.to} className={({ isActive }) => `${styles.item} ${isActive ? styles.active : ''}`}>
          {tab.label}
        </NavLink>
      ))}
    </nav>
  );
}
