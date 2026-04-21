import { NavLink } from 'react-router-dom';
import styles from './BottomNav.module.css';

const tabs = [
  { to: '/home', label: '홈', icon: '🏠' },
  { to: '/diet', label: '식단', icon: '🍽️' },
  { to: '/workout', label: '운동', icon: '🏋️' },
  { to: '/community', label: '커뮤니티', icon: '👥' },
  { to: '/mypage', label: '마이', icon: '👤' },
];

export default function BottomNav() {
  return (
    <nav className={styles.nav}>
      {tabs.map((tab) => (
        <NavLink key={tab.to} to={tab.to} className={({ isActive }) => `${styles.item} ${isActive ? styles.active : ''}`}>
          <span>{tab.icon}</span>
          <small>{tab.label}</small>
        </NavLink>
      ))}
    </nav>
  );
}
