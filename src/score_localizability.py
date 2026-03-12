"""
score_localizability.py
-----------------------
Given a Defects4J bug report XML and the pre-extracted feature dataset,
computes a delta-weighted localizability score for the bug.

Usage:
    python score_localizability.py \
        --xml         Chart_1.xml \
        --features    final_feature_set.csv \
        --deltas      all_vs_none_top5.csv \
        --scaling     full_dataset_scaling.csv \
        --threshold   top5

The script:
  1. Parses the XML, reading ONLY <summary> and <description> — never <fixedFiles>.
  2. Looks up the bug's pre-extracted features in final_feature_set.csv by matching
     project + bug_id derived from the XML filename (e.g. Chart_1.xml → project=Chart, bug_id=1).
  3. Computes the delta-weighted localizability score:
         s(b) = Σ  δ_f * z_f(b)
     using only features that are both in the delta file (practically_significant=True)
     and present in the feature row.
  4. Prints the score, its interpretation, and which features are driving it.
  5. If the score falls below --gate-percentile (default: 33rd percentile of score
     distribution across all bugs), flags the report as needing augmentation and
     lists the weakest feature dimensions to target.

NOTE: <fixedFiles> is never read. The script will warn if it detects an attempt
      to access that tag and will skip it.
"""

import argparse
import sys
import re
import xml.etree.ElementTree as ET
from pathlib import Path
import os, glob, json

import pandas as pd
import numpy as np


# ---------------------------------------------------------------------------
# Constants
# ---------------------------------------------------------------------------
BLOCKED_TAGS = {"fixedFiles", "file"}  # tags we must never read content from

# Feature dimensions mapped to human-readable augmentation instructions
AUGMENTATION_HINTS = {
    "repair_difficulty":            "HIGH repair difficulty — generate an inferred fault hypothesis "
                                    "and likely impacted code concepts to guide retrieval.",
    "reasoning_composite":          "LOW causal reasoning quality — supplement with an explicit "
                                    "cause-effect chain derived from the observed behavior.",
    "actionability":                "LOW actionability — add concrete reproduction context or "
                                    "trigger conditions implied by the description.",
    "clarity":                      "LOW clarity — restate the core failure in unambiguous terms, "
                                    "distinguishing symptom from cause.",
    "expected_observed_alignment":  "MISSING expected-vs-observed contrast — explicitly state what "
                                    "the system should have done vs. what it did.",
    "technical_completeness":       "LOW technical completeness — infer missing structural elements "
                                    "(e.g. reproduction steps, environment context).",
    "ambiguity_type_count":         "HIGH ambiguity — identify and resolve the dominant ambiguity "
                                    "type (missing steps, vague inputs, unclear error).",
    "embedding_cluster_distance":   "HIGH embedding cluster distance — report may be an outlier; "
                                    "consider whether terminology aligns with codebase vocabulary.",
}

# Features that FAVOR localization (positive delta = good)
POSITIVE_DELTA_FEATURES = {
    "reasoning_composite", "actionability", "clarity",
    "expected_observed_alignment", "technical_completeness",
    "description_length", "txt_description_line_count",
    "embedding_cluster_distance",
}

# Features that HINDER localization (negative delta = bad when high)
NEGATIVE_DELTA_FEATURES = {
    "repair_difficulty", "ambiguity_type_count",
    "flesch", "coleman_liau", "ari", "kincaid", "embedding_cluster_size",
}


# ---------------------------------------------------------------------------
# XML parsing — never reads <fixedFiles>
# ---------------------------------------------------------------------------
def parse_bug_report(xml_path: Path) -> dict:
    """
    Parse a Defects4J bug report XML.
    Returns dict with keys: bug_id, summary, description.
    Raises ValueError if <fixedFiles> content is accessed.
    """
    tree = ET.parse(xml_path)
    root = tree.getroot()

    result = {}

    for bug_elem in root.iter("bug"):
        result["bug_id"] = bug_elem.get("id")

        for child in bug_elem:
            tag = child.tag

            if tag in BLOCKED_TAGS:
                # Explicitly skip — do not read text or children
                print(f"  [INFO] Skipping <{tag}> tag — ground truth not read.")
                continue

            if tag == "buginformation":
                for info_child in child:
                    if info_child.tag == "summary":
                        result["summary"] = (info_child.text or "").strip()
                    elif info_child.tag == "description":
                        result["description"] = (info_child.text or "").strip()
                    elif info_child.tag in BLOCKED_TAGS:
                        print(f"  [INFO] Skipping <{info_child.tag}> inside buginformation.")
                        continue

        # Only process first bug entry
        break

    if "bug_id" not in result:
        raise ValueError(f"No <bug> element found in {xml_path}")

    return result


