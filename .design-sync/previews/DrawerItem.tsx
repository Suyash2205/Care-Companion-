import * as React from "react";
import { DrawerItem, SectionHeader } from "carecompanion-design-system";

const MedIcon = (
  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <rect x="3" y="8" width="18" height="12" rx="2" />
    <path d="M8 8V6a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
    <path d="M12 11v5" />
    <path d="M9.5 13.5h5" />
  </svg>
);
const HeartIcon = (
  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M22 12h-4l-3 9L9 3l-3 9H2" />
  </svg>
);
const HomeIcon = (
  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
    <path d="M9 22V12h6v10" />
  </svg>
);

export const Menu = () => (
  <div style={{ maxWidth: 260, background: "#fff", borderRadius: 16, padding: "12px 8px", display: "flex", flexDirection: "column", gap: 3 }}>
    <div style={{ padding: "0 16px 6px" }}>
      <SectionHeader>Menu</SectionHeader>
    </div>
    <DrawerItem icon={MedIcon} label="Medicines" selected />
    <DrawerItem icon={HeartIcon} label="Vitals" />
    <DrawerItem icon={HomeIcon} label="Home" />
  </div>
);
