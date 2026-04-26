import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppLayout from '../components/layout/AppLayout';
import { getEquipments, getMyEquipments, saveMyEquipments } from '../api/equipmentApi';
import styles from '../features/design/SetupPage.module.css';

export default function EquipmentSelectPage() {
  const navigate = useNavigate();
  const [equipments, setEquipments] = useState([]);
  const [selected, setSelected] = useState([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    let mounted = true;
    async function load() {
      try {
        setLoading(true);
        const [allList, myList] = await Promise.all([getEquipments(), getMyEquipments()]);
        if (!mounted) return;
        setEquipments(allList ?? []);
        setSelected((myList ?? []).map((item) => item.name));
      } catch (error) {
        if (!mounted) return;
        setErrorMessage(error.message || '기구 목록을 불러오지 못했습니다.');
      } finally {
        if (mounted) setLoading(false);
      }
    }

    load();
    return () => {
      mounted = false;
    };
  }, []);

  const categories = useMemo(() => {
    const grouped = {};
    for (const item of equipments) {
      const category = item.category || '기타';
      grouped[category] = [...(grouped[category] || []), item];
    }
    return grouped;
  }, [equipments]);

  const toggle = (name) => setSelected((prev) => (prev.includes(name) ? prev.filter((item) => item !== name) : [...prev, name]));

  const save = async () => {
    const selectedIds = equipments.filter((item) => selected.includes(item.name)).map((item) => item.id);
    await saveMyEquipments({ equipmentIds: selectedIds, customEquipmentNames: [] });
    navigate('/setup/profile');
  };

  return (
    <AppLayout title="헬스장 기구 선택" subtitle="DB의 기준 장비 목록" showBottomNav={false}>
      {loading ? <p>불러오는 중...</p> : null}
      {!loading && equipments.length === 0 ? <p>등록된 기구 데이터가 없습니다.</p> : null}
      {Object.entries(categories).map(([category, items]) => (
        <section key={category} className={styles.categorySection}>
          <h3>{category}</h3>
          <div className={styles.equipmentGrid}>
            {items.map((item) => (
              <button key={item.id} type="button" className={`${styles.eqBtn} ${selected.includes(item.name) ? styles.eqSelected : ''}`} onClick={() => toggle(item.name)}>{item.name}</button>
            ))}
          </div>
        </section>
      ))}
      <div className={styles.bottomInfo}>선택된 기구 <strong>{selected.length}</strong><p>{selected.length > 0 ? selected.join(' · ') : '선택 안 함'}</p></div>
      <button type="button" className={styles.primary} onClick={save} disabled={loading || equipments.length === 0}>선택 완료</button>
      {errorMessage ? <p>{errorMessage}</p> : null}
    </AppLayout>
  );
}
