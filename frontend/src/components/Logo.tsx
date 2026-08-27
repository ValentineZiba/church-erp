import styles from "./Logo.module.css";

export default function Logo({ size = "md" }: { size?: "sm" | "md" }) {
  return (
    <span className={`${styles.logo} ${size === "sm" ? styles.sm : ""}`}>
      <svg
        className={styles.mark}
        viewBox="0 0 24 24"
        fill="none"
        aria-hidden="true"
      >
        <rect width="24" height="24" rx="7" fill="currentColor" />
        <path
          d="M12 6v12M7 10h10"
          stroke="var(--logo-accent, var(--brand-foreground))"
          strokeWidth="2"
          strokeLinecap="round"
        />
      </svg>
      <span className={styles.word}>ChurchOS</span>
    </span>
  );
}
