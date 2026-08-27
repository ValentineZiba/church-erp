import PlaceholderPage from "@/components/PlaceholderPage";
import { MODULES } from "@/lib/modules";

const module_ = MODULES.find((m) => m.slug === "accounting")!;

export default function AccountingPage() {
  return (
    <PlaceholderPage
      title={module_.label}
      description={module_.description}
      icon={module_.icon}
    />
  );
}
