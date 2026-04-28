# 🚀 SmartAlignOra (ML Module)

### Posture Classification and Fall Detection using Machine Learning

---

## 📌 Overview

This repository contains the **Machine Learning module** of the SmartAlignOra system.

It focuses on:

* Posture classification (GOOD vs BAD)
* Fall detection using IMU sensor data
* Model training, testing, and ONNX conversion

---

## 🎯 Objective

To build a real-time ML system that can:

* Detect incorrect posture
* Identify fall events using sensor patterns
* Run efficiently on a mobile device

---

## 📊 Dataset

The dataset is collected using **MPU6050 sensor** and contains:

### 🔹 Posture Data

* GOOD POSTURE

### 🔹 Fall Data

* FALL DATASETS

### 🔹 Features Recorded

* Pitch, Roll
* Acceleration (ax, ay, az)
* Gyroscope (gx, gy, gz)
* Acceleration Magnitude
* Timestamp

---

## 🧠 ML Pipeline

### 🔹 Data Processing

* Sliding window approach (~20 samples)
* Feature extraction per window

### 🔹 Features Used

* Mean
* Standard Deviation
* Min / Max
* Range
* Slope
* Percent High

---

## 🤖 Model

* Random Forest Classifier
* Accuracy: ~95%
* Exported to ONNX for mobile deployment

---

## 📁 Project Structure

```
SmartAlignOra-ML/
│── FALL DATASETS/        # Fall detection data  
│── GOOD POSTURE/         # Posture dataset  
│── fall_training.py      # Training script  
│── fall_testing.py       # Model evaluation  
│── convert_to_onnx.py    # Convert model to ONNX  
│── training-fall.ipynb   # Experiment notebook  
│── sample.py             # Sample testing script  
│── requirements.txt      # Dependencies  
```

---

## ▶️ How to Run

### 1️⃣ Install Dependencies

```
pip install -r requirements.txt
```

### 2️⃣ Train Model

```
python fall_training.py
```

### 3️⃣ Test Model

```
python fall_testing.py
```

### 4️⃣ Convert to ONNX

```
python convert_to_onnx.py
```

---

## 📈 Key Concepts Used

* Sliding Window Analysis
* Time-Series Feature Extraction
* Random Forest Classification
* Hybrid Fall Detection Logic

---

## 🔗 Integration

The trained ONNX model is integrated into:

* Android application (Kotlin)
* Real-time BLE data pipeline

---

## 📌 Future Improvements

* Deep learning (LSTM / CNN)
* Larger dataset collection
* Real-world fall scenario enhancement



---

## 📎 Note

This repository contains only the **ML component** of the SmartAlignOra system.

For full system (hardware + app), refer to the main project repository.

---