# ---------------------------------------------------------------------------
# Feature lookup
# ---------------------------------------------------------------------------
def lookup_features(features_df: pd.DataFrame, project: str, bug_id: str) -> pd.Series:
    """
    Find the feature row for project + bug_id.
    Tries matching on the 'id' column (e.g. 'Chart-1') first,
    then on project + bug_id columns separately.
    """
    target_id = f"{project}-{bug_id}"

    if "id" in features_df.columns:
        match = features_df[features_df["id"] == target_id]
        if not match.empty:
            return match.iloc[0]

    # Fallback: match on project and bug_id columns
    if "project" in features_df.columns and "bug_id" in features_df.columns:
        match = features_df[
            (features_df["project"].str.lower() == project.lower()) &
            (features_df["bug_id"].astype(str) == str(bug_id))
        ]
        if not match.empty:
            return match.iloc[0]

    raise ValueError(
        f"Bug '{target_id}' not found in feature dataset. "
        f"Available IDs (first 10): {features_df['id'].head(10).tolist() if 'id' in features_df.columns else 'N/A'}"
    )


# ---------------------------------------------------------------------------
# Scoring
# ---------------------------------------------------------------------------
def compute_score(
    feature_row: pd.Series,
    deltas_df: pd.DataFrame,
    scaling_df: pd.DataFrame,
) -> tuple[float, pd.DataFrame]:
    """
    Compute delta-weighted localizability score.
    
    s(b) = Σ δ_f * z_f(b)
    
    where z_f(b) = (x_f(b) - mean_f) / std_f  using population scaling params.
    Features already z-scored in final_feature_set.csv are used as-is
    (scaling mean=0, std=1 effectively).

    Returns (score, details_df) where details_df has per-feature contributions.
    """
    # Filter to practically significant features only
    sig_deltas = deltas_df[deltas_df["practically_significant"] == True].copy()

    scaling_lookup = scaling_df.set_index("feature")[["mean", "std"]].to_dict("index")

    records = []
    for _, row in sig_deltas.iterrows():
        feature = row["feature"]
        delta = row["cliffs_delta"]

        if feature not in feature_row.index:
            continue

        raw_val = feature_row[feature]

        # Skip if missing
        if pd.isna(raw_val):
            continue

        # Convert boolean to int if needed
        if isinstance(raw_val, bool) or str(raw_val).lower() in ("true", "false"):
            raw_val = 1.0 if str(raw_val).lower() == "true" else 0.0

        try:
            raw_val = float(raw_val)
        except (ValueError, TypeError):
            continue

        # Z-score using population scaling params if available
        if feature in scaling_lookup:
            mean_f = scaling_lookup[feature]["mean"]
            std_f  = scaling_lookup[feature]["std"]
            z = (raw_val - mean_f) / std_f if std_f > 0 else 0.0
        else:
            # Feature already z-scored in dataset
            z = raw_val

        contribution = delta * z
        records.append({
            "feature":      feature,
            "delta":        delta,
            "z_score":      z,
            "contribution": contribution,
            "effect_size":  row.get("effect_size", ""),
        })

    if not records:
        raise ValueError("No matching features found between delta file and feature row.")

    details_df = pd.DataFrame(records).sort_values("contribution", ascending=False)
    score = details_df["contribution"].sum()
    return score, details_df


