import AppLayout from '../../components/layout/AppLayout';
import Card from '../../components/ui/Card';
import styles from './MyPage.module.css';

const rows = [
  { icon: '⬇️', title: '데이터 내보내기', hint: '내 데이터를 파일로 저장' },
  { icon: '⬆️', title: '데이터 가져오기', hint: '저장된 데이터를 불러오기' },
  { icon: '🗑️', title: '계정 삭제', hint: '계정을 삭제하고 모든 데이터 제거', danger: true },
];

export default function DataManagementPage() {
  return (
    <AppLayout title="데이터 관리" showBottomNav={false}>
      <Card>
        {rows.map((row) => (
          <div key={row.title} className={`${styles.dataRow} ${row.danger ? styles.danger : ''}`}>
            <span>{row.icon}</span>
            <div><strong>{row.title}</strong><p>{row.hint}</p></div>
          </div>
        ))}
      </Card>
    </AppLayout>
  );
}
