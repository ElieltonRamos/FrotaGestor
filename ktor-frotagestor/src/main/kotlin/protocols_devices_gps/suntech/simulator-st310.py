#!/usr/bin/env python3
"""
Suntech ST310 Simulator - Espinosa MG → Mato Verde MG
- Protocolo: SA200 (texto, separado por ';')
- Suporte: UDP e TCP
- Mensagens: SA200STT (posição), SA200ALV (alive)
- ACK opcional (para UDP_ACK=1)
- Trajetória realista (Sul + Oeste)
"""

import socket
import time
import math
from datetime import datetime
import sys

# ================== CONFIGURAÇÃO ==================
HOST = "179.124.217.117"
PORT = 5011
PROTOCOL = "TCP"                      # "UDP" ou "TCP"
DEV_ID = "123456789"                     # ID do dispositivo (6 dígitos)
FIRMWARE_VER = "001"                  # Versão do firmware
INTERVAL = 2                          # Intervalo entre envios (segundos)
ITERATIONS = 120                      # Quantidade de posições
SPEED_KMH = 50                        # Velocidade fixa

# COORDENADAS (Sul + Oeste → negativos)
ESPINOSA_LAT = -14.9257
ESPINOSA_LON = -42.8168
MATO_VERDE_LAT = -15.3951
MATO_VERDE_LON = -42.8609

# STATUS (exemplo fixo)
ODOMETER = 129        # km
SATELLITES = 7
GPS_FIX = 1           # 1 = fix válido
BATTERY_V = "13.80"   # Volts
RSSI = "100000"       # Sinal
IO_STATUS = "2"       # Entradas/saídas
EVENT_CODE = "0002"   # Código de evento

# =================================================

def build_sa200stt(dev_id, fw_ver, lat, lon, speed, course, odometer):
    now = datetime.now()
    date_str = now.strftime("%Y%m%d")
    time_str = now.strftime("%H%M%S")

    # Latitude e longitude com 6 casas decimais
    lat_str = f"{lat:+.6f}"
    lon_str = f"{lon:+.6f}"

    # Velocidade e curso
    speed_str = f"{speed:06.3f}"
    course_str = f"{course:05.2f}"

    # Monta STT
    fields = [
        "SA200STT",
        dev_id,
        fw_ver,
        date_str,
        time_str,
        f"{odometer:05d}",
        lat_str,
        lon_str,
        speed_str,
        course_str,
        str(SATELLITES),
        str(GPS_FIX),
        "0",                    # Status de ignição (0=off)
        BATTERY_V,
        RSSI,
        IO_STATUS,
        EVENT_CODE
    ]
    return ";".join(fields) + "\r"

def build_sa200alv(dev_id, fw_ver):
    now = datetime.now()
    date_str = now.strftime("%Y%m%d")
    time_str = now.strftime("%H%M%S")
    fields = [
        "SA200ALV",
        dev_id,
        fw_ver,
        date_str,
        time_str,
        "00129",  # odômetro
        "0",      # status
        BATTERY_V,
        RSSI
    ]
    return ";".join(fields) + "\r"

def generate_trajectory():
    points = []
    for i in range(ITERATIONS + 1):
        fraction = i / ITERATIONS
        lat = ESPINOSA_LAT + (MATO_VERDE_LAT - ESPINOSA_LAT) * fraction
        lon = ESPINOSA_LON + (MATO_VERDE_LON - ESPINOSA_LON) * fraction
        # Curso: evita múltiplos de 5° (bug Traccar)
        course = (i * 7 + 1) % 360
        if course % 5 == 0:
            course = (course + 1) % 360
        points.append((lat, lon, course))
    return points

def send_udp(msg):
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
        s.sendto(msg.encode('utf-8'), (HOST, PORT))
        print(f"UDP → {msg.strip()}")
        try:
            s.settimeout(2)
            ack, _ = s.recvfrom(1024)
            print(f"    ACK ← {ack.decode('utf-8').strip()}")
        except:
            print("    (sem ACK)")

def send_tcp(msg):
    with socket.create_connection((HOST, PORT), timeout=10) as s:
        s.sendall(msg.encode('utf-8'))
        print(f"TCP → {msg.strip()}")
        try:
            s.settimeout(2)
            ack = s.recv(1024)
            if ack:
                print(f"    ACK ← {ack.decode('utf-8').strip()}")
        except:
            print("    (sem ACK)")

def main():
    print(f"ST310 Simulator → {HOST}:{PORT} ({PROTOCOL})")
    print(f"ID: {DEV_ID} | Rota: Espinosa → Mato Verde")

    trajectory = generate_trajectory()
    serial = 1

    # Envia ALIVE inicial
    if PROTOCOL == "UDP":
        send_udp(build_sa200alv(DEV_ID, FIRMWARE_VER))
    else:
        send_tcp(build_sa200alv(DEV_ID, FIRMWARE_VER))
    time.sleep(2)

    # Envia posições
    for i, (lat, lon, course) in enumerate(trajectory):
        odometer = ODOMETER + i
        msg = build_sa200stt(DEV_ID, FIRMWARE_VER, lat, lon, SPEED_KMH, course, odometer)

        if PROTOCOL == "UDP":
            send_udp(msg)
        else:
            send_tcp(msg)

        print(f"[{i+1:03d}] lat={lat:+.6f}, lon={lon:+.6f}, curso={course}°")
        serial = (serial + 1) & 0xFFFF
        time.sleep(INTERVAL)

    print("Simulação concluída.")

if __name__ == "__main__":
    if len(sys.argv) > 1:
        PROTOCOL = sys.argv[1].upper()
    if PROTOCOL not in ["UDP", "TCP"]:
        print("Uso: python st310_sim.py [UDP|TCP]")
        sys.exit(1)
    main()