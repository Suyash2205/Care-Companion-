"""Diagrams for the Task 18 (integration) and Task 19 (deployment) submissions.

Drawn from the repository's actual structure, not from a sketch: the component
names match package names, the Edge Function names match supabase/functions/,
and the table names match supabase/migrations/.
"""
import os
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch, FancyArrowPatch

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..",
                   "..", "Desktop", "CareCompanion-Tasks-15-19", "figures")
OUT = os.path.abspath(OUT)
os.makedirs(OUT, exist_ok=True)

GREEN, BLUE, RED, GREY, AMBER = "#2E7D32", "#3B6EA5", "#C0392B", "#8A8A8A", "#B8860B"
plt.rcParams.update({"font.family": "DejaVu Sans"})


def box(ax, x, y, w, h, label, fc, ec, fs=7.4, bold=False):
    ax.add_patch(FancyBboxPatch((x, y), w, h,
                 boxstyle="round,pad=0.08,rounding_size=0.14",
                 fc=fc, ec=ec, lw=1.0))
    ax.text(x + w / 2, y + h / 2, label, ha="center", va="center", fontsize=fs,
            fontweight="bold" if bold else "normal", linespacing=1.35)


def arrow(ax, p1, p2, label="", fs=6.4, off=(0, 0.16), color="#444", style="-|>"):
    ax.add_patch(FancyArrowPatch(p1, p2, arrowstyle=style, lw=0.9, color=color,
                                 mutation_scale=9, shrinkA=2, shrinkB=2))
    if label:
        ax.text((p1[0] + p2[0]) / 2 + off[0], (p1[1] + p2[1]) / 2 + off[1], label,
                ha="center", va="center", fontsize=fs, color="#222")


def save(fig, name):
    for ext in ("png", "pdf"):
        fig.savefig(os.path.join(OUT, f"{name}.{ext}"), dpi=300, bbox_inches="tight")
    plt.close(fig)
    print("  wrote", name)


