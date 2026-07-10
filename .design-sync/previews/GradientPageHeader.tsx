import * as React from "react";
import { GradientPageHeader } from "carecompanion-design-system";

export const ManageContacts = () => (
  <GradientPageHeader
    title="Manage Contacts"
    subtitle="Contacts synced to Sunita's phone"
  />
);

export const Medicines = () => (
  <GradientPageHeader
    tone="medicines"
    title="Manage Medicines"
    subtitle="3 medicines scheduled today"
  />
);

export const Schedule = () => (
  <GradientPageHeader
    tone="schedule"
    title="Daily Schedule"
    subtitle="Today, 11 July"
  />
);

export const WellnessSos = () => (
  <GradientPageHeader
    tone="sos"
    title="Wellness & SOS"
    subtitle="Emergency settings for Sunita"
  />
);
