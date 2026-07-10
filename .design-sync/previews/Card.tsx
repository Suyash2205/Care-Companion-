import * as React from "react";
import { Card, SectionHeader, MedicineListItem, Button } from "carecompanion-design-system";

export const Basic = () => (
  <div style={{ maxWidth: 420, background: "var(--cc-bg)", padding: 16 }}>
    <Card>
      <SectionHeader>Today's Medicines</SectionHeader>
      <div style={{ display: "flex", flexDirection: "column", gap: 10, marginTop: 10 }}>
        <MedicineListItem name="1. Buprenorphine Strip" qty="Qty 1" tone="amber" />
        <MedicineListItem name="2. Lorazepam 3mg" qty="Qty 1" tone="blue" />
      </div>
      <div style={{ marginTop: 14 }}>
        <Button variant="info" fullWidth>Start taking</Button>
      </div>
    </Card>
  </div>
);
