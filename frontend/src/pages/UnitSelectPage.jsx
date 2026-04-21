import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppLayout from '../components/layout/AppLayout';
import Card from '../components/ui/Card';
import styles from '../features/design/SetupPage.module.css';

const units = [
  { id: 'u1', branch: '육군', region: '경기', name: '제1보병사단 · 보병대대' },
  { id: 'u2', branch: '육군', region: '강원', name: '제7기동군단 · 포병대대' },
  { id: 'u3', branch: '해병', region: '김포', name: '해병대 2사단' },
  { id: 'u4', branch: '공군', region: '청주', name: '17전투비행단' },
];

export default function UnitSelectPage() {
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [selectedId, setSelectedId] = useState('u1');

  const filtered = units.filter((item) => item.name.includes(query) || item.branch.includes(query) || item.region.includes(query));

  return (
    <AppLayout title="부대 식단 선택" subtitle="소속 부대를 선택해주세요." showBottomNav={false}>
      <input className={styles.search} placeholder="부대명/지역 검색" value={query} onChange={(e) => setQuery(e.target.value)} />
      <div className={styles.chips}><span className={styles.activeChip}>최근 선택</span><span>추천</span><span>육군</span><span>해병</span></div>
      {filtered.map((unit) => (
        <Card key={unit.id} className={`${styles.selectCard} ${selectedId === unit.id ? styles.selected : ''}`}>
          <button type="button" onClick={() => setSelectedId(unit.id)}>
            <p>{unit.name}</p>
            <small>{unit.branch} · {unit.region}</small>
          </button>
        </Card>
      ))}
      <button type="button" className={styles.primary} onClick={() => navigate('/setup/equipment')}>이 식단으로 부대 선택</button>
    </AppLayout>
  );
}
