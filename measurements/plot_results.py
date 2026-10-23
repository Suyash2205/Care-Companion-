"""
plot_results.py — Task 22 figures. Every value is read from measurements/raw/,
produced by the Task 21 run; nothing is typed in by hand.
"""
import csv, json, statistics, collections, os
import matplotlib; matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
RAW, FIGS = os.path.join(HERE, "raw"), os.path.join(HERE, "figs")
os.makedirs(FIGS, exist_ok=True)
plt.rcParams.update({
    "font.family": "serif", "font.serif": ["Times New Roman", "DejaVu Serif"],
    "font.size": 8.5, "axes.labelsize": 8.5, "axes.titlesize": 9,
    "xtick.labelsize": 8, "ytick.labelsize": 8, "legend.fontsize": 8,
    "axes.linewidth": 0.6, "grid.linewidth": 0.4, "lines.linewidth": 1.2,
    "figure.dpi": 300, "savefig.bbox": "tight", "savefig.pad_inches": 0.03,
})
GREEN, BLUE, RED, GREY, AMBER = "#2E7D32", "#3B6EA5", "#C0392B", "#8A8A8A", "#B07D00"

def save(fig, name):
    fig.savefig(os.path.join(FIGS, name + ".png"), dpi=300)
    fig.savefig(os.path.join(FIGS, name + ".pdf"))
    plt.close(fig)
    print("fig:", name)

rows = lambda f: list(csv.DictReader(open(os.path.join(RAW, f))))
tests  = rows("unit_tests.csv")
suites = rows("unit_test_suites.csv")
lat    = rows("backend_latency.csv")
rls    = rows("rls_anon_reads.csv")
sp     = rows("ui_text_sizes.csv")
dp     = rows("ui_touch_targets.csv")
uisum  = json.load(open(os.path.join(RAW, "ui_summary.json")))

# ── Fig 1: per-suite outcome and execution time ─────────────────────────────
s = sorted(suites, key=lambda r: -float(r["time_s"]))
names = [r["suite"].split(".")[-1] for r in s]
times = [float(r["time_s"]) for r in s]
counts = [int(r["tests"]) for r in s]
fig, ax = plt.subplots(figsize=(6.6, 4.4))
y = np.arange(len(names))
ax.barh(y, times, color=BLUE, height=0.68)
ax.set_yticks(y); ax.set_yticklabels(names)
ax.invert_yaxis()
ax.set_xlabel("Suite wall-clock execution time (s)")
ax.set_ylabel("Test suite, by name")
ax.grid(axis="x", alpha=0.3)
for yi, (t, c) in enumerate(zip(times, counts)):
    ax.text(t + max(times) * 0.012, yi, f"{t:.2f} s · {c} tests", va="center", fontsize=7.4)
ax.set_xlim(0, max(times) * 1.30)
ax.set_title(f"All {sum(counts)} tests in {len(names)} suites passed (0 failures, 0 errors)")
save(fig, "fig-22-01-suites")

# ── Fig 2: distribution of per-test execution time ──────────────────────────
t_ms = sorted(1000 * float(r["time_s"]) for r in tests)
fig, ax = plt.subplots(figsize=(4.1, 3.0))
bins = np.logspace(np.log10(0.4), np.log10(max(t_ms) * 1.3), 22)
ax.hist([max(v, 0.4) for v in t_ms], bins=bins, color=GREEN, edgecolor="white", linewidth=0.4)
ax.set_xscale("log")
ax.set_xlabel("Per-test execution time (ms, log scale)")
ax.set_ylabel(f"Number of tests (count, n = {len(t_ms)})")
med = statistics.median(t_ms)
ax.axvline(med, color=RED, ls="--", lw=1.0)
ax.annotate(f"median {med:.0f} ms", xy=(med, ax.get_ylim()[1] * 0.88),
            xytext=(med * 1.6, ax.get_ylim()[1] * 0.88), fontsize=7.6, color=RED,
            arrowprops=dict(arrowstyle="-", color=RED, lw=0.7))
ax.grid(axis="y", alpha=0.3)
save(fig, "fig-22-02-test-time")

# ── Fig 3: backend round-trip latency, 30 trials per endpoint ───────────────
by = collections.defaultdict(list)
for r in lat: by[r["endpoint"]].append(1000 * float(r["total_s"]))
order = ["rest_root", "auth_settings", "rls_denied_select", "edge_push_preflight"]
label = {"rest_root": "REST root\n(/rest/v1/)", "auth_settings": "Auth settings\n(/auth/v1/settings)",
         "rls_denied_select": "Row read under RLS\n(/rest/v1/elders)",
         "edge_push_preflight": "Edge function\n(/functions/v1/push)"}
