import * as React from "react";
import { SectionHeader, Card } from "carecompanion-design-system";

export const Default = () => (
  <div style={{ display: "flex", flexDirection: "column", gap: 8, maxWidth: 420 }}>
    <SectionHeader>Elder Details</SectionHeader>
    <Card>Sunita, 72 — Pune</Card>
    <SectionHeader>Emergency Contacts</SectionHeader>
    <Card>Aarav Sharma · +91 98765 43210</Card>
  </div>
);
