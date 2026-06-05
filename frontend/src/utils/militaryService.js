const MS_PER_DAY = 1000 * 60 * 60 * 24;
const ARMY_SERVICE_MONTHS = 18;
const PROMOTION_SCHEDULE = [
  { rank: '일병', months: 3 },
  { rank: '상병', months: 9 },
  { rank: '병장', months: 12 },
];

function parseDate(value) {
  if (!value) return null;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return null;
  date.setHours(0, 0, 0, 0);
  return date;
}

function addMonths(date, months) {
  const next = new Date(date);
  next.setMonth(next.getMonth() + months);
  return next;
}

function toIsoDate(date) {
  if (!date) return null;
  return date.toISOString().slice(0, 10);
}

function daysBetween(start, end) {
  return Math.ceil((end.getTime() - start.getTime()) / MS_PER_DAY);
}

export function calculateMilitaryService(profile = {}) {
  const today = parseDate(new Date()) ?? new Date();
  const enlistmentDate = parseDate(profile.enlistmentDate);
  const manualDischargeDate = parseDate(profile.dischargeDate);
  const manualPromotionDate = parseDate(profile.promotionDate ?? profile.nextPromotionDate);

  if (!enlistmentDate) {
    return {
      rank: profile.rank ?? '이병',
      dischargeDate: toIsoDate(manualDischargeDate),
      nextPromotionDate: toIsoDate(manualPromotionDate),
      daysUntilDischarge: manualDischargeDate ? Math.max(0, daysBetween(today, manualDischargeDate)) : null,
      serviceProgressPercent: profile.serviceProgressPercent ?? null,
    };
  }

  const computedDischarge = addMonths(enlistmentDate, ARMY_SERVICE_MONTHS);
  computedDischarge.setDate(computedDischarge.getDate() - 1);
  const dischargeDate = manualDischargeDate ?? computedDischarge;
  const serviceDays = Math.max(1, daysBetween(enlistmentDate, dischargeDate) + 1);
  const servedDays = Math.max(0, daysBetween(enlistmentDate, today));
  const serviceProgressPercent = Math.min(100, Math.max(0, (servedDays / serviceDays) * 100));

  let rank = '이병';
  let nextPromotionDate = null;
  PROMOTION_SCHEDULE.forEach((item) => {
    const promotionDate = addMonths(enlistmentDate, item.months);
    if (today >= promotionDate) {
      rank = item.rank;
    } else if (!nextPromotionDate) {
      nextPromotionDate = promotionDate;
    }
  });

  return {
    rank: profile.rank ?? rank,
    dischargeDate: toIsoDate(dischargeDate),
    nextPromotionDate: toIsoDate(manualPromotionDate ?? nextPromotionDate),
    daysUntilDischarge: Math.max(0, daysBetween(today, dischargeDate)),
    serviceProgressPercent,
  };
}
