import { useEffect, useMemo, useState } from 'react';
import Card from '../../components/ui/Card';
import { updateProfile, uploadProfileImage } from '../../api/userApi';
import styles from './SetupPage.module.css';

const DEFAULT_PROFILE_IMAGES = [
  'https://api.dicebear.com/9.x/thumbs/svg?seed=soldier-default',
  'https://api.dicebear.com/9.x/thumbs/svg?seed=soldier-1',
  'https://api.dicebear.com/9.x/thumbs/svg?seed=soldier-2',
];

const ARMY_SERVICE_MONTHS = 18;
const PRIVATE_FIRST_CLASS_MONTH = 2;
const CORPORAL_MONTH = 8;
const SERGEANT_MONTH = 14;

function toDateInput(value) {
  if (!value) return '';
  return String(value).slice(0, 10);
}

function parseDateInput(value) {
  if (!value) return null;
  const [year, month, day] = value.split('-').map(Number);
  if (!year || !month || !day) return null;
  return new Date(year, month - 1, day);
}

function formatDate(value) {
  if (!value) return '-';
  const year = value.getFullYear();
  const month = String(value.getMonth() + 1).padStart(2, '0');
  const day = String(value.getDate()).padStart(2, '0');
  return `${year}.${month}.${day}`;
}

function addMonths(value, months) {
  const result = new Date(value);
  const targetMonth = result.getMonth() + months;
  result.setMonth(targetMonth);

  if (result.getMonth() !== ((targetMonth % 12) + 12) % 12) {
    result.setDate(0);
  }

  return result;
}

function addDays(value, days) {
  const result = new Date(value);
  result.setDate(result.getDate() + days);
  return result;
}

function calculateArmyService(enlistmentDate) {
  const enlistedAt = parseDateInput(enlistmentDate);
  if (!enlistedAt) {
    return null;
  }

  const today = new Date();
  const dischargeAt = addDays(addMonths(enlistedAt, ARMY_SERVICE_MONTHS), -1);
  const privateFirstClassAt = addMonths(enlistedAt, PRIVATE_FIRST_CLASS_MONTH);
  const corporalAt = addMonths(enlistedAt, CORPORAL_MONTH);
  const sergeantAt = addMonths(enlistedAt, SERGEANT_MONTH);

  let rank = '이병';
  let nextPromotionDate = privateFirstClassAt;

  if (today >= sergeantAt) {
    rank = '병장';
    nextPromotionDate = null;
  } else if (today >= corporalAt) {
    rank = '상병';
    nextPromotionDate = sergeantAt;
  } else if (today >= privateFirstClassAt) {
    rank = '일병';
    nextPromotionDate = corporalAt;
  }

  return {
    rank,
    dischargeAt,
    nextPromotionDate,
  };
}

export default function MilitaryProfileForm({ initialProfile, submitLabel = '저장하기', onSaved }) {
  const [profileImageUrl, setProfileImageUrl] = useState(DEFAULT_PROFILE_IMAGES[0]);
  const [heightCm, setHeightCm] = useState('');
  const [weightKg, setWeightKg] = useState('');
  const [enlistmentDate, setEnlistmentDate] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [message, setMessage] = useState('');

  const armyService = useMemo(() => calculateArmyService(enlistmentDate), [enlistmentDate]);

  useEffect(() => {
    setProfileImageUrl(initialProfile?.profileImageUrl ?? DEFAULT_PROFILE_IMAGES[0]);
    setHeightCm(initialProfile?.heightCm ?? '');
    setWeightKg(initialProfile?.weightKg ?? '');
    setEnlistmentDate(toDateInput(initialProfile?.enlistmentDate));
  }, [initialProfile]);

  const handleProfileUpload = async (event) => {
    const file = event.target.files?.[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      setMessage('이미지 파일만 업로드할 수 있습니다.');
      return;
    }

    try {
      setUploading(true);
      setMessage('');
      const savedProfile = await uploadProfileImage(file);
      setProfileImageUrl(savedProfile.profileImageUrl ?? DEFAULT_PROFILE_IMAGES[0]);
      setMessage('프로필 이미지가 업로드되었습니다.');
    } catch (error) {
      setMessage(error.message || '프로필 이미지를 업로드하지 못했습니다.');
    } finally {
      setUploading(false);
      event.target.value = '';
    }
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setMessage('');

    try {
      setSubmitting(true);
      const savedProfile = await updateProfile({
        profileImageUrl: profileImageUrl || DEFAULT_PROFILE_IMAGES[0],
        heightCm: heightCm === '' ? null : Number(heightCm),
        weightKg: weightKg === '' ? null : Number(weightKg),
        enlistmentDate: enlistmentDate || null,
      });
      setMessage('나의 군 생활 정보가 저장되었습니다.');
      onSaved?.(savedProfile);
    } catch (error) {
      setMessage(error.message || '군 생활 정보를 저장하지 못했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form className={styles.formStack} onSubmit={handleSubmit}>
      <Card>
        <h3>1. 프로필 사진</h3>
        <div className={styles.profileImagePicker}>
          <img className={styles.profilePreviewImage} src={profileImageUrl} alt="프로필 미리보기" />
          <div className={styles.defaultProfileGrid}>
            {DEFAULT_PROFILE_IMAGES.map((imageUrl) => (
              <button key={imageUrl} type="button" className={profileImageUrl === imageUrl ? styles.activeProfileImage : ''} onClick={() => setProfileImageUrl(imageUrl)}>
                <img src={imageUrl} alt="기본 프로필" />
              </button>
            ))}
          </div>
          <label className={styles.uploadButton}>
            {uploading ? '업로드 중...' : '내 이미지 업로드'}
            <input type="file" accept="image/*" onChange={handleProfileUpload} disabled={uploading} />
          </label>
        </div>
      </Card>

      <Card>
        <h3>2. 키와 몸무게</h3>
        <div className={styles.inlineTwo}>
          <label>
            키(cm)
            <input type="number" min="1" step="0.1" placeholder="예: 175" value={heightCm} onChange={(e) => setHeightCm(e.target.value)} />
          </label>
          <label>
            몸무게(kg)
            <input type="number" min="1" step="0.1" placeholder="예: 72" value={weightKg} onChange={(e) => setWeightKg(e.target.value)} />
          </label>
        </div>
      </Card>

      <Card>
        <h3>3. 군 생활 일정</h3>
        <div className={styles.formGrid}>
          <label>
            육군 입대일
            <input type="date" value={enlistmentDate} onChange={(e) => setEnlistmentDate(e.target.value)} />
          </label>
        </div>
        <div className={styles.serviceSummary}>
          <span>현재 계급 <strong>{armyService?.rank ?? '-'}</strong></span>
          <span>전역 예정일 <strong>{formatDate(armyService?.dischargeAt)}</strong></span>
          <span>다음 진급 <strong>{armyService?.nextPromotionDate ? formatDate(armyService.nextPromotionDate) : '진급 일정 없음'}</strong></span>
        </div>
        <small className={styles.helperText}>육군 병 복무 18개월, 이병 2개월·일병 6개월·상병 6개월 기준으로 자동 계산합니다.</small>
      </Card>

      {message ? <p className={styles.infoText}>{message}</p> : null}
      <button type="submit" className={styles.primary} disabled={submitting || uploading}>{submitting ? '저장 중...' : submitLabel}</button>
    </form>
  );
}
