const SERVICE_MONTHS = 18;

const PROMOTION_MONTHS = [
  { rank: '병장', months: 12 },
  { rank: '상병', months: 9 },
  { rank: '일병', months: 3 },
  { rank: '이병', months: 0 },
];

function parseDate(value) {
  if (!value) return null;
  const [year, month, day] = value.split('-').map(Number);
  if (!year || !month || !day) return null;
  return new Date(year, month - 1, day);
}

function formatDate(date) {
  if (!date) return '';
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function addMonths(date, months) {
  const result = new Date(date);
  const originalDay = result.getDate();
  result.setDate(1);
  result.setMonth(result.getMonth() + months);
  const lastDayOfTargetMonth = new Date(result.getFullYear(), result.getMonth() + 1, 0).getDate();
  result.setDate(Math.min(originalDay, lastDayOfTargetMonth));
  return result;
}

function addDays(date, days) {
  const result = new Date(date);
  result.setDate(result.getDate() + days);
  return result;
}

function differenceInCalendarDays(fromDate, toDate) {
  const start = Date.UTC(fromDate.getFullYear(), fromDate.getMonth(), fromDate.getDate());
  const end = Date.UTC(toDate.getFullYear(), toDate.getMonth(), toDate.getDate());
  return Math.ceil((end - start) / (1000 * 60 * 60 * 24));
}

export function calculateMilitaryService(enlistmentDateValue, baseDateValue = formatDate(new Date())) {
  const enlistmentDate = parseDate(enlistmentDateValue);
  if (!enlistmentDate) return null;

  const baseDate = parseDate(baseDateValue) ?? new Date();
  const dischargeDate = addDays(addMonths(enlistmentDate, SERVICE_MONTHS), -1);
  const elapsedDays = Math.max(0, differenceInCalendarDays(enlistmentDate, baseDate));
  const totalDays = Math.max(1, differenceInCalendarDays(enlistmentDate, dischargeDate) + 1);
  const progressPercent = Math.min(100, Math.max(0, (elapsedDays / totalDays) * 100));
  const rank = PROMOTION_MONTHS.find((promotion) => baseDate >= addMonths(enlistmentDate, promotion.months))?.rank ?? '이병';
  const nextPromotion = [...PROMOTION_MONTHS]
    .reverse()
    .find((promotion) => promotion.months > 0 && baseDate < addMonths(enlistmentDate, promotion.months));

  return {
    enlistmentDate: formatDate(enlistmentDate),
    dischargeDate: formatDate(dischargeDate),
    rank,
    nextPromotionDate: nextPromotion ? formatDate(addMonths(enlistmentDate, nextPromotion.months)) : '',
    daysUntilDischarge: Math.max(0, differenceInCalendarDays(baseDate, dischargeDate)),
    progressPercent,
  };
}
