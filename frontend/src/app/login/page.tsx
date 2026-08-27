"use client";

import { useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { Check } from "lucide-react";
import Logo from "@/components/Logo";
import Button from "@/components/Button";
import styles from "./page.module.css";

const VALUE_PROPS = [
  "Database-per-tenant isolation for every church",
  "Fund accounting, giving, and membership in one place",
  "Built for multi-campus and multi-site ministries",
];

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);

    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
      });

      if (!response.ok) {
        setError("Invalid email or password.");
        return;
      }

      const data = await response.json();
      sessionStorage.setItem("churchos.accessToken", data.accessToken);
      router.push("/dashboard");
    } catch {
      setError("Couldn't reach the server. Is the backend running?");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className={styles.page}>
      <aside className={styles.brandPanel}>
        <Logo />
        <div className={styles.brandCopy}>
          <h1 className={styles.brandTitle}>
            Run your whole church on one platform.
          </h1>
          <ul className={styles.brandList}>
            {VALUE_PROPS.map((item) => (
              <li key={item} className={styles.brandListItem}>
                <Check size={18} />
                <span>{item}</span>
              </li>
            ))}
          </ul>
        </div>
        <p className={styles.brandFooter}>
          © {new Date().getFullYear()} ChurchOS
        </p>
      </aside>

      <div className={styles.formSide}>
        <form className={styles.formCard} onSubmit={handleSubmit}>
          <span className={styles.mobileLogo}>
            <Logo size="sm" />
          </span>

          <div className={styles.formHeader}>
            <h2 className={styles.title}>Sign in</h2>
            <p className={styles.subtitle}>
              Access your church&apos;s ChurchOS workspace.
            </p>
          </div>

          <div className={styles.field}>
            <label htmlFor="email">Email</label>
            <input
              id="email"
              name="email"
              type="email"
              autoComplete="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>

          <div className={styles.field}>
            <div className={styles.rowBetween}>
              <label htmlFor="password">Password</label>
              <span className={styles.linkMuted}>Forgot password?</span>
            </div>
            <input
              id="password"
              name="password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>

          {error && <p className={styles.error}>{error}</p>}

          <Button type="submit" variant="primary" disabled={submitting}>
            {submitting ? "Signing in…" : "Sign in"}
          </Button>
        </form>
      </div>
    </div>
  );
}
