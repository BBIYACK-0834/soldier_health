import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppLayout from '../../components/layout/AppLayout';
import Card from '../../components/ui/Card';
import { useAppContext } from '../../app/AppContext';
import { getMyProfile } from '../../api/userApi';
import { emptyUser } from '../../constants/defaultData';
import { calculateMilitaryService } from '../../utils/militaryService';
import { getWeeklyWorkoutSummary } from '../../utils/workoutStorage';
import styles from './MyPage.module.css';

const menus = [
  { label: '목표 설정', description: '체중, 칼로리, 운동 목표 관리', path: '/mypage/goal', icon: '🎯' },
  { label: '알림 설정', description: '운동 리마인더, 목표 알림 관리', path: '/mypage/notifications', icon: '🔔' },
  { label: '내 기록', description: '운동, 식단, 체중 변화 확인', path: '/mypage/posts', icon: '📊' },
  { label: '데이터 관리', description: '데이터 내보내기, 초기화', path: '/mypage/data', icon: '🪣' },
];

function getRemainingDays(dateString) {
  if (!dateString) return null;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const target = new Date(dateString);
  if (Number.isNaN(target.getTime())) return null;
  target.setHours(0, 0, 0, 0);
  return Math.ceil((target.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
}

function formatDate(dateString) {
  if (!dateString) return '미설정';
  const date = new Date(dateString);
  if (Number.isNaN(date.getTime())) return dateString;
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}.${month}.${day}`;
}

function formatNumber(value, fallback = '0') {
  return Number.isFinite(Number(value)) ? Number(value).toLocaleString() : fallback;
}

function getBranchLabel(value) {
  const labels = {
    ARMY: '육군',
    NAVY: '해군',
    AIR_FORCE: '공군',
    MARINE: '해병대',
  };
  return labels[value] ?? '육군';
}

export default function ProfilePage() {
  const navigate = useNavigate();
  const { actions } = useAppContext();
  const [profile, setProfile] = useState(emptyUser);
  const [weeklyExercise, setWeeklyExercise] = useState(() => getWeeklyWorkoutSummary());

  useEffect(() => {
    let mounted = true;
    async function load() {
      try {
        const data = await getMyProfile();
        if (!mounted) return;
        setProfile(data ? { ...emptyUser, ...data } : emptyUser);
      } catch {
        if (!mounted) return;
        setProfile(emptyUser);
      }
    }
    load();
    return () => {
      mounted = false;
    };
  }, []);

  useEffect(() => {
    const refreshWorkoutSummary = () => setWeeklyExercise(getWeeklyWorkoutSummary());
    window.addEventListener('focus', refreshWorkoutSummary);
    window.addEventListener('tg-workout-progress-updated', refreshWorkoutSummary);
    return () => {
      window.removeEventListener('focus', refreshWorkoutSummary);
      window.removeEventListener('tg-workout-progress-updated', refreshWorkoutSummary);
    };
  }, []);

  const serviceInfo = calculateMilitaryService(profile);
  const rank = serviceInfo.rank || profile?.rank || '계급 미설정';
  const dischargeDate = serviceInfo.dischargeDate || profile?.dischargeDate;
  const enlistmentDate = profile?.enlistmentDate;
  const remainingDays = serviceInfo.daysUntilDischarge ?? getRemainingDays(dischargeDate);
  const serviceProgress = Math.round(serviceInfo.serviceProgressPercent ?? profile?.serviceProgressPercent ?? 0);
  const servedDays = remainingDays !== null && serviceProgress > 0
    ? Math.max(0, Math.round((remainingDays * serviceProgress) / Math.max(1, 100 - serviceProgress)))
    : 0;
  const totalServiceDays = servedDays + (remainingDays ?? 0);
  const currentWeight = Number(profile?.weightKg ?? 0);
  const targetWeight = Number(profile?.targetWeight ?? 0);
  const targetGap = currentWeight && targetWeight ? targetWeight - currentWeight : null;

  const handleLogout = () => {
    actions.logout();
    navigate('/login');
  };

  return (
    <AppLayout>
      <header className={styles.pageHeader}>
        <h1>마이페이지</h1>
        <button type="button" className={styles.settingsButton} onClick={() => navigate('/mypage/settings')} aria-label="내 정보 설정">⚙️</button>
      </header>

      <Card className={styles.profileHero}>
        <div className={styles.profileAvatarWrap}>
          <div className={styles.avatar}>{profile?.profileImageUrl ? <img src={profile.profileImageUrl} alt="프로필" /> : '🪖'}</div>
          <button type="button" className={styles.cameraButton} onClick={() => navigate('/mypage/settings')} aria-label="프로필 사진 변경">📷</button>
        </div>
        <div className={styles.profileIdentity}>
          <button type="button" onClick={() => navigate('/mypage/settings')}>
            <strong>{profile?.nickname || '사용자'}</strong>
            <span>›</span>
          </button>
          <p>{profile?.unitName || '소속 부대 미설정'}</p>
          <p>{rank} ㅣ {formatDate(enlistmentDate)} 입대</p>
        </div>
        <div className={styles.dogTag}>🎖️</div>
      </Card>

      <Card className={styles.serviceCard}>
        <h2>군 복무 & 전역 정보</h2>
        <div className={styles.serviceGrid}>
          <div className={styles.ddayBox}>
            <p>전역까지 남은 시간</p>
            <strong>{remainingDays === null ? 'D-?' : `D-${remainingDays}`}</strong>
            <span className={styles.serviceTrack}><i style={{ width: `${serviceProgress}%` }} /></span>
            <small>{formatDate(dischargeDate)} 전역 예정</small>
          </div>
          <dl className={styles.serviceList}>
            <div><dt>📅 입대일</dt><dd>{formatDate(enlistmentDate)}</dd></div>
            <div><dt>🛡️ 계급</dt><dd>{rank}</dd></div>
            <div><dt>👤 복무일</dt><dd>{servedDays}일 / {totalServiceDays || 0}일</dd></div>
            <div><dt>⭐ 전역 예정일</dt><dd>{formatDate(dischargeDate)}</dd></div>
          </dl>
        </div>
      </Card>

      <Card className={styles.summaryCard}>
        <h2>나의 요약</h2>
        <div className={styles.summaryGrid}>
          <div><p>현재 체중</p><strong>{formatNumber(currentWeight, '0')}kg</strong><small>{targetGap !== null ? `${targetGap > 0 ? '+' : ''}${targetGap.toFixed(1)}kg` : '기록 대기'}</small></div>
          <div><p>목표 체중</p><strong>{formatNumber(targetWeight, '0')}kg</strong><small>{targetGap !== null ? `목표까지 ${Math.abs(targetGap).toFixed(1)}kg` : '목표 설정'}</small></div>
          <div><p>연속 기록</p><strong>{profile?.streakDays ?? 0}일</strong><small>꾸준히 진행 중</small></div>
          <div><p>총 운동 기록</p><strong>{profile?.totalWorkoutCount ?? 0}회</strong><small>이번 주 {weeklyExercise.completed}회</small></div>
        </div>
      </Card>

      <Card className={styles.menuCard}>
        <ul className={styles.menu}>
          {menus.map((item) => (
            <li key={item.label} onClick={() => navigate(item.path)}>
              <span className={styles.menuIcon}>{item.icon}</span>
              <span><strong>{item.label}</strong><small>{item.description}</small></span>
              <b>›</b>
            </li>
          ))}
        </ul>
      </Card>

      <Card className={styles.cheerCard}>
        <div className={styles.medal}>🏅</div>
        <div>
          <strong>오늘도 잘하고 있습니다!</strong>
          <p>꾸준함이 가장 강한 무기입니다.<br />작은 습관이 큰 변화를 만듭니다.</p>
        </div>
        <span className={styles.gearEmoji}>🎒</span>
      </Card>

      <button type="button" className={styles.logoutButton} onClick={handleLogout}>로그아웃</button>
    </AppLayout>
  );
}
