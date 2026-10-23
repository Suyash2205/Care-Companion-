"""Task 21 — parse the JUnit XML from today's run into raw per-test records."""
import glob, csv, json, statistics, xml.etree.ElementTree as ET, pathlib, datetime
files = sorted(glob.glob("/Users/suyash/CareCampanion/app/build/test-results/testDebugUnitTest/*.xml"))
rows, suites = [], []
for fp in files:
    r = ET.parse(fp).getroot()
    suites.append({
        "suite": r.get("name"), "tests": int(r.get("tests")), "failures": int(r.get("failures")),
        "errors": int(r.get("errors")), "skipped": int(r.get("skipped")),
        "time_s": float(r.get("time")), "timestamp": r.get("timestamp"),
    })
    for tc in r.findall("testcase"):
        status = "pass"
        if tc.find("failure") is not None: status = "fail"
        elif tc.find("error") is not None: status = "error"
        elif tc.find("skipped") is not None: status = "skipped"
        rows.append({"suite": tc.get("classname"), "test": tc.get("name"),
                     "time_s": float(tc.get("time") or 0), "status": status})
with open("raw/unit_tests.csv","w",newline="") as fh:
    w = csv.DictWriter(fh, ["suite","test","time_s","status"]); w.writeheader(); w.writerows(rows)
with open("raw/unit_test_suites.csv","w",newline="") as fh:
    w = csv.DictWriter(fh, list(suites[0])); w.writeheader(); w.writerows(suites)
t = [r["time_s"] for r in rows]
summary = {
  "run_date": min(s["timestamp"] for s in suites),
  "suites": len(suites), "tests": len(rows),
  "passed": sum(1 for r in rows if r["status"]=="pass"),
  "failed": sum(1 for r in rows if r["status"]=="fail"),
  "errors": sum(1 for r in rows if r["status"]=="error"),
  "skipped": sum(1 for r in rows if r["status"]=="skipped"),
  "pass_rate_pct": round(100*sum(1 for r in rows if r["status"]=="pass")/len(rows),2),
  "exec_sum_s": round(sum(t),2), "exec_mean_ms": round(1000*statistics.mean(t),1),
  "exec_median_ms": round(1000*statistics.median(t),1), "exec_max_ms": round(1000*max(t),1),
  "exec_min_ms": round(1000*min(t),1),
  "slowest": sorted(({"suite":r["suite"].split(".")[-1],"test":r["test"],"ms":round(1000*r["time_s"],1)} for r in rows), key=lambda d:-d["ms"])[:8],
  "failing": [{"suite":r["suite"],"test":r["test"]} for r in rows if r["status"] in ("fail","error")],
}
pathlib.Path("raw/unit_test_summary.json").write_text(json.dumps(summary, indent=2))
print(json.dumps(summary, indent=2))
