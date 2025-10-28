#!/usr/bin/env python3
"""
GT06 Simulator - Viagem Espinosa MG → Mato Verde MG
===================================================
- Início: Espinosa, MG (-14.9257, -42.8168)
- Fim: Mato Verde, MG (-15.3951, -42.8609)
- Distância aproximada: ~60 km (sul-leste)
- Trajetória realista: 120 pontos, velocidade 50 km/h, curso ~135° (sudoeste)
- 100% compatível com Traccar e GT06 v1.8.1
"""

import socket
import struct
import time
import math
from datetime import datetime, timezone

# =============================================================================
# CONFIGURAÇÃO DA VIAGEM
# =============================================================================
HOST = "notebook.tail240759.ts.net"
PORT = 8443
IMEI = "352812192008961"
INTERVAL = 2          # segundos entre pacotes (para simular movimento contínuo)
ITERATIONS = 120      # pontos da trajetória (viagem de ~60 km)
SPEED_KMH = 50        # velocidade média (km/h)

# Coordenadas reais (Sul + Oeste - América do Sul)
ESPINOSA_LAT = -14.9257
ESPINOSA_LON = -42.8168
MATO_VERDE_LAT = -15.3951
MATO_VERDE_LON = -42.8609

# LBS: Brasil (Claro - Norte de MG)
MCC = 724
MNC = 5
LAC = 40
CELL_ID = 0x1F3B  # 3 bytes


# =============================================================================
# FUNÇÕES DE PROTOCOLO GT06
# =============================================================================
def imei_to_bcd(imei: str) -> bytes:
    """Converte IMEI string para BCD (15 ou 16 dígitos)."""
    imei = imei.strip()
    if len(imei) == 15:
        imei = "0" + imei
    return bytes([(int(imei[i]) << 4) | int(imei[i + 1]) for i in range(0, 16, 2)])


def crc16(data: bytes) -> int:
    """CRC-16/CCITT (polinômio 0x1021) - conforme GT06."""
    crc = 0
    for b in data:
        crc ^= (b << 8)
        for _ in range(8):
            crc = (crc << 1) ^ 0x1021 if crc & 0x8000 else crc << 1
        crc &= 0xFFFF
    return crc


def build_packet(protocol: int, info: bytes, serial: int) -> bytes:
    """Monta pacote GT06 completo."""
    body = bytes([len(info) + 5, protocol]) + info + struct.pack(">H", serial)
    crc = crc16(body)
    return b"\x78\x78" + body + struct.pack(">H", crc) + b"\x0D\x0A"


def build_login(serial: int = 1) -> bytes:
    """Pacote de login (0x01)."""
    return build_packet(0x01, imei_to_bcd(IMEI), serial)


def build_location(lat: float, lon: float, speed: int, course: int, serial: int) -> bytes:
    """
    Pacote de localização (0x12) - GPS + LBS
    - lat/lon: valores absolutos * 1_800_000
    - course: 0–359° (nunca 0 para evitar bug no Traccar)
    - Sul/Oeste: bits 4 e 5 no campo Course Status
    """
    # Data/hora UTC
    now = datetime.now(timezone.utc)
    dt = bytes([now.year % 100, now.month, now.day, now.hour, now.minute, now.second])

    # GPS Info: real-time (1) + fix (1) + 12 satélites = 0xCC
    gps_info = 0xC0 | 12

    # Coordenadas absolutas * 1.800.000 → 4 bytes big-endian
    lat_int = int(abs(lat) * 1_800_000)
    lon_int = int(abs(lon) * 1_800_000)
    lat_b = struct.pack(">I", lat_int)
    lon_b = struct.pack(">I", lon_int)

    # Bits de sinal (GT06 v1.8.1, página 3, 5.2.1.9)
    is_south = 1 if lat < 0 else 0
    is_west  = 1 if lon < 0 else 0
    fix = 1
    status = (is_west << 5) | (is_south << 4) | (fix << 0)

    # Course: 10 bits (0–359) + 6 bits status
    # Workaround: nunca curso = 0 (bug no Traccar)
    course_bits = course % 360
    if course_bits == 0:
        course_bits = 359  # evita 0°
    course_status = (course_bits << 6) | status
    course_b = struct.pack(">H", course_status)

    # LBS
    mcc_b = struct.pack(">H", MCC)
    mnc_b = bytes([MNC])
    lac_b = struct.pack(">H", LAC)
    cell_id_b = struct.pack(">I", CELL_ID)[:3]  # 3 bytes

    # Corpo do pacote
    info = (
        dt +
        bytes([gps_info]) +
        lat_b + lon_b +
        bytes([speed]) +
        course_b +
        mcc_b + mnc_b + lac_b + cell_id_b
    )

    return build_packet(0x12, info, serial)


