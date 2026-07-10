import * as React from "react";

export interface ContactCardProps extends React.HTMLAttributes<HTMLDivElement> {
  /** Contact's name (elder-sized 24px, single line). */
  name: string;
  /** Pastel background of the avatar block. */
  tone?: "blue" | "pink" | "green" | "purple" | "amber" | "teal";
  /** Avatar content — an icon or emoji; a photo URL wins if provided. */
  icon?: React.ReactNode;
  photoSrc?: string;
  /** Call-button handler (round green phone button). */
  onCall?: () => void;
}

const toneBg: Record<string, string> = {
  blue: "var(--cc-pastel-blue)",
  pink: "var(--cc-pastel-pink)",
  green: "var(--cc-pastel-green)",
  purple: "var(--cc-pastel-purple)",
  amber: "var(--cc-pastel-amber)",
  teal: "var(--cc-pastel-teal)",
};

const PhoneIcon = (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.13.96.36 1.9.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.91.34 1.85.57 2.81.7A2 2 0 0 1 22 16.92z" />
  </svg>
);

/**
 * Elder contacts grid card — pastel avatar block on top, round green call
 * button and the contact's name below.
 */
export function ContactCard({ name, tone = "blue", icon, photoSrc, onCall, className, ...rest }: ContactCardProps) {
  return (
    <div className={["cc-contact-card", className ?? ""].filter(Boolean).join(" ")} {...rest}>
      <div className="cc-contact-card__avatar" style={{ background: toneBg[tone] }}>
        {photoSrc ? <img src={photoSrc} alt={name} /> : icon ?? name.charAt(0)}
      </div>
      <div className="cc-contact-card__row">
        <button type="button" className="cc-contact-card__call" onClick={onCall} aria-label={`Call ${name}`}>
          {PhoneIcon}
        </button>
        <span className="cc-contact-card__name">{name}</span>
      </div>
    </div>
  );
}
