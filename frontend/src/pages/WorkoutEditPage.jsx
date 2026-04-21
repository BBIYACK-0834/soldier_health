import { useMemo, useState } from 'react';
import AppLayout from '../components/layout/AppLayout';
import Card from '../components/ui/Card';
import TabSwitcher from '../components/ui/TabSwitcher';
import styles from '../features/design/WorkoutEditPage.module.css';

const allEquipment = ['러닝머신', '덤벨', '벤치프레스', '사이클', '스미스머신', '로잉머신', '렛풀다운', '케이블머신'];
const unitDatasets = [
  { name: '제1보병사단 · 수색대대', equipments: ['러닝머신', '스미스머신', '덤벨'] },
  { name: '제7기동군단 · 포병대대', equipments: ['사이클', '벤치프레스', '케이블머신'] },
];

export default function WorkoutEditPage() {
  const [tab, setTab] = useState('equipment');
  const [selected, setSelected] = useState(['러닝머신', '덤벨', '벤치프레스']);

  const selectedCount = useMemo(() => selected.length, [selected]);

  const toggleEquipment = (name) => setSelected((prev) => (prev.includes(name) ? prev.filter((item) => item !== name) : [...prev, name]));

  return (
    <AppLayout title="운동 수정" showBottomNav={false}>
      <TabSwitcher tabs={[{ value: 'equipment', label: '기구 선택' }, { value: 'dataset', label: '부대 데이터셋' }]} value={tab} onChange={setTab} />

      {tab === 'equipment' && (
        <Card>
          <p className={styles.muted}>현재 선택된 기구 {selectedCount}개</p>
          <div className={styles.grid}>
            {allEquipment.map((eq) => (
              <button type="button" key={eq} className={`${styles.eq} ${selected.includes(eq) ? styles.active : ''}`} onClick={() => toggleEquipment(eq)}>{eq}</button>
            ))}
          </div>
        </Card>
      )}

      {tab === 'dataset' && (
        <div className={styles.datasetList}>
          {unitDatasets.map((dataset) => (
            <Card key={dataset.name}>
              <h3>{dataset.name}</h3>
              <p>{dataset.equipments.join(', ')}</p>
              <button type="button" className={styles.loadBtn} onClick={() => setSelected(dataset.equipments)}>이 데이터셋 불러오기</button>
            </Card>
          ))}
        </div>
      )}

      <button type="button" className={styles.save}>변경 사항 저장</button>
    </AppLayout>
  );
}
