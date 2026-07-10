import * as React from "react";
import { Button } from "carecompanion-design-system";

export const Primary = () => (
  <Button fullWidth>Save Medicine</Button>
);

export const Variants = () => (
  <div style={{ display: "flex", flexWrap: "wrap", gap: 12, alignItems: "center" }}>
    <Button>Add Contact</Button>
    <Button variant="solid">Manage Sunita</Button>
    <Button variant="info">Start taking</Button>
    <Button variant="danger">Trigger SOS</Button>
    <Button variant="outline">Review again</Button>
    <Button variant="ghost">Add Profile</Button>
  </div>
);

export const ElderSizes = () => (
  <div style={{ display: "flex", flexDirection: "column", gap: 12, maxWidth: 420 }}>
    <Button size="lg" variant="info" fullWidth>Start taking</Button>
    <Button size="xl" fullWidth>Finish</Button>
  </div>
);

export const Disabled = () => (
  <Button fullWidth disabled>Save Medicine</Button>
);
