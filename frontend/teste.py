#!/usr/bin/env python3
"""
gt06_sim.py
Simulador GT06 (login + envio periódico de posições) para demo.traccar.org:5023
IMEI: 352812192008961
"""

import socket
import struct
import time
import binascii
from datetime import datetime, timezone

HOST = "demo.traccar.org"
PORT = 5023
IMEI = "352812192008961"   # seu IMEI
INTERVAL = 5               # segundos entre posições
ITERATIONS = 60            # quantas posições enviar

# -------------------------
# Funções utilitárias
# -------------------------

def imei_to_terminal_id_bcd(imei: str) -> bytes:
    """
    Converte IMEI (15 dígitos) para 8 bytes BCD (packed)
    - Prefixa '0' se IMEI tiver 15 dígitos
    """
    imei_digits = imei.strip()
    if not imei_digits.isdigit() or len(imei_digits) not in (15, 16):
        raise ValueError("IMEI deve ter 15 (ou 16) dígitos numéricos")
    if len(imei_digits) == 15:
        imei_digits = "0" + imei_digits  # prefixa para 16
    b = bytearray()
    for i in range(0, 16, 2):
        pair = imei_digits[i:i+2]
        byte = (int(pair[0]) << 4) | int(pair[1])
        b.append(byte & 0xFF)
    return bytes(b)  # 8 bytes

def crc16_ccitt(data: bytes) -> int:
    """Calcula CRC-16 CCITT"""
    crc = binascii.crc_hqx(data, 0)
    return crc & 0xFFFF

def build_packet(protocol_no: int, info: bytes, serial_no: int) -> bytes:
    """Monta pacote GT06"""
    length = 1 + len(info) + 2 + 2  # protocol + info + serial + crc
    serial = struct.pack(">H", serial_no & 0xFFFF)
    body = bytes([length, protocol_no]) + info + serial
    crc = crc16_ccitt(body)
    crc_bytes = struct.pack(">H", crc)
    packet = b'\x78\x78' + body + crc_bytes + b'\x0D\x0A'
    return packet

def build_login_packet(imei: str, serial_no: int = 1) -> bytes:
    """Login packet (protocol 0x01)"""
    termid = imei_to_terminal_id_bcd(imei)
    return build_packet(0x01, termid, serial_no)

def dd_to_gt06_latlon(value_dd: float) -> int:
    """Converte decimal degrees para valor GT06"""
    return int(abs(value_dd) * 60.0 * 30000.0 + 0.5)

def build_location_packet(lat: float, lon: float, speed_kmh: int, course: int, serial_no: int) -> bytes:
    """Monta pacote de localização (protocol 0x12)"""
    now = datetime.now(timezone.utc)
    dt_bytes = bytes([
        now.year % 100, now.month, now.day, now.hour, now.minute, now.second
    ])
    gps_info_len = bytes([0x0C])
    lat_bytes = struct.pack(">I", dd_to_gt06_latlon(lat))
    lon_bytes = struct.pack(">I", dd_to_gt06_latlon(lon))
    speed_byte = bytes([speed_kmh & 0xFF])
    course_bytes = struct.pack(">H", course & 0x03FF)
    mcc = struct.pack(">H", 724)            # MCC Brasil
    mnc = bytes([0x05])                     # exemplo MNC
    lac = struct.pack(">H", 0x0028)         # exemplo
    cellid = struct.pack(">I", 0x00001F3B)  # exemplo
    info = dt_bytes + gps_info_len + lat_bytes + lon_bytes + speed_byte + course_bytes + mcc + mnc + lac + cellid
    return build_packet(0x12, info, serial_no)

# -------------------------
# Main
# -------------------------

def main():
    print(f"Conectando em {HOST}:{PORT} ...")
    try:
        with socket.create_connection((HOST, PORT), timeout=10) as s:
            s.settimeout(5)
            serial = 1
            # envia login
            login_pkt = build_login_packet(IMEI, serial_no=serial)
            print("Enviando LOGIN packet (IMEI)...")
            s.sendall(login_pkt)
            # lê resposta
            try:
                resp = s.recv(1024)
                if resp:
                    print("Resposta do servidor (hex):", resp.hex())
                else:
                    print("Servidor enviou resposta vazia.")
            except socket.timeout:
                print("Nenhuma resposta do servidor após login (timeout).")
            serial += 1

            # envia posições em loop
            base_lat = -23.550520
            base_lon = -46.633308
            for i in range(ITERATIONS):
                lat = base_lat + 0.00005 * i
                lon = base_lon + 0.00006 * i
                speed = 40  # km/h
                course = (i * 5) % 360
                pkt = build_location_packet(lat, lon, speed, course, serial_no=serial)
                s.sendall(pkt)
                print(f"[{i+1}/{ITERATIONS}] enviado: {lat:.6f},{lon:.6f} serial={serial}")
                # tenta receber ACK do servidor
                try:
                    s.settimeout(0.8)
                    resp = s.recv(1024)
                    if resp:
                        print("  recv:", resp.hex())
                except socket.timeout:
                    pass
                serial = (serial + 1) & 0xFFFF
                time.sleep(INTERVAL)
    except (ConnectionRefusedError, TimeoutError) as e:
        print("Erro de conexão:", e)

if __name__ == "__main__":
    main()
