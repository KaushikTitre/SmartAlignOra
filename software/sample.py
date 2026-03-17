"""
Fall Detection - UDP Data Collector
====================================
Labels:
  g  →  normal   (standing / walking normally)
  f  →  fall     (actual fall event)
  p  →  pre_fall (stumble / loss of balance before fall)
  s  →  sitting  (sitting / sedentary)
  q  →  quit and save

Controls:
  Press a label key at any time — all subsequent packets get that label.
  No blocking. All 50 Hz data is captured.
"""

import socket
import csv
import threading
import sys
import os
from datetime import datetime

# ─── Config ────────────────────────────────────────────────────────────────
UDP_IP   = "0.0.0.0"
UDP_PORT = 5005

LABEL_MAP = {
    "g": "normal",
    "f": "fall",
    "p": "pre_fall",
    "s": "sitting",
}

# ─── Shared State ───────────────────────────────────────────────────────────
current_label  = "normal"
running        = True
packet_count   = 0
dropped_count  = 0  # packets with wrong field count


folder = r"C:\Users\Admin\Downloads\content\FALL DATASETS"
os.makedirs(folder, exist_ok=True)  # creates folder if it doesn't exist

filename = os.path.join(folder, f"fall_dataset_{datetime.now().strftime('%Y%m%d_%H%M%S')}.csv")

# ─── Non-blocking Keyboard Thread ───────────────────────────────────────────
def keyboard_listener():
    global current_label, running
    # Use raw terminal input on Linux/Mac; works on Windows too via input()
    if os.name == "nt":
        import msvcrt
        while running:
            if msvcrt.kbhit():
                key = msvcrt.getwch().lower()
                _handle_key(key)
    else:
        import tty, termios
        fd = sys.stdin.fileno()
        old = termios.tcgetattr(fd)
        try:
            tty.setraw(fd)
            while running:
                key = sys.stdin.read(1).lower()
                _handle_key(key)
        finally:
            termios.tcsetattr(fd, termios.TCSADRAIN, old)

def _handle_key(key):
    global current_label, running
    if key in LABEL_MAP:
        current_label = LABEL_MAP[key]
        print(f"\r  ✏️  Label → [{current_label}]          ", flush=True)
    elif key == "q":
        running = False
        print("\n  🛑 Quit signal received...", flush=True)

# ─── Main ────────────────────────────────────────────────────────────────────
def main():
    global running, packet_count, dropped_count

    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.bind((UDP_IP, UDP_PORT))
    sock.settimeout(1.0)   # allows the loop to check `running` flag

    kb_thread = threading.Thread(target=keyboard_listener, daemon=True)
    kb_thread.start()

    with open(filename, "w", newline="") as f:
        writer = csv.writer(f)
        writer.writerow([
            "timestamp_ms", "pitch", "roll",
            "ax", "ay", "az", "acc_mag",
            "gx", "gy", "gz",
            "label"
        ])

        print("=" * 50)
        print("  📡 Fall Detection Data Collector")
        print("=" * 50)
        print("  g → normal   f → fall")
        print("  p → pre_fall s → sitting")
        print("  q → quit & save")
        print("=" * 50)
        print(f"  Saving to: {filename}")
        print(f"  Current label: [{current_label}]")
        print("-" * 50)

        while running:
            try:
                data, _ = sock.recvfrom(1024)
                row = data.decode().strip().split(",")

                if len(row) != 10:
                    dropped_count += 1
                    continue

                writer.writerow(row + [current_label])
                packet_count += 1

                # Live status line (overwrites in place)
                print(
                    f"\r  📦 Packets: {packet_count:6d} | "
                    f"Label: [{current_label:<8s}] | "
                    f"Dropped: {dropped_count}   ",
                    end="", flush=True
                )

            except socket.timeout:
                continue   # just re-check `running`
            except KeyboardInterrupt:
                break

    sock.close()
    print(f"\n\n  ✅ Done! {packet_count} packets saved to: {filename}")
    print(f"  ⚠️  Dropped (malformed): {dropped_count}")

if __name__ == "__main__":
    main()