# ---------------------------------------------------------------------------
# Gate threshold
# ---------------------------------------------------------------------------
def compute_gate_threshold(
    features_df: pd.DataFrame,
    deltas_df: pd.DataFrame,
    scaling_df: pd.DataFrame,
    percentile: float = 47.4,
) -> float:
    """
    Compute the score distribution across all bugs and return the
    gate threshold at the given percentile.
    """
    scores = []
    sig_deltas = deltas_df[deltas_df["practically_significant"] == True]
    scaling_lookup = scaling_df.set_index("feature")[["mean", "std"]].to_dict("index")
    valid_features = [f for f in sig_deltas["feature"].tolist() if f in features_df.columns]
    delta_map = sig_deltas.set_index("feature")["cliffs_delta"].to_dict()

    for _, feature_row in features_df.iterrows():
        s = 0.0
        for feature in valid_features:
            raw_val = feature_row.get(feature)
            if pd.isna(raw_val):
                continue
            if isinstance(raw_val, bool) or str(raw_val).lower() in ("true", "false"):
                raw_val = 1.0 if str(raw_val).lower() == "true" else 0.0
            try:
                raw_val = float(raw_val)
            except (ValueError, TypeError):
                continue
            if feature in scaling_lookup:
                mean_f = scaling_lookup[feature]["mean"]
                std_f  = scaling_lookup[feature]["std"]
                z = (raw_val - mean_f) / std_f if std_f > 0 else 0.0
            else:
                z = raw_val
            s += delta_map[feature] * z
        scores.append(s)

    return float(np.percentile(scores, percentile))


# ---------------------------------------------------------------------------
# Report
# ---------------------------------------------------------------------------
def get_augmentation_hints(details_df: pd.DataFrame) -> list:
    """
    Extract augmentation hints for weak features (negative contribution).
    Returns a list of hint strings.
    """
    hints = []
    # Find features with negative contribution that have hints
    weak = details_df[details_df["contribution"] < 0].copy()
    weak["abs_contrib"] = weak["contribution"].abs()
    weak = weak.sort_values("abs_contrib", ascending=False)

    for _, r in weak.iterrows():
        feat = r["feature"]
        if feat in AUGMENTATION_HINTS:
            hints.append(f"{feat}: {AUGMENTATION_HINTS[feat]}")

    return hints


def print_report(
    bug_info: dict,
    project: str,
    score: float,
    details_df: pd.DataFrame,
    gate_threshold: float,
    needs_augmentation: bool,
) -> None:
    SEP = "=" * 65

    print(f"\n{SEP}")
    print(f"  LOCALIZABILITY REPORT  —  {project}-{bug_info['bug_id']}")
    print(SEP)
    print(f"  Summary   : {bug_info.get('summary', 'N/A')[:80]}")
    print(f"  Score     : {score:+.4f}")
    print(f"  Threshold : {gate_threshold:+.4f}  (gate percentile)")
    print(f"  Status    : {'⚠  NEEDS AUGMENTATION' if needs_augmentation else '✓  OK — likely localizable'}")
    print()

    print("  Top contributing features (sorted by contribution):")
    print(f"  {'Feature':<38} {'Delta':>7}  {'Z':>7}  {'Contrib':>8}")
    print(f"  {'-'*38} {'-'*7}  {'-'*7}  {'-'*8}")
    for _, r in details_df.head(15).iterrows():
        print(f"  {r['feature']:<38} {r['delta']:>+7.3f}  {r['z_score']:>+7.3f}  {r['contribution']:>+8.4f}")

    if needs_augmentation:
        print()
        print("  Augmentation targets (weakest dimensions to address):")
        hints = get_augmentation_hints(details_df)
        for hint in hints[:4]:  # Show up to 4 hints
            parts = hint.split(": ", 1)
            if len(parts) == 2:
                print(f"\n  • {parts[0]}")
                print(f"    → {parts[1]}")

        if not hints:
            print("  (No specific augmentation hints available for weak features.)")

    print(f"\n{SEP}\n")


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------
def parse_args():
    p = argparse.ArgumentParser(description="Score bug report localizability.")
    p.add_argument("--xml-dir",   default="defects4j_xml", help="Path to directory containing Defects4J bug report XML files")
    p.add_argument("--features",  default="bug_features/final_feature_set.csv", help="Path to final_feature_set.csv")
    p.add_argument("--deltas",    default="bug_features/all_vs_none_top5.csv", help="Path to all_vs_none_topN.csv (e.g. top5)")
    p.add_argument("--scaling",   default="bug_features/full_dataset_scaling.csv", help="Path to full_dataset_scaling.csv")
    p.add_argument("--threshold", default="top5", help="Threshold label (top1/top5/top10) — informational only")
    p.add_argument("--gate-percentile", type=float, default=47.4,
                   help="Percentile of score distribution used as augmentation gate (default: 33)")
    p.add_argument("--output",    default="localizability_scores.csv", help="Output CSV file path")
    return p.parse_args()


