import { useState } from 'react';
import AppLayout from '../../components/layout/AppLayout';
import Card from '../../components/ui/Card';
import { emptyNotificationSettings } from '../../constants/defaultData';
import screen from '../../components/ui/Screen.module.css';
import styles from './MyPage.module.css';

const rows = [
  { key: 'mealReminder', label: '식단 알림', hint: '매 식사 시간 알림' },
  { key: 'exerciseReminder', label: '운동 알림', hint: '운동 시간 알림' },
  { key: 'weightReminder', label: '체중 측정 알림', hint: '주 1회 알림' },
  { key: 'waterReminder', label: '물 섭취 알림', hint: '매 2시간 알림' },
];

export default function NotificationSettingsPage() {
  const [settings, setSettings] = useState(emptyNotificationSettings);
  return (
    <AppLayout title="알림 설정" showBottomNav={false}>
      <Card>
        {rows.map((row) => (
          <div key={row.key} className={styles.toggleRow}>
            <div><strong>{row.label}</strong><p className={screen.muted}>{row.hint}</p></div>
            <button type="button" className={`${styles.toggle} ${settings[row.key] ? styles.toggleOn : ''}`} onClick={() => setSettings((prev) => ({ ...prev, [row.key]: !prev[row.key] }))} aria-label={row.label} />
          </div>
        ))}
      </Card>
      <button type="button" className={screen.primaryButton}>저장하기</button>
    </AppLayout>
  );
}
