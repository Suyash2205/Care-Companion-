"""Task 21 — static UI measurements taken from the Kotlin sources."""
import re, pathlib, csv, statistics, json
ROOT = pathlib.Path(__file__).resolve().parents[1]
SRC = ROOT / "app/src/main/java/com/carecompanion/app"

def group(p):
    s = str(p)
    if "/ui/elder/" in s: return "elder"
    if "/ui/guardian/" in s: return "guardian"
    return None

sp_rows, dp_rows = [], []
for f in SRC.rglob("*.kt"):
    g = group(f)
    if not g: continue
    t = f.read_text(errors="ignore")
    for line in t.splitlines():
        if "letterSpacing" in line:       # tracking, not a font size
            continue
        for m in re.finditer(r"(?:fontSize|size)\s*=\s*(\d+(?:\.\d+)?)\s*\.\s*sp\b", line):
            sp_rows.append((g, f.name, float(m.group(1))))
    for m in re.finditer(r"(?:\.height|\.heightIn\(\s*min|\.size|\.sizeIn\(\s*min|\.defaultMinSize\(\s*minHeight\s*=)\s*=?\s*\(?\s*(\d+(?:\.\d+)?)\s*\.\s*dp", t):
        dp_rows.append((g, f.name, float(m.group(1))))

with open("raw/ui_text_sizes.csv","w",newline="") as fh:
    w = csv.writer(fh); w.writerow(["surface","file","sp"]); w.writerows(sp_rows)
with open("raw/ui_touch_targets.csv","w",newline="") as fh:
    w = csv.writer(fh); w.writerow(["surface","file","dp"]); w.writerows(dp_rows)

summary = {}
for g in ("elder","guardian"):
    sp = sorted(v for s,_,v in sp_rows if s == g)
    dp = sorted(v for s,_,v in dp_rows if s == g)
    summary[g] = {
        "sp_n": len(sp), "sp_median": statistics.median(sp), "sp_mean": round(statistics.mean(sp),2),
        "sp_min": min(sp), "sp_max": max(sp),
        "sp_pct_below_16": round(100*sum(1 for v in sp if v < 16)/len(sp),1),
        "sp_pct_at_least_20": round(100*sum(1 for v in sp if v >= 20)/len(sp),1),
        "dp_n": len(dp), "dp_median": statistics.median(dp),
        "dp_pct_at_least_48": round(100*sum(1 for v in dp if v >= 48)/len(dp),1),
    }
# code-size counters, used as the resource measure
def count(globpat, base):
    files = list(base.rglob(globpat)); 
    return len(files), sum(len(f.read_text(errors="ignore").splitlines()) for f in files)
summary["code"] = {
    "main_kt_files": count("*.kt", ROOT/"app/src/main")[0],
    "main_kt_lines": count("*.kt", ROOT/"app/src/main")[1],
    "test_kt_files": count("*.kt", ROOT/"app/src/test")[0],
    "test_kt_lines": count("*.kt", ROOT/"app/src/test")[1],
    "sql_files": count("*.sql", ROOT/"supabase")[0],
    "sql_lines": count("*.sql", ROOT/"supabase")[1],
}
pathlib.Path("raw/ui_summary.json").write_text(json.dumps(summary, indent=2))
print(json.dumps(summary, indent=2))
