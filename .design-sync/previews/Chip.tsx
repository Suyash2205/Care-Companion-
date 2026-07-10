import * as React from "react";
import { Chip } from "carecompanion-design-system";

export const Info = () => (
  <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
    <Chip>Qty 1</Chip>
    <Chip>Before Lunch</Chip>
    <Chip>With Water</Chip>
  </div>
);

export const TakenToggle = () => (
  <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
    <div style={{ display: "flex", gap: 10 }}>
      <Chip tone="danger">Not taken</Chip>
      <Chip tone="success" selected>Taken</Chip>
    </div>
    <div style={{ display: "flex", gap: 10 }}>
      <Chip tone="danger" selected>Not taken</Chip>
      <Chip tone="success">Taken</Chip>
    </div>
  </div>
);
