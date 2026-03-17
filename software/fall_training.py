"""
Fall Detection - Optimized Random Forest Training
=================================================
Improvements:
✔ label cleaning (pref_fall fix)
✔ no scaler (better for RF)
✔ added impact + motion features
✔ better window coverage
✔ optional class merging

Run:
    python fall_training.py
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
from sklearn.metrics import classification_report, confusion_matrix, accuracy_score

# ─── CONFIG ─────────────────────────────────────────────
DATA_FOLDER   = r"C:\Users\Admin\Downloads\content\Fall Datasets"
OUTPUT_FOLDER = r"C:\Users\Admin\Downloads\content\Fall Datasets\model"

WINDOW_SIZE = 75   # ⬅️ improved (was 50)
STEP_SIZE   = 25

# 👉 Choose mode
MODE = "binary"   # "4class" / "3class" / "binary"

# ─── LOAD DATA ──────────────────────────────────────────
def load_data(folder):
    files = glob.glob(os.path.join(folder, "*.csv"))
    dfs = []

    for f in files:
        df = pd.read_csv(f)

        # ✅ CLEAN LABELS
        df["label"] = df["label"].astype(str).str.strip().str.lower()
        df["label"] = df["label"].replace({
            "pref_fall": "pre_fall",
            "prefall": "pre_fall",
            "pre fall": "pre_fall"
        })

        dfs.append(df)
        print(f"  Loaded: {os.path.basename(f)} → {len(df)} rows")

    df = pd.concat(dfs, ignore_index=True)

    # ─── CLASS MODE HANDLING ─────────────────────────────
    if MODE == "3class":
        df["label"] = df["label"].replace({
            "pre_fall": "fall"
        })

    elif MODE == "binary":
        df["label"] = df["label"].replace({
            "normal": "safe",
            "sitting": "safe",
            "pre_fall": "fall"
        })

    print(f"\n  Total rows: {len(df)}")
    print(f"  Label counts:\n{df['label'].value_counts()}\n")

    return df

# ─── FEATURE EXTRACTION ─────────────────────────────────
def extract_features(window):
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

    # 🔥 IMPACT FEATURES (VERY IMPORTANT)
    features["acc_peak"] = np.max(window["acc_mag"])
    features["acc_min"]  = np.min(window["acc_mag"])

    features["gyro_peak"] = max(
        np.max(np.abs(window["gx"])),
        np.max(np.abs(window["gy"])),
        np.max(np.abs(window["gz"]))
    )

    features["motion_energy"] = np.sum(np.abs(window["acc_mag"]))

    # SMA
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

# ─── WINDOWING ──────────────────────────────────────────
def build_windows(df):
    X, y = [], []

    for label in df["label"].unique():
        subset = df[df["label"] == label].reset_index(drop=True)

        for start in range(0, len(subset) - WINDOW_SIZE + 1, STEP_SIZE):
            window = subset.iloc[start:start + WINDOW_SIZE]
            X.append(extract_features(window))
            y.append(label)

    return pd.DataFrame(X), np.array(y)

# ─── TRAIN ──────────────────────────────────────────────
def train(X_train, y_train):
    model = RandomForestClassifier(
        n_estimators=250,
        min_samples_split=5,
        min_samples_leaf=2,
        class_weight="balanced",
        random_state=42,
        n_jobs=-1
    )
    model.fit(X_train, y_train)
    return model

# ─── EVALUATE ───────────────────────────────────────────
def evaluate(model, X_test, y_test, output_folder):
    y_pred = model.predict(X_test)

    acc = accuracy_score(y_test, y_pred)
    print(f"\n  ✅ Test Accuracy: {acc*100:.2f}%\n")

    report = classification_report(y_test, y_pred, zero_division=0)
    print(report)

    with open(os.path.join(output_folder, "classification_report.txt"), "w") as f:
        f.write(report)

    cm = confusion_matrix(y_test, y_pred)
    plt.figure(figsize=(8,6))
    sns.heatmap(cm, annot=True, fmt="d", cmap="Blues")
    plt.title("Confusion Matrix")
    plt.tight_layout()
    plt.savefig(os.path.join(output_folder, "confusion_matrix.png"))
    plt.show()

    return acc

# ─── FEATURE IMPORTANCE ─────────────────────────────────
def plot_feature_importance(model, feature_names, output_folder):
    importances = model.feature_importances_
    indices = np.argsort(importances)[::-1][:20]

    plt.figure(figsize=(10,6))
    plt.bar(range(20), importances[indices])
    plt.xticks(range(20), [feature_names[i] for i in indices], rotation=45, ha="right")
    plt.title("Top Features")
    plt.tight_layout()
    plt.savefig(os.path.join(output_folder, "feature_importance.png"))
    plt.show()

# ─── MAIN ───────────────────────────────────────────────
def main():
    os.makedirs(OUTPUT_FOLDER, exist_ok=True)

    print("="*55)
    print("  Fall Detection — Optimized Training")
    print("="*55)

    print("\n[1/5] Loading data...")
    df = load_data(DATA_FOLDER)

    print("[2/5] Extracting features...")
    X, y = build_windows(df)

    print(f"  Windows: {len(X)}")
    print(f"  Features: {X.shape[1]}")

    print("\n[3/5] Splitting...")
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )

    print(f"  Train: {len(X_train)} | Test: {len(X_test)}")

    print("\n[4/5] Training...")
    model = train(X_train, y_train)

    cv = cross_val_score(model, X_train, y_train, cv=5)
    print(f"  CV Accuracy: {cv.mean()*100:.2f}% ± {cv.std()*100:.2f}%")

    print("\n[5/5] Evaluating...")
    evaluate(model, X_test, y_test, OUTPUT_FOLDER)
    plot_feature_importance(model, list(X.columns), OUTPUT_FOLDER)

    joblib.dump(model, os.path.join(OUTPUT_FOLDER, "fall_model.pkl"))

    print("\n  ✅ Training Complete!")

if __name__ == "__main__":
    main()