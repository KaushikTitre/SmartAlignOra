"""
Fall Detection - Random Forest Training Script
===============================================
Requirements:
    pip install pandas numpy scikit-learn matplotlib seaborn joblib

Usage:
    python train_model.py

Output:
    - fall_detection_model.pkl   (trained model)
    - scaler.pkl                 (feature scaler)
    - confusion_matrix.png       (visual report)
    - classification_report.txt  (accuracy report)
"""

import pandas as pd
import numpy as np
import glob
import os
import joblib
import matplotlib.pyplot as plt
import seaborn as sns

from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import (
    classification_report,
    confusion_matrix,
    accuracy_score
)

# ─── CONFIG ────────────────────────────────────────────────────────────────────
DATA_FOLDER   = r"C:\Users\Admin\Downloads\content\Fall Datasets"   # 🔁 folder with your CSVs
OUTPUT_FOLDER = r"C:\Users\Admin\Downloads\content\Fall Datasets\model"

WINDOW_SIZE   = 50   # 50 samples × 20ms = 1 second window
STEP_SIZE     = 25   # 50% overlap

LABELS = ["normal", "sitting", "pre_fall", "fall"]

# ─── STEP 1: LOAD & MERGE ALL CSVs ─────────────────────────────────────────────
def load_data(folder):
    files = glob.glob(os.path.join(folder, "*.csv"))
    if not files:
        raise FileNotFoundError(f"No CSV files found in: {folder}")

    dfs = []
    for f in files:
        df = pd.read_csv(f)
        print(f"  Loaded: {os.path.basename(f)} → {len(df)} rows")
        dfs.append(df)

    combined = pd.concat(dfs, ignore_index=True)
    print(f"\n  Total rows: {len(combined)}")
    print(f"  Label counts:\n{combined['label'].value_counts()}\n")
    return combined

# ─── STEP 2: SLIDING WINDOW FEATURE EXTRACTION ─────────────────────────────────
def extract_features(window: pd.DataFrame) -> dict:
    """
    For each sensor signal in the window, compute:
    mean, std, min, max, range, RMS, peak-to-peak
    Also compute signal magnitude area (SMA) for acc and gyro.
    """
    features = {}
    signals = ["pitch", "roll", "ax", "ay", "az", "acc_mag", "gx", "gy", "gz"]

    for col in signals:
        vals = window[col].values.astype(float)
        features[f"{col}_mean"]  = np.mean(vals)
        features[f"{col}_std"]   = np.std(vals)
        features[f"{col}_min"]   = np.min(vals)
        features[f"{col}_max"]   = np.max(vals)
        features[f"{col}_range"] = np.max(vals) - np.min(vals)
        features[f"{col}_rms"]   = np.sqrt(np.mean(vals**2))
        features[f"{col}_p2p"]   = np.ptp(vals)

    # Signal Magnitude Area — good discriminator for falls
    features["sma_acc"] = (
        np.sum(np.abs(window["ax"])) +
        np.sum(np.abs(window["ay"])) +
        np.sum(np.abs(window["az"]))
    ) / len(window)

    features["sma_gyro"] = (
        np.sum(np.abs(window["gx"])) +
        np.sum(np.abs(window["gy"])) +
        np.sum(np.abs(window["gz"]))
    ) / len(window)

    return features


def build_windows(df):
    X, y = [], []

    # Process each label separately to avoid mixing across label boundaries
    for label in df["label"].unique():
        subset = df[df["label"] == label].reset_index(drop=True)

        for start in range(0, len(subset) - WINDOW_SIZE, STEP_SIZE):
            window = subset.iloc[start : start + WINDOW_SIZE]
            features = extract_features(window)
            X.append(features)
            y.append(label)

    return pd.DataFrame(X), np.array(y)

# ─── STEP 3: TRAIN ──────────────────────────────────────────────────────────────
def train(X_train, y_train):
    model = RandomForestClassifier(
        n_estimators=200,
        max_depth=None,
        min_samples_split=5,
        min_samples_leaf=2,
        class_weight="balanced",   # handles any remaining imbalance
        random_state=42,
        n_jobs=-1
    )
    model.fit(X_train, y_train)
    return model

