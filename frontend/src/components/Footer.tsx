import Logo from "./Logo";
import styles from "./Footer.module.css";

export default function Footer() {
  return (
    <footer className={styles.footer}>
      <div className={styles.inner}>
        <div>
          <Logo size="sm" />
          <p className={styles.tagline}>
            Multi-tenant church management ERP — membership, giving, fund
            accounting, events, and more, in one place.
          </p>
        </div>
        <p className={styles.copyright}>
          © {new Date().getFullYear()} ChurchOS. All rights reserved.
        </p>
      </div>
    </footer>
  );
}
