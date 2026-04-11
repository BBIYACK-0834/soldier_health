import styles from './MobileShell.module.css';

export default function MobileShell({ title, children, actions }) {
  return (
    <div className={styles.shell}>
      <header className={styles.header}>
        <h1>{title}</h1>
        {actions ? <div>{actions}</div> : null}
      </header>
      <main className={styles.main}>{children}</main>
    </div>
  );
}
