#!/usr/bin/env python3
"""
Suntech ST300 Simulator - Espinosa MG → Mato Verde MG
- Protocolo: ST300 (compatível com servidor existente)
- Formato ST300STT, ST300ALV
- Mantém conexão TCP persistente
"""

import socket
import time
from datetime import datetime
import sys

# ================== CONFIGURAÇÃO ==================
HOST = "localhost"
PORT = 1150
PROTOCOL = "TCP"              # "TCP" ou "UDP"
DEV_ID = "123456789"          # IMEI do dispositivo
MODEL = "ST310"               # Modelo do dispositivo
INTERVAL = 4                  # Intervalo entre envios (segundos)
ITERATIONS = 30               # Quantidade de posições
SPEED_KMH = 50                # Velocidade em movimento (km/h)
KEEP_ALIVE = True             # Manter conexão TCP aberta

# COORDENADAS (Espinosa → Mato Verde)
ESPINOSA_LAT = -14.9257
ESPINOSA_LON = -42.8168
MATO_VERDE_LAT = -15.3951
MATO_VERDE_LON = -42.8609

# STATUS FIXOS
SATELLITES = 7
GPS_MODE = 1                  # 1 = GPS válido
BATTERY_V = "13.80"
ODOMETER = 100000             # km
IO_STATUS = 1                 # 1 = ignição ligada

# =================================================

def build_st300stt(dev_id, lat, lon, speed, heading, ignition):
    """
    Constrói pacote ST300STT no formato esperado pelo servidor:
    ST300STT;DevID;Model;SwVer;Date;Time;Cell;Lat;Lon;Speed;Heading;Sats;GPSMode;Distance;PowerV;BatteryV;IO;Mode;MsgNum

    Baseado no parser do servidor:
    - parts[4] = Date (20251101)
    - parts[5] = Time (13:10:37)
    - parts[7] = Latitude
    - parts[8] = Longitude
    - parts[9] = Speed
    - parts[10] = Heading
    - parts[last] = IO Status (para ignição)
    """
    now = datetime.now()
    date_str = now.strftime("%Y%m%d")          # 20260106
    time_str = now.strftime("%H:%M:%S")        # 23:51:39 (com ":")

    # Formatar coordenadas
    lat_str = f"{lat:.6f}"
    lon_str = f"{lon:.6f}"

    # Formatar speed e heading
    speed_str = f"{speed:.2f}"
    heading_str = f"{heading:.2f}"

    # IO Status (último campo - usado para detectar ignição)
    io_status = 1 if ignition else 0

    fields = [
        "ST300STT",
        dev_id,              # [1] Device ID (IMEI)
        MODEL,               # [2] Model
        "1.0.0",             # [3] Software Version
        date_str,            # [4] Date YYYYMMDD
        time_str,            # [5] Time HH:MM:SS
        "00129",             # [6] Cell ID
        lat_str,             # [7] Latitude
        lon_str,             # [8] Longitude
        speed_str,           # [9] Speed km/h
        heading_str,         # [10] Heading
        str(SATELLITES),     # [11] Number of satellites
        str(GPS_MODE),       # [12] GPS mode
        "0",                 # [13] Distance
        "12.50",             # [14] Power voltage
        BATTERY_V,           # [15] Battery voltage
        "0",                 # [16] IO input
        "1",                 # [17] Mode
        "0",                 # [18] Message number
        str(io_status)       # [19] IO Status (ignição)
    ]

    return ";".join(fields) + "\r\n"

def build_st300alv(dev_id):
    """
    Constrói pacote ST300ALV (Alive/Heartbeat):
    ST300ALV;DevID;Model;SwVer;Date;Time;Cell;Reserved;Mode;MsgNum
    """
    now = datetime.now()
    date_str = now.strftime("%Y%m%d")
    time_str = now.strftime("%H:%M:%S")

    fields = [
        "ST300ALV",
        dev_id,
        MODEL,
        "1.0.0",
        date_str,
        time_str,
        "00129",
        "0",
        "1",
        "0"
    ]

    return ";".join(fields) + "\r\n"

def build_st300gps(dev_id, lat, lon, speed, heading, ignition):
    """
    Constrói pacote ST300GPS (alternativa ao STT):
    Mesmo formato do STT
    """
    msg = build_st300stt(dev_id, lat, lon, speed, heading, ignition)
    return msg.replace("ST300STT", "ST300GPS")

def generate_trajectory():
    """Gera trajetória de Espinosa para Mato Verde"""
    points = []
    for i in range(ITERATIONS + 1):
        fraction = i / ITERATIONS
        lat = ESPINOSA_LAT + (MATO_VERDE_LAT - ESPINOSA_LAT) * fraction
        lon = ESPINOSA_LON + (MATO_VERDE_LON - ESPINOSA_LON) * fraction

        # Heading: Sul ligeiramente para Oeste (~185°)
        heading = 185.0 + (i % 10)

        points.append((lat, lon, heading))
    return points

