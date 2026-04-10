from datasets import load_dataset
import json

ds = load_dataset("SWE-bench/SWE-bench_Multimodal")

# Build a JSON mapping from instance_id to the combined bug report text.
bug_reports = {}
for instance in ds['dev']:
    instance_id = instance['instance_id']
    problem_statement = instance.get('problem_statement', '') or ''
    hints_text = instance.get('hints_text', '') or ''
    bug_report = f"{problem_statement} {hints_text}".strip()
    bug_reports[instance_id] = bug_report

output_path = "swe_bench_bug_reports.json"
with open(output_path, "w", encoding="utf-8") as f:
    json.dump(bug_reports, f, ensure_ascii=False, indent=2)

print(f"Saved {len(bug_reports)} bug reports to {output_path}")
