#include <Adafruit_MPU6050.h>
#include <Adafruit_Sensor.h>
#include <WiFi.h>
#include <WiFiUdp.h>
#include <Wire.h>
#include "esp_timer.h"

/* ================= WiFi CONFIG ================= */
const char* ssid = "Tanay";          // 🔁 CHANGE if needed
const char* password = "tan@y123";   // 🔁 CHANGE if needed

const char* hostIP = "10.134.187.139";  // 🔁 YOUR LAPTOP IP
const int udpPort = 5005;

WiFiUDP udp;

/* ================= MPU6050 ================= */
Adafruit_MPU6050 mpu;

/* ================= Kalman Variables ================= */
double Q_angle = 0.001;
double Q_bias  = 0.003;
double R_measure = 0.03;

// Pitch Kalman
double angleP = 0, biasP = 0;
double PP[2][2] = {{0,0},{0,0}};

// Roll Kalman
double angleR = 0, biasR = 0;
double PR[2][2] = {{0,0},{0,0}};

double dt;
unsigned long lastTime;
unsigned long lastSendTime = 0;
const unsigned long SEND_INTERVAL = 20;   // ~50 Hz

/* ================= Calibration ================= */
double pitchOffset = 0;
double rollOffset  = 0;

/* ================= Kalman Function ================= */
double Kalman_filter(double accelAngle, double gyroRate,
                     double &angle, double &bias, double P[2][2]) {
  double rate = gyroRate - bias;
  angle += dt * rate;

  P[0][0] += dt * (dt*P[1][1] - P[0][1] - P[1][0] + Q_angle);
  P[0][1] -= dt * P[1][1];
  P[1][0] -= dt * P[1][1];
  P[1][1] += Q_bias * dt;

  double S  = P[0][0] + R_measure;
  double K0 = P[0][0] / S;
  double K1 = P[1][0] / S;

  double y = accelAngle - angle;
  angle += K0 * y;
  bias  += K1 * y;

  double P00 = P[0][0], P01 = P[0][1];
  P[0][0] -= K0 * P00;
  P[0][1] -= K0 * P01;
  P[1][0] -= K1 * P00;
  P[1][1] -= K1 * P01;

  return angle;
}

/* ================= Calibration ================= */
void calibrateMPU() {
  Serial.println("Calibrating... Keep device steady");

  double sumPitch = 0, sumRoll = 0;
  const int samples = 500;

  for (int i = 0; i < samples; i++) {
    sensors_event_t a, g, temp;
    mpu.getEvent(&a, &g, &temp);

    sumPitch += atan2(
      -a.acceleration.x,
      sqrt(a.acceleration.y*a.acceleration.y + a.acceleration.z*a.acceleration.z)
    ) * 180.0 / PI;

    sumRoll += atan2(
      a.acceleration.y,
      sqrt(a.acceleration.x*a.acceleration.x + a.acceleration.z*a.acceleration.z)
    ) * 180.0 / PI;

    delay(5);
  }

  pitchOffset = sumPitch / samples;
  rollOffset  = sumRoll  / samples;

  Serial.print("Pitch Offset: "); Serial.println(pitchOffset);
  Serial.print("Roll Offset:  "); Serial.println(rollOffset);
}

/* ================= SETUP ================= */
void setup() {
  Serial.begin(115200);
  Wire.begin();

  /* ---------- WiFi Connect ---------- */
  WiFi.begin(ssid, password);
  Serial.print("Connecting WiFi");
  while (WiFi.status() != WL_CONNECTED) {
    Serial.print(".");
    delay(500);
  }

  Serial.println("\nWiFi Connected!");
  Serial.print("ESP IP: ");
  Serial.println(WiFi.localIP());

  udp.begin(udpPort);

  /* ---------- MPU Setup ---------- */
  if (!mpu.begin()) {
    Serial.println("MPU6050 not found!");
    while (1);
  }

  mpu.setAccelerometerRange(MPU6050_RANGE_4_G);
  mpu.setGyroRange(MPU6050_RANGE_500_DEG);
  mpu.setFilterBandwidth(MPU6050_BAND_44_HZ);

  Serial.println("MPU6050 Ready");

  calibrateMPU();

  lastTime = millis();

  Serial.println("Streaming UDP data...");
}

/* ================= LOOP ================= */
void loop() {

  sensors_event_t a, g, temp;
  mpu.getEvent(&a, &g, &temp);

  /* ---------- Magnitude ---------- */
  float mag = sqrt(
    a.acceleration.x * a.acceleration.x +
    a.acceleration.y * a.acceleration.y +
    a.acceleration.z * a.acceleration.z
  );

  /* ---------- dt ---------- */
  unsigned long now = millis();
  dt = (now - lastTime) / 1000.0;
  if (dt <= 0 || dt > 0.1) dt = 0.02;
  lastTime = now;

  /* ---------- Accel Angles ---------- */
  double accelPitch = atan2(
    -a.acceleration.x,
    sqrt(a.acceleration.y*a.acceleration.y + a.acceleration.z*a.acceleration.z)
  ) * 180.0 / PI - pitchOffset;

  double accelRoll = atan2(
    a.acceleration.y,
    sqrt(a.acceleration.x*a.acceleration.x + a.acceleration.z*a.acceleration.z)
  ) * 180.0 / PI - rollOffset;

  /* ---------- Gyro Rates ---------- */
  double gyroRateY = g.gyro.y * 180.0 / PI;
  double gyroRateX = g.gyro.x * 180.0 / PI;

  /* ---------- Kalman ---------- */
  double pitch = Kalman_filter(accelPitch, gyroRateY, angleP, biasP, PP);
  double roll  = Kalman_filter(accelRoll,  gyroRateX, angleR, biasR, PR);

  /* ---------- Send Every 20ms ---------- */
  if (now - lastSendTime >= SEND_INTERVAL) {

    uint64_t ts = esp_timer_get_time() / 1000;

    char buffer[200];

    sprintf(buffer,
    "%llu,%.2f,%.2f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f",
    ts,pitch,roll,
    a.acceleration.x,
    a.acceleration.y,
    a.acceleration.z,
    mag,
    g.gyro.x*180.0/PI,
    g.gyro.y*180.0/PI,
    g.gyro.z*180.0/PI
    );

    udp.beginPacket(hostIP, udpPort);
    udp.print(buffer);
    udp.endPacket();

    lastSendTime = now;
  }
}