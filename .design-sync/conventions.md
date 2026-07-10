# CareCompanion design system — build conventions

This library mirrors the CareCompanion Android app (a guardian/elder care app). Two audiences share one palette: **guardian screens** use normal type sizes; **elder screens** use oversized type (18–46px), big touch targets, and high contrast. Prefer the elder-sized variants (`size="lg"`/`"xl"`, `ActionCard`, `SosButton`) for anything an elderly user touches.

## Setup

No provider is required. Components style themselves via CSS classes; make sure `styles.css` is loaded (it defines the `--cc-*` tokens and all `cc-*` classes). Page backgrounds should be `var(--cc-bg)` (#F4F6F4 soft sage), never pure white — white is reserved for cards.

## Styling idiom

Components carry their own classes — don't restyle them. For your own layout glue use inline styles or utility CSS referencing the tokens:

- Colors: `--cc-green` (brand #2E8540), `--cc-green-dark`, `--cc-green-tint`, `--cc-bg`, `--cc-surface`, `--cc-text`, `--cc-text-sub`, `--cc-danger`, `--cc-danger-tint`, `--cc-info`
- Gradients: `--cc-grad-primary`, `--cc-grad-medicines`, `--cc-grad-schedule`, `--cc-grad-sos`
- Pastel tile pairs: `--cc-pastel-{blue,purple,green,amber,red,pink,teal}` with matching `-fg` foregrounds
- Radii: `--cc-radius-sm` 10px → `--cc-radius-3xl` 24px (cards are 20–24px, inputs/buttons 14–16px)
- Shadows: `--cc-shadow-card`, `--cc-shadow-raised`, `--cc-shadow-sos`
- Fonts: `--cc-font` (system sans, default), `--cc-font-display` (serif — only for the Wordmark)

Red (`--cc-grad-sos`, `--cc-danger`) is exclusively for emergency/destructive UI. Blue `--cc-info` marks the elder medicine-flow progression actions ("Start taking", "Next").

## Components (window.CareCompanionDS)

`Button`, `SosButton`, `TextField`, `SectionHeader`, `GradientPageHeader`, `Card`, `StatusPill`, `ElderStatusCard`, `AvatarCard`, `ActionCard`, `ContactCard`, `MedicineListItem`, `Chip`, `BottomNav`, `DrawerItem`, `ProfileChoiceCard`, `Wordmark`. Icons are passed in as `ReactNode` (inline SVG, 1.6–2px stroke, `currentColor`) — the library ships none.

## Idiomatic screen skeleton

```jsx
import { GradientPageHeader, Card, SectionHeader, MedicineListItem, Button, BottomNav } from "carecompanion-design-system";

<div style={{ background: "var(--cc-bg)", minHeight: "100%", display: "flex", flexDirection: "column" }}>
  <GradientPageHeader tone="medicines" title="Manage Medicines" subtitle="3 scheduled today" />
  <div style={{ flex: 1, padding: 16, display: "flex", flexDirection: "column", gap: 12 }}>
    <SectionHeader>Today's Medicines</SectionHeader>
    <Card>
      <MedicineListItem name="1. Lorazepam 3mg" qty="Qty 1" tone="blue" />
    </Card>
    <Button fullWidth>Save Medicine</Button>
  </div>
  <BottomNav items={[/* {label, icon} */]} active="Home" />
</div>
```