def main():
    args = parse_args()

    # Get all XML files in the directory
    xml_dir = Path(args.xml_dir)
    if not xml_dir.exists():
        sys.exit(f"ERROR: XML directory not found: {xml_dir}")
    
    xml_files = sorted(xml_dir.glob("*.xml"))
    if not xml_files:
        sys.exit(f"ERROR: No XML files found in {xml_dir}")

    print(f"Found {len(xml_files)} XML files to process...")
    print(f"\nLoading data files...")
    features_df = pd.read_csv(args.features)
    deltas_df   = pd.read_csv(args.deltas)
    scaling_df  = pd.read_csv(args.scaling)

    # Compute gate threshold once (same for all bugs)
    print(f"Computing gate threshold (p{args.gate_percentile:.0f} across all bugs)...")
    gate_threshold = compute_gate_threshold(
        features_df, deltas_df, scaling_df, args.gate_percentile
    )
    print(f"Gate threshold: {gate_threshold:+.4f}\n")

    rows = []
    processed = 0
    errors = []

    for xml_file in xml_files:
        try:
            # Parse project and bug_id from filename (e.g. "Chart_1.xml" -> project="Chart", bug_id="1")
            stem = xml_file.stem  # e.g. "Chart_1"
            parts = stem.rsplit("_", 1)
            if len(parts) != 2:
                errors.append(f"{stem}: Cannot parse project/bug_id from filename. Expected format: Project_N.xml")
                continue
            
            project, bug_id = parts[0], parts[1]
            bug_id_str = f"{project}-{bug_id}"

            # Parse bug report
            bug_info = parse_bug_report(xml_file)

            # Look up features
            try:
                feature_row = lookup_features(features_df, project, bug_id)
            except ValueError as e:
                errors.append(f"{bug_id_str}: {str(e)}")
                continue

            # Compute score
            score, details_df = compute_score(feature_row, deltas_df, scaling_df)

            # Determine if augmentation is needed
            needs_augmentation = score < gate_threshold

            # Get augmentation hints if needed
            augmentation_hints = []
            if needs_augmentation:
                augmentation_hints = get_augmentation_hints(details_df)

            # Collect results
            rows.append({
                "id": bug_id_str,
                "project": project,
                "bug_id": bug_id,
                "score": score,
                "gate_threshold": gate_threshold,
                "needs_augmentation": needs_augmentation,
                "augmentation_hints": "; ".join(augmentation_hints) if augmentation_hints else "",
            })

            processed += 1
            if processed % 50 == 0:
                print(f"Processed {processed}/{len(xml_files)} files...")

        except Exception as e:
            errors.append(f"{xml_file.name}: {str(e)}")
            continue

    # Create output DataFrame and save to CSV
    if not rows:
        print(f"\n{'='*65}")
        print(f"  ERROR: No bugs were successfully processed.")
        print(f"{'='*65}\n")
        sys.exit(1)
    
    results_df = pd.DataFrame(rows)
    results_df.to_csv(args.output, index=False)
    
    print(f"\n{'='*65}")
    print(f"  PROCESSING COMPLETE")
    print(f"{'='*65}")
    print(f"  Processed: {processed}/{len(xml_files)} files")
    print(f"  Output CSV: {args.output}")
    print(f"  Bugs needing augmentation: {results_df['needs_augmentation'].sum()}")
    
    if errors:
        print(f"\n  Errors encountered ({len(errors)}):")
        for error in errors[:10]:  # Show first 10 errors
            print(f"    - {error}")
        if len(errors) > 10:
            print(f"    ... and {len(errors) - 10} more errors")
    
    print(f"{'='*65}\n")


if __name__ == "__main__":
    main()