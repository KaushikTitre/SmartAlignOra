import socket
import numpy as np
import pandas as pd
import joblib
from collections import deque

# ─── CONFIG ─────────────────────────────────────────────
UDP_IP   = "0.0.0.0"
UDP_PORT = 5005

# Update this path to your actual model location
MODEL_PATH = r"C:\Users\Admin\Downloads\content\Fall Datasets\model\fall_model.pkl"

WINDOW_SIZE = 75
STEP_SIZE   = 25

# ─── LOAD MODEL ─────────────────────────────────────────
print("Loading model...")
try:
    model = joblib.load(MODEL_PATH)
    fall_index = list(model.classes_).index("fall")
except Exception as e:
    print(f"Error loading model: {e}")
    exit()

# ─── BUFFER & COUNTERS ──────────────────────────────────
buffer = deque(maxlen=WINDOW_SIZE)
counter = 0
still_counter = 0

# ─── FEATURE EXTRACTION ─────────────────────────────────
def extract_features(window_deque):
    # Convert deque of dicts to DataFrame
    window = pd.DataFrame(list(window_deque))
    features = {}
    signals = ["pitch","roll","ax","ay","az","acc_mag","gx","gy","gz"]

    for col in signals:
        vals = window[col].astype(float).values
        features[f"{col}_mean"]  = np.mean(vals)
        features[f"{col}_std"]   = np.std(vals)
        features[f"{col}_min"]   = np.min(vals)
        features[f"{col}_max"]   = np.max(vals)
        features[f"{col}_range"] = np.max(vals) - np.min(vals)
        features[f"{col}_rms"]   = np.sqrt(np.mean(vals**2))
        features[f"{col}_p2p"]   = np.ptp(vals)

    features["acc_peak"] = np.max(window["acc_mag"])
    features["acc_min"]  = np.min(window["acc_mag"])
    features["gyro_peak"] = max(
        np.max(np.abs(window["gx"])),
        np.max(np.abs(window["gy"])),
        np.max(np.abs(window["gz"]))
    )
    features["motion_energy"] = np.sum(np.abs(window["acc_mag"]))
    features["sma_acc"] = (np.abs(window["ax"]).sum() + np.abs(window["ay"]).sum() + np.abs(window["az"]).sum()) / len(window)
    features["sma_gyro"] = (np.abs(window["gx"]).sum() + np.abs(window["gy"]).sum() + np.abs(window["gz"]).sum()) / len(window)

    return features

# ─── START UDP ──────────────────────────────────────────
sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock.bind((UDP_IP, UDP_PORT))
sock.settimeout(1.0)

print("=" * 55)
print(f"🚀 LIVE FALL DETECTION LISTENING ON PORT {UDP_PORT}")
print("=" * 55)

while True:
    try:
        data, addr = sock.recvfrom(1024)
        row = data.decode().strip().split(",")

        # Basic validation: ensure we have the right number of columns
        if len(row) < 10:
            continue

        sample = {
            "pitch": float(row[1]),
            "roll": float(row[2]),
            "ax": float(row[3]),
            "ay": float(row[4]),
            "az": float(row[5]),
            "acc_mag": float(row[6]),
            "gx": float(row[7]),
            "gy": float(row[8]),
            "gz": float(row[9]),
        }

        buffer.append(sample)
        counter += 1

        # Only proceed if we have enough data for a full window
        if len(buffer) < WINDOW_SIZE:
            print(f"\rFilling buffer... ({len(buffer)}/{WINDOW_SIZE})", end="")
            continue

        # Process data every STEP_SIZE samples
        if counter % STEP_SIZE == 0:
            feat = extract_features(buffer)
            X = pd.DataFrame([feat])
            
            # Ensure columns match the model's expected input order
            X = X[model.feature_names_in_]

            pred = model.predict(X)[0]
            prob = model.predict_proba(X)[0]
            fall_prob = prob[fall_index]

            print(f"\rPred: {pred:6} | Fall Prob: {fall_prob:.2f} | Mag: {sample['acc_mag']:.2f}", end="")

            # Logic Checkpoints
            impact = sample["acc_mag"] > 14
            ml_detected = (pred == "fall" and fall_prob > 0.85)
            
            # Stillness Check: Low gyro activity
            if abs(sample["gx"]) < 15 and abs(sample["gy"]) < 15 and abs(sample["gz"]) < 15:
                still_counter += 1
            else:
                still_counter = 0

            # Combined Alert Logic
            if ml_detected and (impact or still_counter >= 5):
                print("\n" + "!"*20)
                print("🚨 CONFIRMED FALL DETECTED! 🚨")
                print("!"*20 + "\n")
                still_counter = 0 # Reset after alert

    except socket.timeout:
        # This keeps the loop alive even if no data is coming in
        continue
    except KeyboardInterrupt:
        break
    except Exception as e:
        print(f"\nError processing data: {e}")
        continue

sock.close()
print("\n🛑 Stopped")