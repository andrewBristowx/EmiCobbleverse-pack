"""Lector/escritor minimo de NBT (gzip, big-endian) para estructuras de Minecraft."""
import gzip, io, struct

class L(list):   # lista NBT con tipo de elemento
    def __init__(self, et, items=()): super().__init__(items); self.et = et
class Byte(int): pass
class Short(int): pass
class Long(int): pass
class Float(float): pass
class Double(float): pass
class IntArray(list): pass
class LongArray(list): pass
class ByteArray(bytes): pass

def parse(data):
    f = io.BytesIO(gzip.decompress(data) if data[:2] == b'\x1f\x8b' else data)
    def rs(): n = struct.unpack('>H', f.read(2))[0]; return f.read(n).decode('utf8', 'replace')
    def rp(t):
        if t == 1: return Byte(struct.unpack('>b', f.read(1))[0])
        if t == 2: return Short(struct.unpack('>h', f.read(2))[0])
        if t == 3: return struct.unpack('>i', f.read(4))[0]
        if t == 4: return Long(struct.unpack('>q', f.read(8))[0])
        if t == 5: return Float(struct.unpack('>f', f.read(4))[0])
        if t == 6: return Double(struct.unpack('>d', f.read(8))[0])
        if t == 7: n = struct.unpack('>i', f.read(4))[0]; return ByteArray(f.read(n))
        if t == 8: return rs()
        if t == 9:
            et = f.read(1)[0]; n = struct.unpack('>i', f.read(4))[0]; return L(et, [rp(et) for _ in range(n)])
        if t == 10:
            d = {}
            while True:
                tt = f.read(1)[0]
                if tt == 0: return d
                k = rs(); d[k] = rp(tt)
        if t == 11: n = struct.unpack('>i', f.read(4))[0]; return IntArray(struct.unpack('>%di' % n, f.read(4 * n)))
        if t == 12: n = struct.unpack('>i', f.read(4))[0]; return LongArray(struct.unpack('>%dq' % n, f.read(8 * n)))
    t = f.read(1)[0]; rs(); return rp(t)

def dumps(root, name=''):
    out = io.BytesIO()
    def ws(s): b = s.encode('utf8'); out.write(struct.pack('>H', len(b))); out.write(b)
    def tid(v):
        if isinstance(v, Byte): return 1
        if isinstance(v, Short): return 2
        if isinstance(v, Long): return 4
        if isinstance(v, Float): return 5
        if isinstance(v, Double): return 6
        if isinstance(v, bool): return 1
        if isinstance(v, int): return 3
        if isinstance(v, float): return 6
        if isinstance(v, ByteArray): return 7
        if isinstance(v, str): return 8
        if isinstance(v, L): return 9
        if isinstance(v, dict): return 10
        if isinstance(v, IntArray): return 11
        if isinstance(v, LongArray): return 12
        raise TypeError(type(v))
    def wp(v):
        t = tid(v)
        if t == 1: out.write(struct.pack('>b', int(v)))
        elif t == 2: out.write(struct.pack('>h', v))
        elif t == 3: out.write(struct.pack('>i', v))
        elif t == 4: out.write(struct.pack('>q', v))
        elif t == 5: out.write(struct.pack('>f', v))
        elif t == 6: out.write(struct.pack('>d', v))
        elif t == 7: out.write(struct.pack('>i', len(v))); out.write(bytes(v))
        elif t == 8: ws(v)
        elif t == 9:
            out.write(bytes([v.et])); out.write(struct.pack('>i', len(v)))
            for x in v: wp(x)
        elif t == 10:
            for k, x in v.items(): out.write(bytes([tid(x)])); ws(k); wp(x)
            out.write(b'\x00')
        elif t == 11: out.write(struct.pack('>i', len(v))); out.write(struct.pack('>%di' % len(v), *v))
        elif t == 12: out.write(struct.pack('>i', len(v))); out.write(struct.pack('>%dq' % len(v), *v))
    out.write(bytes([10])); ws(name); wp(root)
    return gzip.compress(out.getvalue())
