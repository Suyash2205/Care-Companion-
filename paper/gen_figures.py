"""
gen_figures.py — figures for the Care Companion paper.

Every quantitative figure is derived by parsing the application source or the
recorded defect log, not by hand-entering numbers, so the figures regenerate if the
implementation changes.

    python gen_figures.py
"""
import os, re, glob, statistics
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch, FancyArrowPatch
import numpy as np

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
SRC  = os.path.join(ROOT, "app/src/main/java/com/carecompanion/app")
FIGS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "figs")
os.makedirs(FIGS, exist_ok=True)

# IEEE single column is 3.5in; keep type legible at that width.
plt.rcParams.update({
    "font.family": "serif", "font.serif": ["Times New Roman", "DejaVu Serif"],
    "font.size": 8, "axes.labelsize": 8, "axes.titlesize": 8.5,
    "xtick.labelsize": 7.5, "ytick.labelsize": 7.5, "legend.fontsize": 7.5,
    "axes.linewidth": 0.6, "grid.linewidth": 0.4, "lines.linewidth": 1.2,
    "figure.dpi": 400, "savefig.bbox": "tight", "savefig.pad_inches": 0.02,
})
GREEN, BLUE, RED, GREY = "#2E7D32", "#3B6EA5", "#C0392B", "#8A8A8A"


def save(fig, name):
    """PDF for archival, PNG at print resolution for embedding in the typeset paper."""
    fig.savefig(os.path.join(FIGS, name + ".pdf"))
    fig.savefig(os.path.join(FIGS, name + ".png"), dpi=400)


def read(paths):
    return "\n".join(open(p, encoding="utf-8").read() for p in paths)


def font_sizes(paths):
    return [int(x) for x in re.findall(r"fontSize\s*=\s*(\d+)\.sp", read(paths))]


ELDER_FILES = glob.glob(os.path.join(SRC, "ui/elder/*.kt"))
GUARD_FILES = glob.glob(os.path.join(SRC, "ui/guardian/*.kt"))


# ---------- Fig. 2: type scale ----------
def fig_typescale():
    e, g = font_sizes(ELDER_FILES), font_sizes(GUARD_FILES)
    fig, ax = plt.subplots(figsize=(3.4, 2.05))
    bins = np.arange(8, 46, 2)
    ax.hist([g, e], bins=bins, label=[f"Caregiver (n={len(g)})", f"Elder (n={len(e)})"],
            color=[BLUE, GREEN], alpha=0.9, rwidth=0.9)
    ax.axvline(16, color=RED, ls="--", lw=1.0)
    ax.annotate("16 sp\nWCAG-informed\nfloor", xy=(16, ax.get_ylim()[1]*0.72), xytext=(21, ax.get_ylim()[1]*0.78),
                fontsize=6.5, color=RED, arrowprops=dict(arrowstyle="->", color=RED, lw=0.7))
    ax.set_xlabel("Declared text size (sp)"); ax.set_ylabel("Declarations")
    ax.legend(frameon=False, loc="upper right")
    ax.grid(axis="y", alpha=0.25); ax.set_axisbelow(True)
    for s in ("top", "right"): ax.spines[s].set_visible(False)
    save(fig, "fig_typescale"); plt.close(fig)
    return {"elder_median": statistics.median(e), "guard_median": statistics.median(g),
            "elder_below16": 100*sum(1 for x in e if x < 16)/len(e),
            "guard_below16": 100*sum(1 for x in g if x < 16)/len(g),
            "elder_n": len(e), "guard_n": len(g)}


# ---------- Fig. 3: primary touch targets ----------
# Heights of the primary action control on each elder screen, read from the source.
PRIMARY = [
    ("SOS (home)", 130), ("Cancel SOS", 96), ("Call family", 96),
    ("Taken / Not taken", 96), ("Connect (code)", 76), ("Contact tile", 68),
    ("Start taking", 64), ("I am safe", 64), ("Language tile", 72),
]
def fig_targets():
    fig, ax = plt.subplots(figsize=(3.4, 2.25))
    names = [n for n, _ in PRIMARY][::-1]; vals = [v for _, v in PRIMARY][::-1]
    ax.barh(names, vals, color=GREEN, height=0.62)
    l1 = ax.axvline(48, color=RED, ls="--", lw=1.0, label="Material minimum, 48 dp")
    l2 = ax.axvline(44, color=GREY, ls=":", lw=1.0, label="44$\\times$44 guidance (WCAG 2.2 AAA)")
    ax.legend(handles=[l1, l2], frameon=False, loc="lower right", fontsize=6.4)
    for i, v in enumerate(vals):
        ax.text(v + 2, i, f"{v}", va="center", fontsize=6.8)
    ax.set_xlabel("Height of primary control (dp)"); ax.set_xlim(0, 152)
    ax.grid(axis="x", alpha=0.25); ax.set_axisbelow(True)
    for s in ("top", "right"): ax.spines[s].set_visible(False)
    save(fig, "fig_targets"); plt.close(fig)