# ─── STEP 4: EVALUATE ──────────────────────────────────────────────────────────
def evaluate(model, X_test, y_test, output_folder):
    y_pred = model.predict(X_test)

    acc = accuracy_score(y_test, y_pred)
    print(f"\n  ✅ Test Accuracy: {acc*100:.2f}%\n")

    report = classification_report(y_test, y_pred, target_names=LABELS, zero_division=0)
    print(report)

    # Save report
    report_path = os.path.join(output_folder, "classification_report.txt")
    with open(report_path, "w") as f:
        f.write(f"Test Accuracy: {acc*100:.2f}%\n\n")
        f.write(report)
    print(f"  📄 Report saved: {report_path}")

    # Confusion matrix
    cm = confusion_matrix(y_test, y_pred, labels=LABELS)
    plt.figure(figsize=(8, 6))
    sns.heatmap(
        cm, annot=True, fmt="d", cmap="Blues",
        xticklabels=LABELS, yticklabels=LABELS
    )
    plt.title("Confusion Matrix")
    plt.ylabel("Actual")
    plt.xlabel("Predicted")
    plt.tight_layout()
    cm_path = os.path.join(output_folder, "confusion_matrix.png")
    plt.savefig(cm_path, dpi=150)
    plt.show()
    print(f"  📊 Confusion matrix saved: {cm_path}")

    return acc

# ─── STEP 5: FEATURE IMPORTANCE ────────────────────────────────────────────────
def plot_feature_importance(model, feature_names, output_folder):
    importances = model.feature_importances_
    indices = np.argsort(importances)[::-1][:20]   # top 20

    plt.figure(figsize=(10, 6))
    plt.bar(range(20), importances[indices])
    plt.xticks(range(20), [feature_names[i] for i in indices], rotation=45, ha="right")
    plt.title("Top 20 Feature Importances")
    plt.tight_layout()
    fi_path = os.path.join(output_folder, "feature_importance.png")
    plt.savefig(fi_path, dpi=150)
    plt.show()
    print(f"  📊 Feature importance saved: {fi_path}")

# ─── MAIN ───────────────────────────────────────────────────────────────────────
def main():
    os.makedirs(OUTPUT_FOLDER, exist_ok=True)

    print("=" * 55)
    print("  Fall Detection — Random Forest Training")
    print("=" * 55)

    # 1. Load
    print("\n[1/5] Loading data...")
    df = load_data(DATA_FOLDER)

    # 2. Sliding window features
    print("[2/5] Extracting windowed features...")
    X, y = build_windows(df)
    print(f"  Windows created: {len(X)}")
    print(f"  Features per window: {X.shape[1]}")
    print(f"  Label distribution: { {l: int((y==l).sum()) for l in np.unique(y)} }")

    # 3. Scale + split
    print("\n[3/5] Splitting and scaling...")
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )
    scaler = StandardScaler()
    X_train_sc = scaler.fit_transform(X_train)
    X_test_sc  = scaler.transform(X_test)
    print(f"  Train: {len(X_train)} | Test: {len(X_test)}")

    # 4. Train
    print("\n[4/5] Training Random Forest...")
    model = train(X_train_sc, y_train)

    # Cross-validation score
    cv_scores = cross_val_score(model, X_train_sc, y_train, cv=5, scoring="accuracy")
    print(f"  5-Fold CV Accuracy: {cv_scores.mean()*100:.2f}% ± {cv_scores.std()*100:.2f}%")

    # 5. Evaluate
    print("\n[5/5] Evaluating...")
    evaluate(model, X_test_sc, y_test, OUTPUT_FOLDER)
    plot_feature_importance(model, list(X.columns), OUTPUT_FOLDER)

    # Save model + scaler
    model_path  = os.path.join(OUTPUT_FOLDER, "fall_detection_model.pkl")
    scaler_path = os.path.join(OUTPUT_FOLDER, "scaler.pkl")
    joblib.dump(model,  model_path)
    joblib.dump(scaler, scaler_path)
    print(f"\n  💾 Model saved:  {model_path}")
    print(f"  💾 Scaler saved: {scaler_path}")
    print("\n  ✅ Training complete!")

if __name__ == "__main__":
    main()