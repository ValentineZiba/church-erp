import type { LucideIcon } from "lucide-react";
import styles from "./PlaceholderPage.module.css";

export default function PlaceholderPage({
  title,
  description,
  icon: Icon,
}: {
  title: string;
  description: string;
  icon: LucideIcon;
}) {
  return (
    <div className={styles.placeholder}>
      <span className={styles.icon}>
        <Icon size={22} />
      </span>
      <h1>{title}</h1>
      <p>{description}</p>
      <span className={styles.badge}>Coming soon</span>
    </div>
  );
}