def send_tcp_persistent(messages):
    """Mantém conexão TCP aberta e envia todas mensagens"""
    try:
        print(f"🔌 Conectando ao servidor {HOST}:{PORT}...")
        sock = socket.create_connection((HOST, PORT), timeout=10)
        print("✅ Conexão TCP estabelecida!")
        print()

        for i, msg in enumerate(messages):
            try:
                # Envia mensagem
                sock.sendall(msg.encode('utf-8'))
                msg_type = msg.split(";")[0]
                print(f"📤 [{i+1:03d}] {msg_type} → {msg.strip()[:80]}...")

                # Aguarda processamento
                time.sleep(0.5)

                # Tenta ler resposta
                sock.settimeout(1)
                try:
                    response = sock.recv(1024)
                    if response:
                        print(f"    📥 ACK: {response.decode('utf-8', errors='ignore').strip()}")
                except socket.timeout:
                    pass

                # Intervalo entre mensagens
                if i < len(messages) - 1:
                    time.sleep(INTERVAL)

            except Exception as e:
                print(f"    ❌ Erro ao enviar: {e}")
                break

        print()
        print("⏳ Aguardando 2s antes de fechar conexão...")
        time.sleep(2)

        sock.close()
        print("🔌 Conexão TCP fechada.")

    except Exception as e:
        print(f"❌ Erro de conexão TCP: {e}")

def send_tcp_single(msg):
    """Envia mensagem TCP em conexão única"""
    try:
        with socket.create_connection((HOST, PORT), timeout=10) as s:
            s.sendall(msg.encode('utf-8'))
            msg_type = msg.split(";")[0]
            print(f"TCP {msg_type} → {msg.strip()[:80]}...")

            time.sleep(0.5)

            try:
                s.settimeout(1)
                ack = s.recv(1024)
                if ack:
                    print(f"    ← ACK: {ack.decode('utf-8', errors='ignore').strip()}")
            except socket.timeout:
                pass

            time.sleep(1)

    except Exception as e:
        print(f"❌ Erro TCP: {e}")

def send_udp(msg):
    """Envia mensagem via UDP"""
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
            s.sendto(msg.encode('utf-8'), (HOST, PORT))
            msg_type = msg.split(";")[0]
            print(f"UDP {msg_type} → {msg.strip()[:80]}...")

            try:
                s.settimeout(2)
                ack, _ = s.recvfrom(1024)
                print(f"    ← ACK: {ack.decode('utf-8', errors='ignore').strip()}")
            except socket.timeout:
                pass
    except Exception as e:
        print(f"❌ Erro UDP: {e}")

def main():
    print("=" * 70)
    print("SUNTECH ST300 SIMULATOR")
    print("=" * 70)
    print(f"Servidor    : {HOST}:{PORT}")
    print(f"Protocolo   : {PROTOCOL}")
    print(f"IMEI        : {DEV_ID}")
    print(f"Modelo      : {MODEL}")
    print(f"Rota        : Espinosa, MG → Mato Verde, MG")
    print(f"Velocidade  : {SPEED_KMH} km/h")
    print(f"Intervalo   : {INTERVAL}s")
    print(f"Iterações   : {ITERATIONS}")
    print(f"Keep-Alive  : {KEEP_ALIVE}")
    print("=" * 70)
    print()

    # Gerar trajetória
    trajectory = generate_trajectory()

    # Preparar mensagens
    messages = []

    # 1. ALIVE inicial
    alive_msg = build_st300alv(DEV_ID)
    messages.append(alive_msg)

    # 2. Posições GPS
    for i, (lat, lon, heading) in enumerate(trajectory):
        # Primeiro pacote parado, depois em movimento
        if i == 0:
            speed = 0.0
            ignition = False  # Parado
        else:
            speed = SPEED_KMH
            ignition = True   # Em movimento

        # Usar ST300STT (recomendado) ou ST300GPS
        msg = build_st300stt(DEV_ID, lat, lon, speed, heading, ignition)
        messages.append(msg)

        # ALIVE a cada 10 posições
        if (i + 1) % 10 == 0:
            alive_msg = build_st300alv(DEV_ID)
            messages.append(alive_msg)

    # Enviar mensagens
    if PROTOCOL == "TCP" and KEEP_ALIVE:
        send_tcp_persistent(messages)
    elif PROTOCOL == "TCP" and not KEEP_ALIVE:
        for i, msg in enumerate(messages):
            send_tcp_single(msg)
            if i < len(messages) - 1:
                time.sleep(INTERVAL)
    else:  # UDP
        for i, msg in enumerate(messages):
            send_udp(msg)
            if i < len(messages) - 1:
                time.sleep(INTERVAL)

    print()
    print("=" * 70)
    print("✅ Simulação concluída!")
    print("=" * 70)

if __name__ == "__main__":
    if len(sys.argv) > 1:
        PROTOCOL = sys.argv[1].upper()

    if PROTOCOL not in ["UDP", "TCP"]:
        print("Uso: python st300_sim.py [TCP|UDP]")
        sys.exit(1)

    try:
        main()
    except KeyboardInterrupt:
        print("\n\n⚠️  Simulação interrompida pelo usuário")
        sys.exit(0)
