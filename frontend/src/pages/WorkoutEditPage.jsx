import { useEffect, useMemo, useState } from 'react';
import AppLayout from '../components/layout/AppLayout';
import Card from '../components/ui/Card';
import TabSwitcher from '../components/ui/TabSwitcher';
import { applyGymDataset, createUnitGymDataset, getEquipments, getMyEquipments, getUnitGymDatasets, saveMyEquipments } from '../api/equipmentApi';
import { getMyUnit } from '../api/unitApi';
import styles from '../features/design/WorkoutEditPage.module.css';

export default function WorkoutEditPage() {
  const [tab, setTab] = useState('equipment');
  const [equipments, setEquipments] = useState([]);
  const [datasets, setDatasets] = useState([]);
  const [selected, setSelected] = useState([]);
  const [unitId, setUnitId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    let mounted = true;
    async function load() {
      try {
        setLoading(true);
        const [allList, myList, myUnit] = await Promise.all([getEquipments(), getMyEquipments(), getMyUnit()]);
        if (!mounted) return;
        setEquipments(allList ?? []);
        setSelected((myList ?? []).map((item) => item.name));
        const id = myUnit?.id ?? null;
        setUnitId(id);
        if (id) {
          const list = await getUnitGymDatasets(id);
          if (!mounted) return;
          setDatasets(list ?? []);
        }
      } catch (error) {
        if (!mounted) return;
        setErrorMessage(error.message || '운동 설정 데이터를 불러오지 못했습니다.');
      } finally {
        if (mounted) setLoading(false);
      }
    }

    load();
    return () => {
      mounted = false;
    };
  }, []);

  const selectedCount = useMemo(() => selected.length, [selected]);
  const toggleEquipment = (name) => setSelected((prev) => (prev.includes(name) ? prev.filter((item) => item !== name) : [...prev, name]));

  const save = async () => {
    const selectedIds = equipments.filter((item) => selected.includes(item.name)).map((item) => item.id);
    await saveMyEquipments({ equipmentIds: selectedIds, customEquipmentNames: [] });
  };

  const saveDataset = async () => {
    if (!unitId) return;
    const selectedIds = equipments.filter((item) => selected.includes(item.name)).map((item) => item.id);
    await createUnitGymDataset(unitId, {
      datasetName: '내 데이터셋',
      description: '',
      equipmentIds: selectedIds,
      customEquipmentNames: [],
    });
    const list = await getUnitGymDatasets(unitId);
    setDatasets(list ?? []);
  };

  return (
    <AppLayout title="운동 기구 설정" showBottomNav={false}>
      <TabSwitcher tabs={[{ value: 'equipment', label: '기구 선택' }, { value: 'dataset', label: '부대 데이터셋' }]} value={tab} onChange={setTab} />
      {tab === 'equipment' && (
        <>
          <Card>
            <p className={styles.muted}>현재 선택된 기구 {selectedCount}</p>
            {loading ? <p>불러오는 중...</p> : null}
            {!loading && equipments.length === 0 ? <p>등록된 기구 데이터가 없습니다.</p> : null}
            <div className={styles.grid}>
              {equipments.map((eq) => (
                <button type="button" key={eq.id} className={`${styles.eq} ${selected.includes(eq.name) ? styles.active : ''}`} onClick={() => toggleEquipment(eq.name)}>{eq.name}</button>
              ))}
            </div>
          </Card>
          <div className={styles.selected}>선택된 기구 <strong>{selectedCount}</strong><p>{selected.length > 0 ? selected.join(' · ') : '선택 안 함'}</p></div>
          <button type="button" className={styles.save} onClick={save} disabled={equipments.length === 0}>변경 사항 저장</button>
        </>
      )}
      {tab === 'dataset' && (
        <div className={styles.datasetList}>
          {!unitId ? <Card><p>선택된 부대가 없어 데이터셋을 불러올 수 없습니다.</p></Card> : null}
          {unitId && datasets.length === 0 ? <Card><p>등록된 부대 데이터셋이 없습니다.</p></Card> : null}
          {datasets.map((dataset) => (
            <Card key={dataset.id}>
              <h3>{dataset.datasetName}</h3>
              <p className={styles.meta}>{dataset.description || '설명 없음'}</p>
              <p>{[...(dataset.equipments ?? []).map((item) => item.name), ...(dataset.customEquipmentNames ?? [])].join(' · ') || '기구 정보 없음'}</p>
              <button type="button" className={styles.loadBtn} onClick={() => applyGymDataset(dataset.id)}>이 데이터셋 불러오기</button>
            </Card>
          ))}
          <button type="button" className={styles.save} onClick={saveDataset} disabled={!unitId}>내 부대 데이터셋 저장</button>
        </div>
      )}
      {errorMessage ? <p>{errorMessage}</p> : null}
    </AppLayout>
  );
}