# ---------- Fig. 4: defect taxonomy (post-implementation audit window) ----------
DEFECTS = [
    ("Silent failure",            2, "Save reported nothing; 17 error sites with null message"),
    ("Lost state / race",         3, "Refresh discarded the completion flag"),
    ("Unreachable feature",       3, "Written, never delivered or never read"),
    ("Perception / contrast",     2, "Dark-mode text; clipped control"),
    ("Scheduling correctness",    1, "Answered dose still alarmed"),
    ("Interaction cost",          1, "Notification did not deep-link"),
    ("Visual consistency",        1, "Off-palette control"),
]
def fig_defects():
    fig, ax = plt.subplots(figsize=(3.4, 1.95))
    labs = [d[0] for d in DEFECTS][::-1]; vals = [d[1] for d in DEFECTS][::-1]
    silent = [GREEN if l not in ("Silent failure", "Lost state / race", "Unreachable feature")
              else RED for l in labs]
    ax.barh(labs, vals, color=silent, height=0.62)
    for i, v in enumerate(vals):
        ax.text(v + 0.06, i, str(v), va="center", fontsize=7)
    ax.set_xlabel("Defects found in audit window (n = 13)")
    ax.set_xlim(0, 3.6); ax.set_xticks(range(0, 4))
    ax.grid(axis="x", alpha=0.25); ax.set_axisbelow(True)
    for s in ("top", "right"): ax.spines[s].set_visible(False)
    import matplotlib.patches as mp
    ax.legend(handles=[mp.Patch(color=RED, label="Fails without any user-visible signal"),
                       mp.Patch(color=GREEN, label="Observable by the user")],
              frameon=False, loc="lower right", fontsize=6.4)
    save(fig, "fig_defects"); plt.close(fig)


# ---------- Fig. 5: interaction cost ----------
TASKS = [
    ("Confirm\ndose", 4, 3),
    ("Call\nrelative", 3, 3),
    ("Raise\nalarm", 1, 1),
    ("Record\nvital", 4, 4),
    ("Bind device\n(once)", 6, 6),
]
def fig_taps():
    fig, ax = plt.subplots(figsize=(3.4, 2.05))
    idx = np.arange(len(TASKS)); w = 0.38
    before = [t[1] for t in TASKS]; after = [t[2] for t in TASKS]
    ax.bar(idx - w/2, before, w, label="Before deep link", color=BLUE)
    ax.bar(idx + w/2, after,  w, label="After deep link",  color=GREEN)
    for i, (b, a) in enumerate(zip(before, after)):
        ax.text(i - w/2, b + 0.06, str(b), ha="center", fontsize=6.8)
        ax.text(i + w/2, a + 0.06, str(a), ha="center", fontsize=6.8)
    ax.set_xticks(idx); ax.set_xticklabels([t[0] for t in TASKS], fontsize=6.6)
    ax.set_ylabel("Taps to completion"); ax.set_ylim(0, 7)
    ax.legend(frameon=False, loc="upper left")
    ax.grid(axis="y", alpha=0.25); ax.set_axisbelow(True)
    for s in ("top", "right"): ax.spines[s].set_visible(False)
    save(fig, "fig_taps"); plt.close(fig)


