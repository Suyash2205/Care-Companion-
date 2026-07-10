import * as React from "react";
import { StatusPill } from "carecompanion-design-system";

export const Status = () => (
  <StatusPill>Status: At home</StatusPill>
);

export const Badge = () => (
  <div style={{ display: "flex", gap: 12, alignItems: "center" }}>
    <StatusPill variant="badge">SAFE</StatusPill>
    <StatusPill variant="alert" small>ALERT</StatusPill>
  </div>
);

export const Small = () => (
  <StatusPill small>At Home | 15 mins ago</StatusPill>
);
