import { Calendar, HandCoins, ListChecks, Users } from "lucide-react";
import StatCard from "@/components/StatCard";
import styles from "./page.module.css";

export default function DashboardOverviewPage() {
  return (
    <div className={styles.page}>
      <div className={styles.statGrid}>
        <StatCard icon={Users} label="Members" />
        <StatCard icon={HandCoins} label="Giving this month" />
        <StatCard icon={Calendar} label="Upcoming events" />
        <StatCard icon={ListChecks} label="Open tasks" />
      </div>

      <div className={styles.emptyPanel}>
        <div className={styles.emptyTitle}>No activity yet</div>
        <p className={styles.emptyBody}>
          This tenant isn&apos;t connected to a backend yet — once
          provisioning and the membership/giving modules exist, recent
          activity will show up here.
        </p>
      </div>
    </div>
  );
}
