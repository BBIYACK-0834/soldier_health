import { useEffect, useState } from 'react';
import Card from '../../components/ui/Card';
import { updateProfile } from '../../api/userApi';
import styles from './SetupPage.module.css';

export const RANK_OPTIONS = ['이병', '일병', '상병', '병장', '하사', '중사', '상사', '원사', '소위', '중위', '대위'];

function toDateInput(value) {
  if (!value) return '';
  return String(value).slice(0, 10);
}

export default function MilitaryProfileForm({ initialProfile, submitLabel = '저장하기', onSaved }) {
  const [heightCm, setHeightCm] = useState('');
  const [weightKg, setWeightKg] = useState('');
  const [rank, setRank] = useState('');
  const [dischargeDate, setDischargeDate] = useState('');
  const [promotionDate, setPromotionDate] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState('');

  useEffect(() => {
    setHeightCm(initialProfile?.heightCm ?? '');
    setWeightKg(initialProfile?.weightKg ?? '');
    setRank(initialProfile?.rank ?? '');
    setDischargeDate(toDateInput(initialProfile?.dischargeDate));
    setPromotionDate(toDateInput(initialProfile?.promotionDate));
  }, [initialProfile]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setMessage('');

    try {
      setSubmitting(true);
      const savedProfile = await updateProfile({
        heightCm: heightCm === '' ? null : Number(heightCm),
        weightKg: weightKg === '' ? null : Number(weightKg),
        rank: rank || null,
        dischargeDate: dischargeDate || null,
        promotionDate: promotionDate || null,
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
        <h3>1. 키와 몸무게</h3>
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
        <h3>2. 계급</h3>
        <div className={styles.chips}>
          {RANK_OPTIONS.map((item) => (
            <button key={item} type="button" className={rank === item ? styles.activeChip : ''} onClick={() => setRank(item)}>
              {item}
            </button>
          ))}
        </div>
      </Card>

      <Card>
        <h3>3. 군 생활 일정</h3>
        <div className={styles.formGrid}>
          <label>
            전역 예정일
            <input type="date" value={dischargeDate} onChange={(e) => setDischargeDate(e.target.value)} />
          </label>
          <label>
            다음 진급 예정일
            <input type="date" value={promotionDate} onChange={(e) => setPromotionDate(e.target.value)} />
          </label>
        </div>
        <small className={styles.helperText}>전역까지 남은 기간과 진급 예정일은 마이 페이지에서 함께 표시됩니다.</small>
      </Card>

      {message ? <p className={styles.infoText}>{message}</p> : null}
      <button type="submit" className={styles.primary} disabled={submitting}>{submitting ? '저장 중...' : submitLabel}</button>
    </form>
  );
}
