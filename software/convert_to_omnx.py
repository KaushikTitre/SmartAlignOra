import joblib
import numpy as np
from skl2onnx import convert_sklearn
from skl2onnx.common.data_types import FloatTensorType

# ─── PATHS ─────────────────────────────
MODEL_PATH = r"C:\Users\Admin\Downloads\content\Fall Datasets\model\fall_model.pkl"
ONNX_PATH  = r"C:\Users\Admin\Downloads\content\Fall Datasets\model\fall_model.onnx"

# ─── LOAD MODEL ────────────────────────
print("Loading model...")
model = joblib.load(MODEL_PATH)

# 🔥 number of input features
n_features = len(model.feature_names_in_)
print("Features:", n_features)

# ─── CONVERT ───────────────────────────
initial_type = [("input", FloatTensorType([None, n_features]))]

onnx_model = convert_sklearn(
    model,
    initial_types=initial_type
)

# ─── SAVE ──────────────────────────────
with open(ONNX_PATH, "wb") as f:
    f.write(onnx_model.SerializeToString())

print("✅ ONNX saved at:", ONNX_PATH)