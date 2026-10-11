#!/usr/bin/env python3
"""Descarga y verifica la variante TFLite de YAMNet usada en el prototipo.

Este archivo se obtiene de un espejo identificado de un modelo Apache-2.0;
revisar su procedencia/licencia antes de redistribuir una compilación pública.
"""
import hashlib
import pathlib
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[1]
TARGET = ROOT / "app/src/main/assets/yamnet.tflite"
EXPECTED = "10c95ea3eb9a7bb4cb8bddf6feb023250381008177ac162ce169694d05c317de"
URLS = [
    "https://huggingface.co/thelou1s/yamnet/resolve/main/lite-model_yamnet_classification_tflite_1.tflite",
    "https://tfhub.dev/google/lite-model/yamnet/classification/tflite/1?lite-format=tflite",
]

def valid(content):
    return (len(content) > 1_000_000 and content[4:8] == b"TFL3" and
            hashlib.sha256(content).hexdigest() == EXPECTED)

if TARGET.exists() and valid(TARGET.read_bytes()):
    print("YAMNet presente y validado")
else:
    for url in URLS:
        try:
            request = urllib.request.Request(url, headers={"User-Agent":"LadrIA-MVP/0.1"})
            with urllib.request.urlopen(request, timeout=120) as resp:
                content = resp.read()
            if not valid(content):
                raise ValueError("Archivo descargado no coincide con SHA-256")
            TARGET.write_bytes(content)
            print(f"Modelo verificado y copiado: {TARGET}")
            break
        except Exception as error:
            print(f"No disponible {url}: {error}")
    else:
        raise SystemExit("No se pudo descargar YAMNet verificable. No se construirá un APK incompleto.")
