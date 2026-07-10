import * as React from "react";
import { TextField } from "carecompanion-design-system";

const PersonIcon = (
  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
    <circle cx="12" cy="7" r="4" />
  </svg>
);

const PhoneIcon = (
  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.13.96.36 1.9.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.91.34 1.85.57 2.81.7A2 2 0 0 1 22 16.92z" />
  </svg>
);

export const Basic = () => (
  <div style={{ maxWidth: 420, paddingTop: 10 }}>
    <TextField label="Medicine name" placeholder="e.g. Lorazepam 3mg" />
  </div>
);

export const WithIcon = () => (
  <div style={{ display: "flex", flexDirection: "column", gap: 20, maxWidth: 420, paddingTop: 10 }}>
    <TextField label="Contact name" leadingIcon={PersonIcon} defaultValue="Aarav Sharma" />
    <TextField label="Phone number" leadingIcon={PhoneIcon} placeholder="+91 98765 43210" />
  </div>
);
