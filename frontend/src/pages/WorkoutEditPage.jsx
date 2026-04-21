import { useMemo, useState } from 'react';
import AppLayout from '../components/layout/AppLayout';
import Card from '../components/ui/Card';
import TabSwitcher from '../components/ui/TabSwitcher';
import styles from '../features/design/WorkoutEditPage.module.css';

const allEquipment = ['러닝머신', '사이클', '스텝밀', '로잉머신', '스미스머신', '벤치프레스', '렛풀다운', '덤벨', '케이블머신', '숄더프레스'];
const unitDatasets = [
  { name: '제1보병사단·수색대대', users: 128, tag: '인기', equipments: ['러닝머신', '스미스머신', '덤벨'] },
  { name: '제7기동군단·포병대대', users: 96, tag: '최신', equipments: ['사이클', '벤치프레스', '케이블머신'] },
  { name: '해병대 2사단', users: 78, tag: '공유', equipments: ['렛풀다운', '덤벨', '로잉머신'] },
];

export default function WorkoutEditPage() {
  const [tab, setTab] = useState('equipment');
  const [selected, setSelected] = useState(['러닝머신', '스미스머신', '덤벨']);

  const selectedCount = useMemo(() => selected.length, [selected]);
  const toggleEquipment = (name) => setSelected((prev) => (prev.includes(name) ? prev.filter((item) => item !== name) : [...prev, name]));

  return (
    <AppLayout title="운동 기구 설정" showBottomNav={false}>
      <TabSwitcher tabs={[{ value: 'equipment', label: '기구 선택' }, { value: 'dataset', label: '부대 데이터셋' }]} value={tab} onChange={setTab} />
      {tab === 'equipment' && (
        <>
          <Card>
            <input className={styles.search} placeholder="기구 이름 검색" />
            <p className={styles.muted}>현재 선택된 기구 {selectedCount}/12</p>
            <div className={styles.grid}>
              {allEquipment.map((eq) => (
                <button type="button" key={eq} className={`${styles.eq} ${selected.includes(eq) ? styles.active : ''}`} onClick={() => toggleEquipment(eq)}>{eq}</button>
              ))}
            </div>
          </Card>
          <div className={styles.selected}>선택된 기구 <strong>{selectedCount}/12</strong><p>{selected.join(' · ')}</p></div>
          <button type="button" className={styles.save}>변경 사항 저장</button>
        </>
      )}
      {tab === 'dataset' && (
        <div className={styles.datasetList}>
          {unitDatasets.map((dataset) => (
            <Card key={dataset.name}>
              <p className={styles.tag}>{dataset.tag}</p>
              <h3>{dataset.name}</h3>
              <p className={styles.meta}>사용자 {dataset.users}명</p>
              <p>{dataset.equipments.join(' · ')}</p>
              <button type="button" className={styles.loadBtn} onClick={() => setSelected(dataset.equipments)}>이 데이터셋 불러오기</button>
            </Card>
          ))}
          <button type="button" className={styles.save}>내 부대 데이터셋 저장</button>
        </div>
      )}
    </AppLayout>
  );
}
