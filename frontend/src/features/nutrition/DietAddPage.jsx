import { useMemo, useState } from 'react';
import AppLayout from '../../components/layout/AppLayout';
import Card from '../../components/ui/Card';
import TabSwitcher from '../../components/ui/TabSwitcher';
import { mockFoods } from '../../constants/mockData';
import styles from './DietPage.module.css';
import screen from '../../components/ui/Screen.module.css';

const tabs = [
  { value: 'recent', label: '최근' },
  { value: 'favorite', label: '즐겨찾기' },
  { value: 'manual', label: '직접 입력' },
];

export default function DietAddPage() {
  const [tab, setTab] = useState('recent');
  const [keyword, setKeyword] = useState('');
  const [selected, setSelected] = useState([]);

  const foods = useMemo(() => {
    if (tab === 'manual') return [];
    return mockFoods.filter((food) => {
      const matchesTab = tab === 'favorite' ? food.isFavorite : true;
      const matchesKeyword = keyword.trim() ? food.foodName.includes(keyword.trim()) : true;
      return matchesTab && matchesKeyword;
    });
  }, [keyword, tab]);

  const toggleFood = (food) => {
    setSelected((prev) => (prev.some((item) => item.id === food.id) ? prev.filter((item) => item.id !== food.id) : [...prev, food]));
  };

  return (
    <AppLayout title="음식 검색" subtitle="최근·즐겨찾기·직접 입력으로 식단을 추가하세요." showBottomNav={false}>
      <input className={screen.input} placeholder="음식명을 입력하세요" value={keyword} onChange={(e) => setKeyword(e.target.value)} />
      <TabSwitcher tabs={tabs} value={tab} onChange={setTab} />

      {tab === 'manual' ? (
        <Card>
          <h3 className={screen.sectionTitle}>직접 입력</h3>
          <div className={screen.list}>
            <input className={screen.input} placeholder="음식명" />
            <input className={screen.input} placeholder="칼로리 kcal" type="number" />
            <input className={screen.input} placeholder="탄수화물 g" type="number" />
            <input className={screen.input} placeholder="단백질 g" type="number" />
            <input className={screen.input} placeholder="지방 g" type="number" />
            <button type="button" className={screen.primaryButton}>직접 입력 음식 추가</button>
          </div>
        </Card>
      ) : (
        <div className={screen.list}>
          {foods.map((food) => (
            <Card key={food.id}>
              <div className={styles.item}>
                <span>{food.foodName} <small>({food.calories} kcal)</small></span>
                <button type="button" onClick={() => toggleFood(food)}>{selected.some((item) => item.id === food.id) ? '✓' : '+'}</button>
              </div>
              <p className={styles.base}>탄 {food.carbg}g · 단 {food.proteing}g · 지 {food.fatg}g · {food.servingUnit}</p>
            </Card>
          ))}
        </div>
      )}

      <button type="button" className={screen.primaryButton}>선택한 음식 {selected.length}개 추가</button>
    </AppLayout>
  );
}
