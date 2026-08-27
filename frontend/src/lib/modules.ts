import {
  BarChart3,
  Calendar,
  HandCoins,
  HeartHandshake,
  Landmark,
  LayoutDashboard,
  Mail,
  Users,
  UsersRound,
} from "lucide-react";

export const MODULES = [
  {
    slug: "members",
    label: "Membership",
    description: "Member records, households, groups, and tags.",
    icon: Users,
  },
  {
    slug: "giving",
    label: "Giving",
    description: "Donations, pledges, campaigns, and payment methods.",
    icon: HandCoins,
  },
  {
    slug: "accounting",
    label: "Fund Accounting",
    description: "Chart of accounts, ledger, budgets, and financial statements.",
    icon: Landmark,
  },
  {
    slug: "events",
    label: "Events",
    description: "Event registration and check-in.",
    icon: Calendar,
  },
  {
    slug: "groups",
    label: "Groups",
    description: "Small groups and discipleship tracking.",
    icon: UsersRound,
  },
  {
    slug: "volunteers",
    label: "Volunteers",
    description: "Serving teams and volunteer scheduling.",
    icon: HeartHandshake,
  },
  {
    slug: "communications",
    label: "Communications",
    description: "Email and SMS campaigns and templates.",
    icon: Mail,
  },
  {
    slug: "reporting",
    label: "Reporting",
    description: "Dashboards and the custom report builder.",
    icon: BarChart3,
  },
] as const;

export const OVERVIEW_NAV_ITEM = {
  slug: "",
  label: "Overview",
  icon: LayoutDashboard,
};
