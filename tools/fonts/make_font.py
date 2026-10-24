"""Instance Unbounded[wght] at fixed weights and subset to Latin + Cyrillic."""
import sys
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer
from fontTools import subset

src, outdir = sys.argv[1], sys.argv[2]
unicodes = (list(range(0x20, 0x7F)) + list(range(0xA0, 0x100))
            + list(range(0x400, 0x460))
            + [0x2013, 0x2014, 0x2019, 0x201C, 0x201D, 0x2026, 0x2022, 0x2192,
               0x00D7, 0x2116, 0x2009, 0x202F])
for wght in (700, 900):
    f = TTFont(src)
    inst = instancer.instantiateVariableFont(f, {"wght": wght})
    opts = subset.Options()
    opts.layout_features = ['kern', 'liga', 'tnum', 'lnum']
    opts.name_IDs = ['*']
    opts.hinting = False
    opts.desubroutinize = True
    s = subset.Subsetter(opts)
    s.populate(unicodes=unicodes)
    s.subset(inst)
    out = '%s/unbounded_%d.ttf' % (outdir, wght)
    inst.save(out)
    print(out)
