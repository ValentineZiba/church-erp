import Navbar from "@/components/Navbar";
import Footer from "@/components/Footer";
import Button from "@/components/Button";
import HeroPreview from "@/components/HeroPreview";
import { MODULES } from "@/lib/modules";
import styles from "./page.module.css";

const DIFFERENTIATORS = [
  {
    title: "True multi-tenancy",
    body: "Every church gets its own isolated database, not a row-filtered shared table — the isolation level financial and safeguarding data actually needs.",
  },
  {
    title: "Real fund accounting",
    body: "A chart of accounts, ledger, and budgets — not just donation logging. Built to match enterprise-grade church management systems.",
  },
  {
    title: "Africa-aware payments & messaging",
    body: "Stripe and PayPal for global reach, plus PayFast, Paystack, and Flutterwave, and SMS via Africa's Talking or Twilio.",
  },
];

export default function Home() {
  return (
    <>
      <Navbar />

      <div className={styles.hero}>
        <div>
          <span className={styles.eyebrow}>Multi-tenant Church ERP</span>
          <h1 className={styles.title}>
            Run your church&apos;s operations from one platform.
          </h1>
          <p className={styles.subtitle}>
            Membership, giving, fund accounting, events, volunteers, and
            communications — one system per church, fully isolated, built to
            scale from a 50-member congregation to a multi-campus network.
          </p>
          <div className={styles.ctaRow}>
            <Button href="/login" variant="primary">
              Get started
            </Button>
            <Button href="#modules" variant="secondary">
              See modules
            </Button>
          </div>
        </div>
        <HeroPreview />
      </div>

      <section id="modules" className={styles.section}>
        <div className={styles.sectionHeader}>
          <span className={styles.sectionEyebrow}>Modules</span>
          <h2 className={styles.sectionTitle}>Everything your church runs on</h2>
          <p className={styles.sectionSubtitle}>
            Each module is self-contained and works together through one
            platform.
          </p>
        </div>
        <div className={styles.moduleGrid}>
          {MODULES.map(({ slug, label, description, icon: Icon }) => (
            <div key={slug} className={styles.moduleCard}>
              <span className={styles.moduleIcon}>
                <Icon size={20} />
              </span>
              <div className={styles.moduleLabel}>{label}</div>
              <p className={styles.moduleDescription}>{description}</p>
            </div>
          ))}
        </div>
      </section>

      <div className={styles.differentiators} id="why">
        <section className={styles.section}>
          <div className={styles.sectionHeader}>
            <span className={styles.sectionEyebrow}>Why ChurchOS</span>
            <h2 className={styles.sectionTitle}>
              Enterprise-grade, built for churches
            </h2>
          </div>
          <div className={styles.diffGrid}>
            {DIFFERENTIATORS.map(({ title, body }) => (
              <div key={title}>
                <div className={styles.diffTitle}>{title}</div>
                <p className={styles.diffBody}>{body}</p>
              </div>
            ))}
          </div>
        </section>
      </div>

      <div className={styles.ctaBanner}>
        <h2 className={styles.ctaTitle}>Ready to bring your church online?</h2>
        <p className={styles.ctaSubtitle}>
          Sign in to your tenant, or get in touch to set one up for your
          church.
        </p>
        <div className={styles.ctaRowCenter}>
          <Button href="/login" variant="primary">
            Sign in
          </Button>
        </div>
      </div>

      <Footer />
    </>
  );
}
