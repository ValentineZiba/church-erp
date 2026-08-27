import { Calendar, HandCoins, Users } from "lucide-react";
import styles from "./HeroPreview.module.css";

const STATS = [
  { icon: Users, label: "Members", value: "1,204" },
  { icon: HandCoins, label: "Giving (MTD)", value: "$18,420" },
  { icon: Calendar, label: "Upcoming events", value: "6" },
];

export default function HeroPreview() {
  return (
    <div className={styles.frame} aria-hidden="true">
      <div className={styles.rail}>
        <span className={styles.dot} />
        <span className={styles.dot} />
        <span className={styles.dot} />
      </div>
      <div className={styles.body}>
        <aside className={styles.sidebar}>
          <div className={styles.sidebarBrand} />
          {Array.from({ length: 5 }).map((_, i) => (
            <div
              key={i}
              className={`${styles.sidebarItem} ${i === 0 ? styles.sidebarItemActive : ""}`}
            />
          ))}
        </aside>
        <div className={styles.content}>
          <div className={styles.statRow}>
            {STATS.map(({ icon: Icon, label, value }) => (
              <div key={label} className={styles.statCard}>
                <Icon size={16} />
                <span className={styles.statValue}>{value}</span>
                <span className={styles.statLabel}>{label}</span>
              </div>
            ))}
          </div>
          <div className={styles.panel} />
          <div className={styles.panelRow}>
            <div className={styles.panelSmall} />
            <div className={styles.panelSmall} />
          </div>
        </div>
      </div>
    </div>
  );
}
