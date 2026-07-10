import * as React from "react";
import { ProfileChoiceCard } from "carecompanion-design-system";

const ElderIcon = (
  <svg viewBox="0 0 24 24" width="36" height="36" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
    <circle cx="12" cy="7" r="4" />
  </svg>
);

export const Pair = () => (
  <div style={{ display: "flex", gap: 12, maxWidth: 420 }}>
    <ProfileChoiceCard name="Sunita" icon={ElderIcon} selected />
    <ProfileChoiceCard name="Ramesh" icon={ElderIcon} />
  </div>
);