def fig_interfaces():
    """Task 18: the interface between every pair of parts, with the protocol named."""
    fig, ax = plt.subplots(figsize=(7.6, 5.3))
    ax.set_xlim(0, 14.8); ax.set_ylim(0, 10); ax.axis("off")

    ax.text(7.0, 9.6, "CareCompanion — component interfaces",
            ha="center", fontsize=10, fontweight="bold")

    # Clients
    box(ax, 0.3, 7.2, 3.5, 1.5,
        "ELDER SURFACE\nui.elder  (7 files)\nCompose, 4 languages", "#EAF6EC", GREEN, bold=False)
    box(ax, 0.3, 5.1, 3.5, 1.5,
        "GUARDIAN SURFACE\nui.guardian  (27 files)\nCompose + Navigation", "#EAF1F8", BLUE)
    box(ax, 0.3, 2.9, 3.5, 1.6,
        "DEVICE SERVICES\nreminder.*  AlarmManager\nnotify.*  FCM receiver\nvoice.*  mic + TTS", "#FFF6E0", AMBER)

    # App internals
    box(ax, 4.6, 5.4, 3.0, 3.3,
        "data.repo\n(8 repositories)\n\ndata.remote\nRetrofit +\nAuthInterceptor\n\ndata.repo.OutboxStore\noffline queue", "#F4F4F4", GREY)

    # Backend
    box(ax, 8.6, 7.3, 5.1, 1.4,
        "Supabase PostgREST\n17 tables · 37 RLS policies · 26 functions", "#EAF6EC", GREEN)
    box(ax, 8.6, 5.3, 5.1, 1.4,
        "Edge Functions (Deno)\nfunctions/push · functions/voice", "#EAF1F8", BLUE)
    box(ax, 8.6, 3.3, 2.4, 1.4, "Firebase\nAuth + FCM", "#FDECEC", RED)
    box(ax, 11.3, 3.3, 2.4, 1.4, "Sarvam AI\nASR + LLM", "#FDECEC", RED)
    box(ax, 8.6, 1.3, 5.1, 1.3, "pg_cron\nscan_missed_doses() every 15 min", "#FFF6E0", AMBER)

    # Interfaces
    arrow(ax, (3.8, 7.9), (4.6, 7.4), "StateFlow", off=(0.1, 0.3))
    arrow(ax, (3.8, 5.9), (4.6, 6.4), "StateFlow", off=(0.1, -0.3))
    arrow(ax, (3.8, 3.7), (4.6, 5.4), "PendingIntent\n/ deep link", off=(-0.7, 0.1))
    arrow(ax, (7.6, 7.6), (8.6, 7.9), "HTTPS/JSON\nBearer JWT", off=(0.0, 0.42))
    arrow(ax, (7.6, 6.3), (8.6, 6.1), "HTTPS/JSON", off=(0.0, 0.3))
    arrow(ax, (9.8, 5.3), (9.8, 4.7), "", off=(0, 0))
    arrow(ax, (12.5, 5.3), (12.5, 4.7), "REST", off=(0.5, 0.0), fs=6.0)
    arrow(ax, (9.8, 3.3), (9.8, 2.6), "", off=(0, 0))
    # trigger path runs down the right margin so it crosses nothing
    ax.add_patch(FancyArrowPatch((14.4, 7.3), (14.4, 2.0), arrowstyle="-", lw=0.9, color="#777"))
    ax.add_patch(FancyArrowPatch((14.4, 2.0), (14.4, 2.6), arrowstyle="-|>", lw=0.9,
                                 color="#777", mutation_scale=9))
    ax.text(14.25, 4.8, "trigger notify_push()", fontsize=6.0, color="#555",
            rotation=90, ha="right", va="center")
    arrow(ax, (8.6, 3.9), (3.8, 3.9), "data-only FCM push", off=(0, 0.28), color=RED)

    ax.text(7, 0.45,
            "Every client-to-server call carries the Firebase ID token; row-level security, not the client, "
            "decides what each account may read or write.",
            ha="center", fontsize=6.6, style="italic", color="#333")
    save(fig, "fig-18-interfaces")


def fig_dose_trace():
    """Task 18: one complete operation, input to output."""
    fig, ax = plt.subplots(figsize=(7.6, 4.0))
    ax.set_xlim(0, 14); ax.set_ylim(0, 7.6); ax.axis("off")
    ax.text(7, 7.2, "End-to-end trace: one medicine dose, input to output",
            ha="center", fontsize=10, fontweight="bold")

    steps = [
        (0.15, "INPUT\nGuardian saves\nan 08:00\nschedule", "#EAF1F8", BLUE),
        (2.45, "Row inserted in\nmedicine_\nschedules\nvia PostgREST", "#EAF6EC", GREEN),
        (4.75, "DailyArmWorker\narms the alarm\nsetExactAnd-\nAllowWhileIdle", "#FFF6E0", AMBER),
        (7.05, "08:00 —\nReminder-\nReceiver posts\nthe notification", "#FFF6E0", AMBER),
        (9.35, "Elder taps,\nor speaks\nthe answer\n(voice function)", "#EAF6EC", GREEN),
        (11.65, "OUTPUT\nadherence_logs\nrow → guardian\ndashboard", "#EAF1F8", BLUE),
    ]
    for x, label, fc, ec in steps:
        box(ax, x, 3.3, 2.0, 2.3, label, fc, ec, fs=5.9)
    for i in range(len(steps) - 1):
        arrow(ax, (steps[i][0] + 2.0, 4.45), (steps[i + 1][0], 4.45))

    box(ax, 9.5, 0.6, 4.15, 1.5,
        "No answer by the grace period →\nscan_missed_doses() raises an alert →\nnotify_push() → guardian's phone",
        "#FDECEC", RED, fs=6.3)
    arrow(ax, (10.3, 3.3), (11.0, 2.1), "", color=RED)

    ax.text(0.15, 1.5, "Validation points:\n• RLS rejects a write for an elder you do not manage\n"
                      "• Duplicate response is idempotent on (schedule, date)\n"
                      "• Offline writes queue in OutboxStore and replay",
            fontsize=6.4, va="center", color="#333")
    save(fig, "fig-18-dose-trace")


