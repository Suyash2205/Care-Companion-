import * as React from "react";
import { ElderStatusCard } from "carecompanion-design-system";

export const Safe = () => (
  <div style={{ maxWidth: 420 }}>
    <ElderStatusCard name="Sunita" status="At Home | 15 mins ago" safe />
  </div>
);

export const Alert = () => (
  <div style={{ maxWidth: 420 }}>
    <ElderStatusCard name="Ramesh" status="Left home | 2 mins ago" safe={false} />
  </div>
);
