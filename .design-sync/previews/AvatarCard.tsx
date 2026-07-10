import * as React from "react";
import { AvatarCard } from "carecompanion-design-system";

export const Default = () => (
  <div style={{ maxWidth: 420, background: "var(--cc-bg)", padding: 16 }}>
    <AvatarCard name="Sunita" status="Status: At home" />
  </div>
);
