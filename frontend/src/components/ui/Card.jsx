import styles from './Card.module.css';

export default function Card({ children, className = '' }) {
  return <section className={`${styles.card} ${className}`.trim()}>{children}</section>;
}
