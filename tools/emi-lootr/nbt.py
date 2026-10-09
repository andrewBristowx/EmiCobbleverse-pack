import gzip,struct,io
class P:
    __slots__=('t','v')
    def __init__(s,t,v): s.t=t; s.v=v
    def __repr__(s): return 'P(%d,%r)'%(s.t,s.v)
class L(list):
    def __init__(s,et=0,items=()): super().__init__(items); s.et=et
FMT={1:'>b',2:'>h',3:'>i',4:'>q',5:'>f',6:'>d'}
def rd(f,t):
    u=lambda fm: struct.unpack(fm,f.read(struct.calcsize(fm)))[0]
    if t in FMT: return P(t,u(FMT[t]))
    if t==7: n=u('>i'); return P(7,f.read(n))
    if t==8: n=u('>H'); return P(8,f.read(n).decode('utf-8','surrogatepass'))
    if t==9:
        et=u('>B'); n=u('>i'); return L(et,[rd(f,et) for _ in range(n)])
    if t==10:
        d={}
        while True:
            tt=u('>B')
            if tt==0: return d
            n=u('>H'); k=f.read(n).decode('utf-8','surrogatepass'); d[k]=rd(f,tt)
    if t==11: n=u('>i'); return P(11,list(struct.unpack('>%di'%n,f.read(4*n))))
    if t==12: n=u('>i'); return P(12,list(struct.unpack('>%dq'%n,f.read(8*n))))
    raise Exception('tag %d'%t)
def tid(x):
    if isinstance(x,dict): return 10
    if isinstance(x,L): return 9
    return x.t
def wr(o,x):
    t=tid(x)
    if t in FMT: o.write(struct.pack(FMT[t],x.v))
    elif t==7: o.write(struct.pack('>i',len(x.v))); o.write(x.v)
    elif t==8: b=x.v.encode('utf-8','surrogatepass'); o.write(struct.pack('>H',len(b))); o.write(b)
    elif t==9:
        o.write(struct.pack('>Bi',x.et,len(x)))
        for e in x: wr(o,e)
    elif t==10:
        for k,v in x.items():
            b=k.encode('utf-8','surrogatepass'); o.write(struct.pack('>BH',tid(v),len(b))); o.write(b); wr(o,v)
        o.write(b'\x00')
    elif t==11: o.write(struct.pack('>i',len(x.v))); o.write(struct.pack('>%di'%len(x.v),*x.v))
    elif t==12: o.write(struct.pack('>i',len(x.v))); o.write(struct.pack('>%dq'%len(x.v),*x.v))
def load(b):
    f=io.BytesIO(gzip.decompress(b)); t=struct.unpack('>B',f.read(1))[0]; n=struct.unpack('>H',f.read(2))[0]; name=f.read(n).decode()
    return name,rd(f,t)
def dump(name,root):
    o=io.BytesIO(); o.write(struct.pack('>B',tid(root))); nb=name.encode(); o.write(struct.pack('>H',len(nb))); o.write(nb); wr(o,root)
    return gzip.compress(o.getvalue(),mtime=0)
