import type { LucideIcon } from "lucide-react";
import styles from "./StatCard.module.css";

export default function StatCard({
  icon: Icon,
  label,
  value = "—",
}: {
  icon: LucideIcon;
  label: string;
  value?: string;
}) {
  return (
    <div className={styles.card}>
      <span className={styles.icon}>
        <Icon size={18} />
      </span>
      <div className={styles.value}>{value}</div>
      <div className={styles.label}>{label}</div>
    </div>
  );
}
