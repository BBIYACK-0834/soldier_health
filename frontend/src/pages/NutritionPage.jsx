import { useState } from 'react';
import AppLayout from '../components/layout/AppLayout';
import Card from '../components/ui/Card';
import ProgressBar from '../components/ui/ProgressBar';
import MacroBox from '../components/ui/MacroBox';
import styles from '../features/design/DietPage.module.css';

const baseMeals = {
  breakfast: ['쌀밥', '쇠고기미역국', '계란말이', '김치'],
  lunch: ['잡곡밥', '닭가슴살 샐러드', '오이무침', '깍두기'],
  dinner: ['쌀밥', '두부 샐러드', '방울토마토', '김치'],
};

const mealOrder = [
  ['breakfast', '아침 07:30 · 360 kcal'],
  ['lunch', '점심 12:10 · 650 kcal'],
  ['dinner', '저녁 18:00 · 510 kcal'],
];

export default function NutritionPage() {
  const [extras, setExtras] = useState({ breakfast: ['프로틴바 1개 (120 kcal)'], lunch: [], dinner: [], snack: ['견과류 1봉 (150 kcal)'] });

  const addFood = (key) => {
    const value = window.prompt('추가 음식 입력');
    if (!value) return;
    setExtras((prev) => ({ ...prev, [key]: [...prev[key], value] }));
  };

  const removeFood = (key, idx) => setExtras((prev) => ({ ...prev, [key]: prev[key].filter((_, i) => i !== idx) }));

  return (
    <AppLayout title="식단 기록" subtitle="부대 기본 식단 + 추가 음식">
      <Card>
        <p className={styles.total}>총 섭취 칼로리 <strong>1,520 kcal</strong></p>
        <div className={styles.macroGrid}>
          <MacroBox label="탄수화물" intake={180} target={300} color="#50739a" tone="#dfe5ef" />
          <MacroBox label="단백질" intake={88} target={120} color="#6f8f55" tone="#e4e9de" />
          <MacroBox label="지방" intake={38} target={60} color="#d28a2c" tone="#efe2cf" />
        </div>
        <ProgressBar value={1520} max={2000} />
      </Card>

      {mealOrder.map(([key, title]) => (
        <Card key={key}>
          <div className={styles.row}><h3>{title}</h3><button type="button" onClick={() => addFood(key)}>+ 추가</button></div>
          <p className={styles.fixed}>고정 식단: {baseMeals[key].join(', ')}</p>
          <div className={styles.list}>
            {extras[key].map((item, idx) => (
              <div key={`${item}-${idx}`} className={styles.item}><span>{item}</span><button type="button" onClick={() => removeFood(key, idx)}>×</button></div>
            ))}
          </div>
        </Card>
      ))}

      <Card>
        <div className={styles.row}><h3>간식</h3><button type="button" onClick={() => addFood('snack')}>+ 간식 추가</button></div>
        {extras.snack.map((item, idx) => (
          <div key={`${item}-${idx}`} className={styles.item}><span>{item}</span><button type="button" onClick={() => removeFood('snack', idx)}>×</button></div>
        ))}
      </Card>

      <button type="button" className={styles.save}>오늘 식단 저장</button>
    </AppLayout>
  );
}