def fig_deployment():
    """Task 19: what is installed where, and how it starts."""
    fig, ax = plt.subplots(figsize=(7.6, 5.0))
    ax.set_xlim(0, 14); ax.set_ylim(0, 9.4); ax.axis("off")
    ax.text(7, 9.0, "CareCompanion — deployment diagram",
            ha="center", fontsize=10, fontweight="bold")

    # Device tier
    ax.add_patch(FancyBboxPatch((0.2, 4.3), 5.2, 4.1,
                 boxstyle="round,pad=0.1,rounding_size=0.15",
                 fc="none", ec=GREY, lw=0.9, ls="--"))
    ax.text(2.8, 8.15, "DEVICE TIER — Android 7.0+ (API 24–36)",
            ha="center", fontsize=7.2, fontweight="bold", color="#333")
    box(ax, 0.5, 6.3, 2.2, 1.5, "Elder's phone\napp-release.aab\nv1.3 (code 3)", "#EAF6EC", GREEN, fs=6.3)
    box(ax, 3.0, 6.3, 2.2, 1.5, "Guardian's phone\nsame binary, role\nchosen at login", "#EAF1F8", BLUE, fs=6.3)
    box(ax, 0.5, 4.6, 4.7, 1.4,
        "Bundled at build time:\ngoogle-services.json, SUPABASE_URL,\n"
        "anon key, GOOGLE_WEB_CLIENT_ID", "#F4F4F4", GREY, fs=6.0)

    # Distribution
    box(ax, 6.15, 6.5, 2.3, 1.3, "Google Play\nApp Signing\n(upload key)", "#FFF6E0", AMBER, fs=6.6)

    # Cloud tier
    ax.add_patch(FancyBboxPatch((8.8, 2.0), 5.0, 6.4,
                 boxstyle="round,pad=0.1,rounding_size=0.15",
                 fc="none", ec=GREY, lw=0.9, ls="--"))
    ax.text(11.3, 8.15, "CLOUD TIER — fully managed",
            ha="center", fontsize=7.2, fontweight="bold", color="#333")
    box(ax, 9.1, 6.5, 4.4, 1.3, "Supabase project\nPostgreSQL 15 + PostgREST", "#EAF6EC", GREEN, fs=6.6)
    box(ax, 9.1, 5.0, 4.4, 1.2, "Edge Functions (Deno)\npush · voice", "#EAF1F8", BLUE, fs=6.6)
    box(ax, 9.1, 3.6, 4.4, 1.1, "pg_cron schedules", "#FFF6E0", AMBER, fs=6.6)
    box(ax, 9.1, 2.3, 2.1, 1.1, "Firebase", "#FDECEC", RED, fs=6.6)
    box(ax, 11.4, 2.3, 2.1, 1.1, "Sarvam AI", "#FDECEC", RED, fs=6.6)

    arrow(ax, (5.45, 7.4), (6.15, 7.4), "install", off=(0, 0.3), fs=6.0)
    arrow(ax, (8.45, 7.15), (9.1, 7.15), "")
    arrow(ax, (5.2, 5.3), (9.1, 5.6), "HTTPS 443 only", off=(0, 0.3), fs=6.0)

    ax.text(7, 1.3,
            "Start-up: install → Google Sign-In → guardian creates the elder profile and shares a 6-digit code →\n"
            "elder's phone redeems the code once and is bound to that profile. No IP addresses or ports to configure.",
            ha="center", fontsize=6.5, color="#333")
    save(fig, "fig-19-deployment")


if __name__ == "__main__":
    fig_interfaces()
    fig_dose_trace()
    fig_deployment()
    print("Figures written to", OUT)
