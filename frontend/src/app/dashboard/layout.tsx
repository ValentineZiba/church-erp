import Sidebar from "@/components/Sidebar";
import Topbar from "@/components/Topbar";
import styles from "./layout.module.css";

export default function DashboardLayout({ children }: LayoutProps<"/dashboard">) {
  return (
    <div className={styles.shell}>
      <Sidebar />
      <div className={styles.content}>
        <Topbar />
        <main className={styles.main}>{children}</main>
      </div>
    </div>
  );
}