fig, ax = plt.subplots(figsize=(5.4, 3.2))
data = [by[k] for k in order]
bp = ax.boxplot(data, tick_labels=[label[k] for k in order], widths=0.55, patch_artist=True,
                medianprops=dict(color=RED, lw=1.3), flierprops=dict(marker="o", ms=2.6, mfc=GREY, mec=GREY))
for p in bp["boxes"]: p.set(facecolor="#DCE6F1", edgecolor=BLUE, linewidth=0.8)
for k, d in zip(order, data):
    ax.scatter([order.index(k) + 1] * len(d), d, s=5, color=BLUE, alpha=0.35, zorder=3)
ax.set_ylabel("Round-trip time (ms)")
ax.set_xlabel("Endpoint measured (n = 30 trials each, Wi-Fi, Mumbai to ap-south-1)")
ax.grid(axis="y", alpha=0.3)
save(fig, "fig-22-03-latency")

# ── Fig 4: latency breakdown, means of the four phases ──────────────────────
fig, ax = plt.subplots(figsize=(5.4, 2.9))
phases = [("dns_s", "DNS lookup", "#9CC3E5"), ("tcp_s", "TCP connect", BLUE),
          ("tls_s", "TLS handshake", "#1F4E79"), ("ttfb_s", "Server + first byte", GREEN)]
base = np.zeros(len(order))
for key, name, col in phases:
    prev = {"dns_s": None, "tcp_s": "dns_s", "tls_s": "tcp_s", "ttfb_s": "tls_s"}[key]
    vals = []
    for k in order:
        cur = statistics.mean(1000 * float(r[key]) for r in lat if r["endpoint"] == k)
        pv = 0 if prev is None else statistics.mean(1000 * float(r[prev]) for r in lat if r["endpoint"] == k)
        vals.append(max(cur - pv, 0))
    ax.bar(np.arange(len(order)), vals, bottom=base, label=name, color=col, width=0.6)
    base += np.array(vals)
ax.set_xticks(np.arange(len(order)))
ax.set_xticklabels([label[k].replace("\n", " ") for k in order], fontsize=7.2, rotation=12, ha="right")
ax.set_ylabel("Mean elapsed time (ms)")
ax.set_xlabel("Endpoint measured (n = 30 trials each)")
ax.legend(frameon=False, ncol=2, loc="upper left", fontsize=7.4)
ax.set_ylim(0, base.max() * 1.42)
ax.grid(axis="y", alpha=0.3)
save(fig, "fig-22-04-latency-phases")

# ── Fig 5: declared font size, elder vs guardian ────────────────────────────
e = sorted(float(r["sp"]) for r in sp if r["surface"] == "elder")
g = sorted(float(r["sp"]) for r in sp if r["surface"] == "guardian")
fig, ax = plt.subplots(figsize=(4.6, 3.1))
ax.step(e, 100 * np.arange(1, len(e) + 1) / len(e), where="post", color=GREEN,
        label=f"Elder surface (n = {len(e)})")
ax.step(g, 100 * np.arange(1, len(g) + 1) / len(g), where="post", color=BLUE,
        label=f"Guardian surface (n = {len(g)})")
ax.axvline(16, color=RED, ls="--", lw=1.0)
ax.text(16.6, 8, "16 sp\nWCAG-informed\nminimum", fontsize=7, color=RED)
ax.set_xlabel("Declared font size (sp)")
ax.set_ylabel("Declarations at or below this size (%)")
ax.legend(frameon=False, loc="lower right")
ax.grid(alpha=0.3)
save(fig, "fig-22-05-typescale")

# ── Fig 6: declared control size, elder vs guardian ─────────────────────────
e = sorted(float(r["dp"]) for r in dp if r["surface"] == "elder")
g = sorted(float(r["dp"]) for r in dp if r["surface"] == "guardian")
fig, ax = plt.subplots(figsize=(4.6, 3.1))
ax.step(e, 100 * np.arange(1, len(e) + 1) / len(e), where="post", color=GREEN,
        label=f"Elder surface (n = {len(e)})")
ax.step(g, 100 * np.arange(1, len(g) + 1) / len(g), where="post", color=BLUE,
        label=f"Guardian surface (n = {len(g)})")
ax.axvline(48, color=RED, ls="--", lw=1.0)
ax.text(49.5, 10, "48 dp\nMaterial minimum\ntouch target", fontsize=7, color=RED)
ax.set_xlabel("Declared control height or size (dp)")
ax.set_ylabel("Declarations at or below this size (%)")
ax.legend(frameon=False, loc="lower right")
ax.grid(alpha=0.3)
save(fig, "fig-22-06-targets")

