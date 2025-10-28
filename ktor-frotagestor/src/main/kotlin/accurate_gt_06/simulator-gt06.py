#!/usr/bin/env python3
"""
GT06 Simulator - Espinosa MG → Mato Verde MG
- 100% CORRETO: latitude < 0, longitude < 0
- Workaround para bug do Traccar
"""

import socket
import struct
import time
import math
from datetime import datetime, timezone

# CONFIGURAÇÃO
HOST = "notebook.tail240759.ts.net"
PORT = 8443
IMEI = "352812192008961"
INTERVAL = 2
ITERATIONS = 120
SPEED_KMH = 50

# COORDENADAS (Sul + Oeste)
ESPINOSA_LAT = -14.9257
ESPINOSA_LON = -42.8168
MATO_VERDE_LAT = -15.3951
MATO_VERDE_LON = -42.8609

# LBS
MCC, MNC, LAC, CELL_ID = 724, 5, 40, 0x1F3B

def imei_to_bcd(imei):
    imei = imei.strip()
    if len(imei) == 15: imei = "0" + imei
    return bytes([(int(imei[i]) << 4) | int(imei[i+1]) for i in range(0, 16, 2)])

def crc16(data):
    crc = 0
    for b in data:
        crc ^= (b << 8)
        for _ in range(8):
            crc = (crc << 1) ^ 0x1021 if crc & 0x8000 else crc << 1
        crc &= 0xFFFF
    return crc

def build_packet(proto, info, serial):
    body = bytes([len(info) + 5, proto]) + info + struct.pack(">H", serial)
    crc = crc16(body)
    return b"\x78\x78" + body + struct.pack(">H", crc) + b"\x0D\x0A"

def build_login(serial=1):
    return build_packet(0x01, imei_to_bcd(IMEI), serial)

def build_location(lat: float, lon: float, speed: int, course: int, serial: int) -> bytes:
    now = datetime.now(timezone.utc)
    dt = bytes([now.year % 100, now.month, now.day, now.hour, now.minute, now.second])
    gps_info = 0xC0 | 12

    lat_int = int(abs(lat) * 1_800_000)
    lon_int = int(abs(lon) * 1_800_000)
    lat_b = struct.pack(">I", lat_int)
    lon_b = struct.pack(">I", lon_int)

    # FORÇA BITS 4 E 5 (Sul + Oeste)
    status = 0b00110100  # West + South + Fix
    course_bits = course % 360
    if course_bits == 0: course_bits = 359
    course_status = (course_bits << 6) | status
    course_b = struct.pack(">H", course_status)

    mcc_b = struct.pack(">H", MCC)
    mnc_b = bytes([MNC])
    lac_b = struct.pack(">H", LAC)
    cell_id_b = struct.pack(">I", CELL_ID)[:3]

    info = dt + bytes([gps_info]) + lat_b + lon_b + bytes([speed]) + course_b + mcc_b + mnc_b + lac_b + cell_id_b
    return build_packet(0x12, info, serial)

def generate_trajectory():
    points = []
    for i in range(ITERATIONS + 1):
        fraction = i / ITERATIONS
        lat = ESPINOSA_LAT + (MATO_VERDE_LAT - ESPINOSA_LAT) * fraction
        lon = ESPINOSA_LON + (MATO_VERDE_LON - ESPINOSA_LON) * fraction
        course = (i * 7 + 1) % 360  # evita múltiplos de 5°
        points.append((lat, lon, course))
    return points

def main():
    print("GT06: Espinosa → Mato Verde (Sul + Oeste)")
    try:
        with socket.create_connection((HOST, PORT), timeout=10) as s:
            s.settimeout(5)
            serial = 1
            s.sendall(build_login(serial))
            ack = s.recv(20)
            print(f"Login ACK: {ack.hex()}")
            serial += 1

            for i, (lat, lon, course) in enumerate(generate_trajectory()):
                pkt = build_location(lat, lon, SPEED_KMH, course, serial)
                s.sendall(pkt)
                print(f"[{i+1:03d}] lat={lat:+.6f}, lon={lon:+.6f}, curso={course}°")
                try:
                    s.settimeout(1)
                    ack = s.recv(20)
                    if ack: print(f"  ACK: {ack.hex()}")
                except: pass
                serial = (serial + 1) & 0xFFFF
                time.sleep(INTERVAL)
    except Exception as e:
        print(f"Erro: {e}")

if __name__ == "__main__":
    main()