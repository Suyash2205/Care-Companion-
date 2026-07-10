import * as React from "react";
import { ContactCard } from "carecompanion-design-system";

const PersonIcon = (
  <svg viewBox="0 0 24 24" width="56" height="56" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
    <circle cx="12" cy="7" r="4" />
  </svg>
);

export const Grid = () => (
  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12, maxWidth: 420, background: "var(--cc-bg)", padding: 16 }}>
    <ContactCard name="Aarav" tone="blue" icon={PersonIcon} />
    <ContactCard name="Riya" tone="pink" icon={PersonIcon} />
    <ContactCard name="Meera" tone="green" icon={PersonIcon} />
    <ContactCard name="Rahul" tone="purple" icon={PersonIcon} />
  </div>
);
