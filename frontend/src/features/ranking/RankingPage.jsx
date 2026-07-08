import AppLayout from '../../components/layout/AppLayout';
import Card from '../../components/ui/Card';
import styles from './RankingPage.module.css';

const placeholderRanks = [
  { rank: 1, name: '우리 부대 TOP', score: '운동 완료 기록 기준' },
  { rank: 2, name: '전우 랭킹', score: '준비 중' },
  { rank: 3, name: '월간 챌린지', score: '준비 중' },
];

export default function RankingPage() {
  return (
    <AppLayout title="랭킹" subtitle="운동 완료 기록과 챌린지 순위를 확인하세요.">
      <Card>
        <h3 className={styles.title}>랭킹 준비 중</h3>
        <p className={styles.description}>하단 바 구성을 위해 랭킹 탭을 추가했습니다. 실제 순위 데이터가 연결되면 이 화면에 표시됩니다.</p>
        <ol className={styles.list}>
          {placeholderRanks.map((item) => (
            <li key={item.rank}>
              <strong>{item.rank}</strong>
              <span>{item.name}</span>
              <small>{item.score}</small>
            </li>
          ))}
        </ol>
      </Card>
    </AppLayout>
  );
}
