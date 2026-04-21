import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppLayout from '../components/layout/AppLayout';
import styles from '../features/design/SetupPage.module.css';

const categories = {
  유산소: ['러닝머신', '사이클', '스텝밀'],
  '근력·상체': ['벤치프레스', '스미스머신', '렛풀다운'],
  기타: ['덤벨', '케이블머신', '풀업바'],
};

export default function EquipmentSelectPage() {
  const navigate = useNavigate();
  const [selected, setSelected] = useState(['러닝머신', '스미스머신', '덤벨']);
  const count = useMemo(() => selected.length, [selected]);

  const toggle = (name) => setSelected((prev) => (prev.includes(name) ? prev.filter((item) => item !== name) : [...prev, name]));

  return (
    <AppLayout title="헬스장 기구 선택" subtitle="이용 가능한 운동 기구를 선택해주세요." showBottomNav={false}>
      {Object.entries(categories).map(([category, items]) => (
        <section key={category} className={styles.categorySection}>
          <h3>{category}</h3>
          <div className={styles.equipmentGrid}>
            {items.map((item) => (
              <button key={item} type="button" className={`${styles.eqBtn} ${selected.includes(item) ? styles.eqSelected : ''}`} onClick={() => toggle(item)}>{item}</button>
            ))}
          </div>
        </section>
      ))}
      <div className={styles.bottomInfo}>선택된 기구 <strong>{count}/12</strong><p>{selected.join(' · ')}</p></div>
      <button type="button" className={styles.primary} onClick={() => navigate('/setup/profile')}>선택 완료</button>
    </AppLayout>
  );
}