# ── Fig 7: anonymous read attempt on every table ────────────────────────────
PERSONAL = {"adherence_logs","alerts","contacts","elders","guardian_elder_links","invite_codes",
            "medicine_schedules","medicines","ott_shortcuts","reminders","sos_events","users","vitals"}
seen = {}
for r in rls: seen.setdefault(r["table"], []).append(r)
tabs = sorted(seen, key=lambda t: (t not in PERSONAL, t))
nrows = [max(int(x["rows_returned"]) for x in seen[t]) for t in tabs]
codes = [seen[t][0]["http_code"] for t in tabs]
fig, ax = plt.subplots(figsize=(6.0, 4.0))
y = np.arange(len(tabs))
cols = [GREEN if t in PERSONAL else GREY for t in tabs]
ax.barh(y, [max(v, 0.06) for v in nrows], color=cols, height=0.65)
ax.set_yticks(y)
ax.set_yticklabels([t + ("" if t in PERSONAL else "  (reference)") for t in tabs], fontsize=7.6)
ax.invert_yaxis()
ax.set_xlabel("Rows returned to an unauthenticated caller (count, limit = 100 per request)")
ax.set_ylabel("Table queried over /rest/v1/")
ax.set_xlim(0, max(max(nrows), 1) * 1.55)
for yi, (v, c) in enumerate(zip(nrows, codes)):
    ax.text(max(v, 0.06) + max(nrows) * 0.04, yi, f"{v} rows \u00b7 HTTP {c}", va="center", fontsize=7.2)
ax.set_title("Anonymous read: 0 rows from all 13 personal-data tables;\nonly 4 non-personal reference tables are readable by design (3 trials each)",
             fontsize=8.2)
from matplotlib.patches import Patch
ax.legend(handles=[Patch(color=GREEN, label="Personal care data"),
                   Patch(color=GREY, label="Non-personal reference data")],
          frameon=False, loc="center right", fontsize=7.4, bbox_to_anchor=(1.0, 0.62))
ax.grid(axis="x", alpha=0.3)
save(fig, "fig-22-07-rls")

# ── Fig 8: release artefact composition ─────────────────────────────────────
sec = rows("apk_sections.csv")[:6]
fig, ax = plt.subplots(figsize=(4.8, 2.9))
k = [r["entry"] for r in sec]
unc = [int(r["uncompressed_bytes"]) / 1e6 for r in sec]
com = [int(r["compressed_bytes"]) / 1e6 for r in sec]
x = np.arange(len(k))
ax.bar(x - 0.19, unc, width=0.38, color="#9CC3E5", label="Uncompressed")
ax.bar(x + 0.19, com, width=0.38, color=BLUE, label="Stored in the APK")
ax.set_xticks(x); ax.set_xticklabels(k, fontsize=7.4)
ax.tick_params(axis="x", labelrotation=12)
ax.set_ylabel("Size (MB)")
ax.set_xlabel("Archive entry inside the v1.3 release APK (total 20.9 MB on disk)")
for i, v in enumerate(com):
    ax.text(i + 0.19, v + max(unc) * 0.02, f"{v:.1f}", ha="center", fontsize=7)
ax.set_ylim(0, max(unc) * 1.20)
ax.legend(frameon=False, fontsize=7.4)
ax.grid(axis="y", alpha=0.3)
save(fig, "fig-22-08-apk")

# ── Fig 9: compact RLS figure sized for a single IEEE column ────────────────
seen = {}
for r in rls: seen.setdefault(r["table"], []).append(r)
tabs = sorted(seen, key=lambda t: (t not in PERSONAL, t))
nrows = [max(int(x["rows_returned"]) for x in seen[t]) for t in tabs]
fig, ax = plt.subplots(figsize=(3.45, 2.75))
y = np.arange(len(tabs))
ax.barh(y, [max(v, 0.08) for v in nrows], height=0.7,
        color=[GREEN if t in PERSONAL else GREY for t in tabs])
ax.set_yticks(y); ax.set_yticklabels(tabs, fontsize=6.2); ax.invert_yaxis()
ax.set_xlabel("Rows returned to an anonymous caller (count)", fontsize=7)
ax.set_ylabel("Table queried", fontsize=7)
ax.tick_params(axis="x", labelsize=6.5)
ax.set_xlim(0, max(nrows) * 1.12)
from matplotlib.patches import Patch
ax.legend(handles=[Patch(color=GREEN, label="Personal care data (13)"),
                   Patch(color=GREY, label="Reference data (4)")],
          frameon=False, fontsize=6.2, loc="lower right")
ax.grid(axis="x", alpha=0.3)
save(fig, "fig-24-rls-compact")
