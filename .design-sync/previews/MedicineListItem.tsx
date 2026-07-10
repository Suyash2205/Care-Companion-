import * as React from "react";
import { MedicineListItem } from "carecompanion-design-system";

const PillIcon = (
  <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="m10.5 20.5 10-10a4.95 4.95 0 1 0-7-7l-10 10a4.95 4.95 0 1 0 7 7z" />
    <path d="m8.5 8.5 7 7" />
  </svg>
);

export const TodaysList = () => (
  <div style={{ display: "flex", flexDirection: "column", gap: 10, maxWidth: 420 }}>
    <MedicineListItem name="1. Buprenorphine/Naloxone Strip" qty="Qty 1" tone="amber" icon={PillIcon} />
    <MedicineListItem name="2. Lorazepam 3mg" qty="Qty 1" tone="blue" icon={PillIcon} />
    <MedicineListItem name="3. Alprazolam ODT" qty="Qty 2" tone="purple" icon={PillIcon} />
  </div>
);
