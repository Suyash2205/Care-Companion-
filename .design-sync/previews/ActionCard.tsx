import * as React from "react";
import { ActionCard } from "carecompanion-design-system";

const ContactsIcon = (
  <svg viewBox="0 0 24 24" width="44" height="44" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
    <circle cx="9" cy="7" r="4" />
    <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
    <path d="M16 3.13a4 4 0 0 1 0 7.75" />
  </svg>
);

const MovieIcon = (
  <svg viewBox="0 0 24 24" width="44" height="44" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
    <rect x="2" y="4" width="20" height="16" rx="2" />
    <path d="m10 9 5 3-5 3z" />
  </svg>
);

const MedicineIcon = (
  <svg viewBox="0 0 24 24" width="44" height="44" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
    <rect x="3" y="8" width="18" height="12" rx="2" />
    <path d="M8 8V6a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
    <path d="M12 11v5" />
    <path d="M9.5 13.5h5" />
  </svg>
);

export const Pair = () => (
  <div style={{ display: "flex", gap: 14, maxWidth: 420, background: "var(--cc-bg)", padding: 16 }}>
    <ActionCard icon={ContactsIcon} label="Contacts" tone="blue" />
    <ActionCard icon={MovieIcon} label="Entertainment" tone="purple" />
  </div>
);

export const WithHint = () => (
  <div style={{ display: "flex", gap: 14, maxWidth: 420, background: "var(--cc-bg)", padding: 16 }}>
    <ActionCard icon={MedicineIcon} label="Medicines" tone="green" hint="Tap to open" />
    <ActionCard icon={MovieIcon} label="TV Shows" tone="red" hint="Coming soon" />
  </div>
);