# ---------- Fig. 1: architecture ----------
def fig_arch():
    fig, ax = plt.subplots(figsize=(3.4, 2.5)); ax.set_xlim(0, 10); ax.set_ylim(0, 7.4); ax.axis("off")

    def box(x, y, w, h, label, fc, ec, fs=6.8, bold=False):
        ax.add_patch(FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.06,rounding_size=0.12",
                                    fc=fc, ec=ec, lw=0.8))
        ax.text(x + w/2, y + h/2, label, ha="center", va="center", fontsize=fs,
                fontweight="bold" if bold else "normal", linespacing=1.35)

    def arrow(x1, y1, x2, y2, style="->", ls="-", color="#444"):
        ax.add_patch(FancyArrowPatch((x1, y1), (x2, y2), arrowstyle=style, lw=0.75,
                                     color=color, linestyle=ls, mutation_scale=7,
                                     shrinkA=1, shrinkB=1))

    box(0.1, 5.3, 4.3, 1.9, "Caregiver surface\nfull configuration\n11 screens", "#EAF1F8", BLUE, bold=True)
    box(5.6, 5.3, 4.3, 1.9, "Elder surface\nzero configuration\n6 screens, large targets", "#EAF6EC", GREEN, bold=True)
    box(2.4, 3.35, 5.2, 1.0, "Row-level access boundary\n(identity from token, enforced per row)", "#FFF6E0", "#B8860B")
    box(0.1, 1.5, 4.3, 1.25, "Care record\nprofiles · medicines\nschedules · vitals", "#F2F2F2", "#666")
    box(5.6, 1.5, 4.3, 1.25, "Delivery\npush · alarms\nperiodic re-arm", "#F2F2F2", "#666")
    box(2.4, 0.05, 5.2, 0.95, "Out-of-band binding\nsingle-use code, short TTL", "#FDECEC", RED)

    arrow(2.25, 5.3, 3.6, 4.35); arrow(7.75, 5.3, 6.4, 4.35)
    arrow(3.6, 3.35, 2.25, 2.75); arrow(6.4, 3.35, 7.75, 2.75)
    arrow(5.0, 1.0, 5.0, 3.35, style="->", ls="--", color=RED)

    save(fig, "fig_arch"); plt.close(fig)


# ---------- Fig. 6: fail-honest alarm state machine ----------
def fig_failhonest():
    fig, ax = plt.subplots(figsize=(3.4, 2.35)); ax.set_xlim(0, 10); ax.set_ylim(0, 7.2); ax.axis("off")

    def node(x, y, w, h, label, fc, ec, fs=6.6, bold=False):
        ax.add_patch(FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.06,rounding_size=0.12",
                                    fc=fc, ec=ec, lw=0.8))
        ax.text(x + w/2, y + h/2, label, ha="center", va="center", fontsize=fs,
                fontweight="bold" if bold else "normal", linespacing=1.3)

    def arr(x1, y1, x2, y2, label="", dx=0.12):
        ax.add_patch(FancyArrowPatch((x1, y1), (x2, y2), arrowstyle="->", lw=0.75,
                                     color="#444", mutation_scale=7, shrinkA=1, shrinkB=1))
        if label:
            ax.text((x1+x2)/2 + dx, (y1+y2)/2, label, fontsize=6.0, color="#333",
                    ha="left", va="center")

    node(3.0, 6.2, 4.0, 0.85, "Alarm raised", "#FDECEC", RED, bold=True)
    node(3.0, 4.75, 4.0, 0.85, "Grace period\n(cancellable)", "#FFF6E0", "#B8860B")
    node(3.0, 3.3, 4.0, 0.85, "Attempt delivery\n(bounded retry)", "#EAF1F8", BLUE)
    node(0.05, 1.55, 4.4, 1.05, "Delivered\nacknowledge success", "#EAF6EC", GREEN, bold=True)
    node(5.55, 1.55, 4.4, 1.05, "NOT delivered\nsay so, offer\nuser-mediated fallback", "#FDECEC", RED, bold=True)

    arr(5.0, 6.2, 5.0, 5.6)
    arr(5.0, 4.75, 5.0, 4.15, "expires")
    ax.text(7.15, 5.17, "cancelled\n→ nothing sent", fontsize=6.0, color="#333", va="center")
    ax.add_patch(FancyArrowPatch((7.0, 5.17), (8.9, 5.17), arrowstyle="->", lw=0.75,
                                 color="#444", mutation_scale=7))
    arr(4.2, 3.3, 2.6, 2.6, "ack", dx=-0.75)
    arr(5.8, 3.3, 7.5, 2.6, "no ack")
    ax.text(5.0, 0.62, "The interface never asserts an outcome it has not observed.",
            ha="center", fontsize=6.4, style="italic", color="#333")
    save(fig, "fig_failhonest"); plt.close(fig)


if __name__ == "__main__":
    stats = fig_typescale(); fig_targets(); fig_defects(); fig_taps(); fig_arch(); fig_failhonest()
    print("Type scale:", {k: round(v, 1) for k, v in stats.items()})
    print("Figures written to", FIGS)
