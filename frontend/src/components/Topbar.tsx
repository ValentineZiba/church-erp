"use client";

import { usePathname } from "next/navigation";
import { Bell } from "lucide-react";
import { getActiveNavLabel } from "@/lib/navigation";
import styles from "./Topbar.module.css";

export default function Topbar() {
  const pathname = usePathname();

  return (
    <header className={styles.topbar}>
      <h1 className={styles.title}>{getActiveNavLabel(pathname)}</h1>
      <div className={styles.actions}>
        <button className={styles.iconButton} aria-label="Notifications" disabled>
          <Bell size={18} />
        </button>
        <div className={styles.avatar} title="Church Admin">
          CA
        </div>
      </div>
    </header>
  );
}
