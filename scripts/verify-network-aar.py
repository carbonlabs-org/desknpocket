"""Inspect the generated bridge without loading its native code (stdlib only)."""
import hashlib
import io
import json
import struct
import sys
import zipfile
from pathlib import Path


def inspect(path):
    result = {"sha256": hashlib.sha256(path.read_bytes()).hexdigest(), "abis": {}}
    machines = {"armeabi-v7a": (1, 40), "arm64-v8a": (2, 183),
                "x86": (1, 3), "x86_64": (2, 62)}
    with zipfile.ZipFile(path) as aar:
        if aar.testzip() is not None:
            raise ValueError("AAR CRC failure")
        aar.getinfo("AndroidManifest.xml")
        with zipfile.ZipFile(io.BytesIO(aar.read("classes.jar"))) as jar:
            if jar.testzip() is not None:
                raise ValueError("Binding JAR CRC failure")
            classes = sorted(n for n in jar.namelist() if n.endswith(".class"))
            for name in ("libtailscale/Libtailscale.class", "libtailscale/AppContext.class",
                         "libtailscale/IPNService.class"):
                if name not in classes:
                    raise ValueError(f"Missing binding {name}")
            result["binding_class_count"] = len(classes)
        for abi, (elf_class, machine) in machines.items():
            data = aar.read(f"jni/{abi}/libgojni.so")
            if data[:6] != b"\x7fELF" + bytes((elf_class, 1)):
                raise ValueError(f"Unexpected ELF format: {abi}")
            if struct.unpack_from("<H", data, 18)[0] != machine:
                raise ValueError(f"Wrong ELF architecture: {abi}")
            is64 = elf_class == 2
            phoff = struct.unpack_from("<Q" if is64 else "<I", data, 32 if is64 else 28)[0]
            entsize, count = struct.unpack_from("<HH", data, 54 if is64 else 42)
            loads = []
            for i in range(count):
                fields = struct.unpack_from("<IIQQQQQQ" if is64 else "<IIIIIIII",
                                            data, phoff + i * entsize)
                if fields[0] != 1:  # PT_LOAD
                    continue
                offset, address = (fields[2], fields[3]) if is64 else (fields[1], fields[2])
                alignment = fields[7]
                if alignment < 16384 or alignment & (alignment - 1) or (address - offset) % 16384:
                    raise ValueError(f"ELF load segment is not 16 KB aligned: {abi}")
                loads.append(alignment)
            if not loads:
                raise ValueError(f"No ELF load segments: {abi}")
            result["abis"][abi] = {"bytes": len(data), "load_alignments": loads}
    result["scope"] = "Archive, bindings and ELF alignment only; APK packaging and device execution untested."
    return result


if __name__ == "__main__":
    print(json.dumps(inspect(Path(sys.argv[1])), indent=2))
