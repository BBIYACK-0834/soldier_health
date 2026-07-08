import { useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { getMyUnit } from '../../api/unitApi';
import { getMyProfile } from '../../api/userApi';
import { getMyEquipments } from '../../api/equipmentApi';
import styles from './RequiredSetupModal.module.css';

const setupRoutes = ['/guide', '/mypage/goal', '/mypage/settings', '/setup/equipment', '/setup/profile', '/unit/setup', '/unit/search', '/unit/select', '/unit/complete'];

function isSetupRoute(pathname) {
  return setupRoutes.some((route) => pathname.startsWith(route));
}

function getMissingItems(profile, unit, equipments = []) {
  const missing = [];
  if (!unit?.id && !profile?.unitId) missing.push({ label: '부대', path: '/unit/setup' });
  if (!Number(profile?.heightCm) || !Number(profile?.weightKg)) missing.push({ label: '키/현재 몸무게', path: '/mypage/settings' });
  if (!Number(profile?.targetWeight)) missing.push({ label: '목표 체중', path: '/mypage/goal' });
  if (!profile?.goalType) missing.push({ label: '운동 목표', path: '/mypage/goal' });
  if (!profile?.workoutLevel) missing.push({ label: '운동 숙련도', path: '/mypage/goal' });
  if (!Number(profile?.workoutDaysPerWeek)) missing.push({ label: '주 운동 횟수', path: '/mypage/goal' });
  if (!Number(profile?.preferredWorkoutMinutes)) missing.push({ label: '선호 운동 시간', path: '/mypage/goal' });
  if (!profile?.branchType) missing.push({ label: '군 구분', path: '/mypage/goal' });
  if (!equipments.length) missing.push({ label: '기구 선택', path: '/setup/equipment' });
  return missing;
}

export default function RequiredSetupModal() {
  const navigate = useNavigate();
  const location = useLocation();
  const [state, setState] = useState({ loading: true, profile: null, unit: null, equipments: [] });

  useEffect(() => {
    let mounted = true;
    Promise.allSettled([getMyProfile(), getMyUnit(), getMyEquipments()])
      .then(([profileResult, unitResult, equipmentsResult]) => {
        if (!mounted) return;
        setState({
          loading: false,
          profile: profileResult.status === 'fulfilled' ? profileResult.value : null,
          unit: unitResult.status === 'fulfilled' ? unitResult.value : null,
          equipments: equipmentsResult.status === 'fulfilled' ? equipmentsResult.value ?? [] : [],
        });
      });
    return () => {
      mounted = false;
    };
  }, [location.pathname]);

  const missingItems = useMemo(() => getMissingItems(state.profile, state.unit, state.equipments), [state]);

  if (state.loading || isSetupRoute(location.pathname) || missingItems.length === 0) return null;

  const firstPath = missingItems[0].path;

  return (
    <div className={styles.backdrop} role="dialog" aria-modal="true" aria-labelledby="required-setup-title">
      <div className={styles.modal}>
        <h2 id="required-setup-title">필수 설정이 필요합니다</h2>
        <p>운동 추천과 식단 추정 기준을 위해 아래 정보를 먼저 등록해주세요. 설정이 끝나면 앱 기능을 정상 이용할 수 있습니다.</p>
        <ul className={styles.list}>
          {missingItems.map((item) => <li key={item.label}><span>{item.label}</span><span>필수</span></li>)}
        </ul>
        <div className={styles.actions}>
          <button type="button" onClick={() => navigate(firstPath)}>설정하러 가기</button>
        </div>
      </div>
    </div>
  );
}
