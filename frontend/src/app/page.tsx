import { Building2, Database, Globe2, Rocket, SettingsIcon, ShieldCheck } from "lucide-react";
import Navbar from "@/components/Navbar";
import Footer from "@/components/Footer";
import Button from "@/components/Button";
import HeroPreview from "@/components/HeroPreview";
import { MODULES } from "@/lib/modules";
import styles from "./page.module.css";

const TRUST_BADGES = [
  "Database-per-tenant isolation",
  "Multi-campus ready",
  "8 core modules",
];

const HOW_IT_WORKS = [
  {
    step: "01",
    title: "Provision your workspace",
    body: "Your church gets its own isolated database the moment your workspace is created — not a shared table filtered by tenant ID.",
    icon: Building2,
  },
  {
    step: "02",
    title: "Configure your modules",
    body: "Turn on membership, giving, fund accounting, events, and the rest as your ministry needs them — no bundled features you'll never use.",
    icon: SettingsIcon,
  },
  {
    step: "03",
    title: "Launch across every campus",
    body: "Bring on staff and volunteers, connect every site, and run the whole operation — single church or multi-campus network — from one login.",
    icon: Rocket,
  },
];

const DIFFERENTIATORS = [
  {
    title: "True multi-tenancy",
    body: "Every church gets its own isolated database, not a row-filtered shared table — the isolation level financial and safeguarding data actually needs.",
    icon: Database,
  },
  {
    title: "Real fund accounting",
    body: "A chart of accounts, ledger, and budgets — not just donation logging. Built to match enterprise-grade church management systems.",
    icon: Building2,
  },
  {
    title: "Africa-aware payments & messaging",
    body: "Stripe and PayPal for global reach, plus PayFast, Paystack, and Flutterwave, and SMS via Africa's Talking or Twilio.",
    icon: Globe2,
  },
  {
    title: "Security & compliance built in",
    body: "Audit logging on members, donations, and ledger entries, TLS-only by default, and a data model designed around POPIA/GDPR obligations.",
    icon: ShieldCheck,
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
              Start your free trial
            </Button>
            <Button href="#modules" variant="ghost">
              See modules →
            </Button>
          </div>
          <ul className={styles.trustBar}>
            {TRUST_BADGES.map((label) => (
              <li key={label}>{label}</li>
            ))}
          </ul>
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

      <div className={styles.howItWorks} id="how">
        <section className={styles.section}>
          <div className={styles.sectionHeader}>
            <span className={styles.sectionEyebrow}>How it works</span>
            <h2 className={styles.sectionTitle}>From sign-up to Sunday-ready</h2>
          </div>
          <div className={styles.stepGrid}>
            {HOW_IT_WORKS.map(({ step, title, body, icon: Icon }) => (
              <div key={step} className={styles.stepCard}>
                <div className={styles.stepHeader}>
                  <span className={styles.stepIcon}>
                    <Icon size={18} />
                  </span>
                  <span className={styles.stepNumber}>{step}</span>
                </div>
                <div className={styles.stepTitle}>{title}</div>
                <p className={styles.stepBody}>{body}</p>
              </div>
            ))}
          </div>
        </section>
      </div>

      <div className={styles.differentiators} id="why">
        <section className={styles.section}>
          <div className={styles.sectionHeader}>
            <span className={styles.sectionEyebrow}>Why ChurchOS</span>
            <h2 className={styles.sectionTitle}>
              Enterprise-grade, built for churches
            </h2>
          </div>
          <div className={styles.diffGrid}>
            {DIFFERENTIATORS.map(({ title, body, icon: Icon }) => (
              <div key={title} className={styles.diffCard}>
                <span className={styles.diffIcon}>
                  <Icon size={20} />
                </span>
                <div className={styles.diffTitle}>{title}</div>
                <p className={styles.diffBody}>{body}</p>
              </div>
            ))}
          </div>
        </section>
      </div>

      <div className={styles.ctaBanner}>
        <div className={styles.ctaBannerInner}>
          <h2 className={styles.ctaTitle}>
            Ready to bring your church online?
          </h2>
          <p className={styles.ctaSubtitle}>
            Set up your church&apos;s workspace in minutes, or explore the
            modules first to see how ChurchOS fits your ministry.
          </p>
          <div className={styles.ctaRowCenter}>
            <Button href="/login" variant="primary" className={styles.ctaButtonOnDark}>
              Start your free trial
            </Button>
            <Button href="#modules" variant="ghost" className={styles.ctaButtonGhostOnDark}>
              See modules
            </Button>
          </div>
        </div>
      </div>

      <Footer />
    </>
  );
}
