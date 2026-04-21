import { useState } from 'react';
import AppLayout from '../components/layout/AppLayout';
import Card from '../components/ui/Card';
import ProgressBar from '../components/ui/ProgressBar';
import MacroBox from '../components/ui/MacroBox';
import styles from '../features/design/DietPage.module.css';

const sections = [
  { key: 'breakfast', label: '☀️ 아침 07:30 · 360 kcal', fixed: '쌀밥, 소고기미역국, 계란말이, 감자채볶음, 김치' },
  { key: 'lunch', label: '☀️ 점심 12:10 · 650 kcal', fixed: '잡곡밥, 닭가슴살 샐러드, 오이무침, 깍두기' },
  { key: 'dinner', label: '🌙 저녁 18:00 · 510 kcal', fixed: '쌀밥, 두부 샐러드, 방울토마토, 김치' },
  { key: 'snack', label: '☕ 간식', fixed: '-' },
];

export default function NutritionPage() {
  const [extras, setExtras] = useState({ breakfast: ['프로틴바 1개 (120 kcal)'], lunch: ['바나나 1개 (93 kcal)'], dinner: ['우유 1팩 (120 kcal)'], snack: ['견과류 1봉 (150 kcal)'] });

  const addFood = (key) => {
    const value = window.prompt('추가 음식 입력');
    if (!value) return;
    setExtras((prev) => ({ ...prev, [key]: [...prev[key], value] }));
  };

  const removeFood = (key, idx) => setExtras((prev) => ({ ...prev, [key]: prev[key].filter((_, i) => i !== idx) }));

  return (
    <AppLayout title="식단 기록" subtitle="2024.06.01 (토)" headerAction={<span className={styles.calendar}>🗓️</span>}>
      <Card>
        <p className={styles.totalTitle}>총 섭취 칼로리</p>
        <p className={styles.totalKcal}>1,520 kcal</p>
        <div className={styles.macroGrid}>
          <MacroBox label="탄수화물" intake={180} target={300} color="#50739a" tone="#dfe5ef" />
          <MacroBox label="단백질" intake={88} target={120} color="#6f8f55" tone="#e4e9de" />
          <MacroBox label="지방" intake={38} target={60} color="#d28a2c" tone="#efe2cf" />
        </div>
        <ProgressBar value={1520} max={2000} />
        <small>부대 식단 기준 2,000 kcal</small>
      </Card>

      <Card>
        <div className={styles.row}><strong>오늘 부대</strong><button type="button">+ 부대 변경</button></div>
        <p className={styles.fixed}>육군 일반식</p>
      </Card>

      {sections.map((section) => (
        <Card key={section.key}>
          <div className={styles.row}><h3>{section.label}</h3><button type="button" onClick={() => addFood(section.key)}>+ {section.key === 'snack' ? '간식' : section.label.slice(2, 4)} 추가</button></div>
          {section.key !== 'snack' ? <p className={styles.base}>부대식: {section.fixed}</p> : null}
          <div className={styles.extraWrap}>
            <p>추가한 음식</p>
            {extras[section.key].map((item, idx) => (
              <div key={`${item}-${idx}`} className={styles.item}><span>{item}</span><button type="button" onClick={() => removeFood(section.key, idx)}>×</button></div>
            ))}
          </div>
        </Card>
      ))}

      <button type="button" className={styles.save}>오늘 식단 저장</button>
    </AppLayout>
  );
}