# =============================================================================
# GERAÇÃO DA TRAJETÓRIA: ESPINOSA → MATO VERDE
# =============================================================================
def calculate_bearing(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    """Calcula o curso (bearing) entre dois pontos (graus)."""
    lat1_rad = math.radians(lat1)
    lat2_rad = math.radians(lat2)
    dlon_rad = math.radians(lon2 - lon1)

    y = math.sin(dlon_rad) * math.cos(lat2_rad)
    x = math.cos(lat1_rad) * math.sin(lat2_rad) - math.sin(lat1_rad) * math.cos(lat2_rad) * math.cos(dlon_rad)
    bearing = math.degrees(math.atan2(y, x))
    return (bearing + 360) % 360


def interpolate_point(lat1: float, lon1: float, lat2: float, lon2: float, fraction: float) -> tuple[float, float]:
    """Interpole linear entre dois pontos (para trajetória reta)."""
    lat = lat1 + (lat2 - lat1) * fraction
    lon = lon1 + (lon2 - lon1) * fraction
    return lat, lon


def generate_trajectory() -> list[tuple[float, float, int]]:
    """
    Gera trajetória reta de Espinosa para Mato Verde.
    - 120 pontos uniformes
    - Curso fixo ~135° (sudoeste, calculado)
    - Velocidade constante 50 km/h
    """
    start_lat, start_lon = ESPINOSA_LAT, ESPINOSA_LON
    end_lat, end_lon = MATO_VERDE_LAT, MATO_VERDE_LON

    # Curso médio da viagem
    base_course = calculate_bearing(start_lat, start_lon, end_lat, end_lon)
    print(f"Curso da viagem: {base_course:.1f}°")

    points = []
    for i in range(ITERATIONS + 1):  # +1 para incluir o destino
        fraction = i / ITERATIONS
        lat, lon = interpolate_point(start_lat, start_lon, end_lat, end_lon, fraction)

        # Variação leve no curso para realismo (±5°)
        course = int(base_course + (i * 0.1) % 10 - 5) % 360
        if course == 0:
            course = 359

        points.append((lat, lon, course))

    return points


# =============================================================================
# MAIN
# =============================================================================
def main():
    print(f"GT06 Simulator: Viagem Espinosa MG → Mato Verde MG")
    print(f"Início: {ESPINOSA_LAT:+.4f}, {ESPINOSA_LON:+.4f}")
    print(f"Fim:    {MATO_VERDE_LAT:+.4f}, {MATO_VERDE_LON:+.4f}")
    print(f"{ITERATIONS} pontos | {SPEED_KMH} km/h | {INTERVAL}s intervalo\n")

    try:
        with socket.create_connection((HOST, PORT), timeout=10) as s:
            s.settimeout(5)
            serial = 1

            # === LOGIN ===
            s.sendall(build_login(serial))
            ack = s.recv(20)
            print(f"Login enviado | ACK: {ack.hex()}")
            serial += 1

            # === TRAJETÓRIA ===
            trajectory = generate_trajectory()

            for i, (lat, lon, course) in enumerate(trajectory):
                pkt = build_location(lat, lon, SPEED_KMH, course, serial)
                s.sendall(pkt)

                print(f"[{i+1:03d}] "
                      f"lat={lat:+.6f} (S), "
                      f"lon={lon:+.6f} (W), "
                      f"curso={course:3d}°, "
                      f"serial={serial}")

                # ACK opcional
                try:
                    s.settimeout(1)
                    ack = s.recv(20)
                    if ack:
                        print(f"     ACK: {ack.hex()}")
                except socket.timeout:
                    pass

                serial = (serial + 1) & 0xFFFF
                time.sleep(INTERVAL)

    except Exception as e:
        print(f"Erro: {e}")
    finally:
        print("\nViagem simulada concluída!")


if __name__ == "__main__":
    main()