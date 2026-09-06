import Link from "next/link";
import Logo from "./Logo";
import Button from "./Button";
import styles from "./Navbar.module.css";

export default function Navbar() {
  return (
    <header className={styles.header}>
      <div className={styles.inner}>
        <Link href="/" aria-label="ChurchOS home">
          <Logo />
        </Link>
        <nav className={styles.nav}>
          <a href="#modules">Modules</a>
          <a href="#how">How it works</a>
          <a href="#why">Why ChurchOS</a>
        </nav>
        <div className={styles.actions}>
          <Button href="/login" variant="ghost">
            Log in
          </Button>
          <Button href="/login" variant="primary" className={styles.navCta}>
            Get started
          </Button>
        </div>
      </div>
    </header>
  );
}
