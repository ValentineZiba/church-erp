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
        <div className={styles.metaColumn}>
          <p className={styles.copyright}>
            © {new Date().getFullYear()} ChurchOS. All rights reserved.
          </p>
          <a href="/platform/login" className={styles.platformLink}>
            Platform admin
          </a>
        </div>
      </div>
    </footer>
  );
}
