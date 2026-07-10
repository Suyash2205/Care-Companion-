import * as React from "react";
import { SosButton } from "carecompanion-design-system";

export const Default = () => (
  <div style={{ maxWidth: 420 }}>
    <SosButton />
  </div>
);

export const Hindi = () => (
  <div style={{ maxWidth: 420 }}>
    <SosButton hint="आपातकाल के लिए दबाकर रखें" />
  </div>
);
