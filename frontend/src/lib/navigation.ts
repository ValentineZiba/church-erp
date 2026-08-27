import { LayoutDashboard } from "lucide-react";
import { MODULES } from "./modules";

export const DASHBOARD_NAV = [
  { href: "/dashboard", label: "Overview", icon: LayoutDashboard },
  ...MODULES.map((m) => ({
    href: `/dashboard/${m.slug}`,
    label: m.label,
    icon: m.icon,
  })),
];

export function isNavItemActive(pathname: string, href: string) {
  return href === "/dashboard" ? pathname === href : pathname.startsWith(href);
}

export function getActiveNavLabel(pathname: string) {
  const match = [...DASHBOARD_NAV]
    .sort((a, b) => b.href.length - a.href.length)
    .find((item) => isNavItemActive(pathname, item.href));
  return match?.label ?? "Dashboard";
}
