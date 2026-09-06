"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import styles from "./page.module.css";

export default function PlatformDashboardPage() {
  const router = useRouter();
  const [checked, setChecked] = useState(false);

  useEffect(() => {
    if (!sessionStorage.getItem("churchos.platformAccessToken")) {
      router.replace("/platform/login");
      return;
    }
    setChecked(true);
  }, [router]);

  if (!checked) {
    return null;
  }

  return (
    <div className={styles.page}>
      <h1 className={styles.title}>Platform control plane</h1>
      <p className={styles.body}>
        Signed in as a super-admin. Tenant provisioning and platform-wide
        tooling aren&apos;t built yet — this page is a placeholder so the
        login flow has somewhere to land.
      </p>
    </div>
  );
}
