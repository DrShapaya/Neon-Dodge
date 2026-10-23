"""Генерирует adaptive-icon слои Neon Dodge из той же геометрии, что и icon.svg."""
import io
import sys

res = sys.argv[1]


def circle(cx, cy, r):
    return "M%g,%g m-%g,0 a%g,%g 0 1,1 %g,0 a%g,%g 0 1,1 -%g,0" % (cx, cy, r, r, r, 2 * r, r, r, 2 * r)


HEX_BIG = "M84,41 L75.5,55.7 L58.5,55.7 L50,41 L58.5,26.3 L75.5,26.3 Z"
HEX_ORANGE = "M37,31 L33.5,37 L26.5,37 L23,31 L26.5,25 L33.5,25 Z"
HEX_LIME = "M86,82 L83.5,86.3 L78.5,86.3 L76,82 L78.5,77.7 L83.5,77.7 Z"
TRAIL = "M30,98 C30,88 32,80 36,74 C38,69 40,67 43,66"
SPARK = "M56,52.5 Q56.6,55.4 59.5,56 Q56.6,56.6 56,59.5 Q55.4,56.6 52.5,56 Q55.4,55.4 56,52.5 Z"

HEAD = '''<?xml version="1.0" encoding="utf-8"?>
<!-- Сгенерировано tools/icon/make_vectors.py из tools/icon/icon.svg -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
'''


def radial_fill(path, cx, cy, r, color, alpha_hex):
    return '''    <path android:pathData="%s">
        <aapt:attr name="android:fillColor">
            <gradient
                android:type="radial"
                android:centerX="%g"
                android:centerY="%g"
                android:gradientRadius="%g"
                android:startColor="#%s%s"
                android:endColor="#00%s" />
        </aapt:attr>
    </path>
''' % (path, cx, cy, r, alpha_hex, color, color)


def fill(path, color, alpha=None):
    a = '' if alpha is None else '\n        android:fillAlpha="%g"' % alpha
    return '''    <path
        android:fillColor="#%s"%s
        android:pathData="%s" />
''' % (color, a, path)


def stroke(path, color, width, alpha=None, fill_color=None, fill_alpha=None):
    out = '    <path\n'
    if fill_color:
        out += '        android:fillColor="#%s"\n' % fill_color
        if fill_alpha is not None:
            out += '        android:fillAlpha="%g"\n' % fill_alpha
    out += '        android:strokeColor="#%s"\n' % color
    if alpha is not None:
        out += '        android:strokeAlpha="%g"\n' % alpha
    out += '        android:strokeWidth="%g"\n' % width
    out += '        android:strokeLineJoin="round"\n'
    out += '        android:strokeLineCap="round"\n'
    out += '        android:pathData="%s" />\n' % path
    return out


# ---------- background ----------
bg = HEAD
bg += '''    <path android:pathData="M0,0 L108,0 L108,108 L0,108 Z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:type="radial"
                android:centerX="41"
                android:centerY="67"
                android:gradientRadius="81"
                android:startColor="#1B2C70"
                android:centerColor="#0B1030"
                android:endColor="#05060F" />
        </aapt:attr>
    </path>
'''
for (x, y, r, a) in [(24, 44, 0.7, 0.6), (82, 74, 0.8, 0.5), (74, 86, 0.6, 0.4),
                     (36, 22, 0.6, 0.5), (88, 28, 0.7, 0.4)]:
    bg += fill(circle(x, y, r), "DDF2FF", a)
bg += '</vector>\n'

# ---------- foreground ----------
fg = HEAD
fg += '''    <!-- Сцена «на волосок»: сфера проскакивает мимо астероида. -->
    <group
        android:pivotX="54"
        android:pivotY="56"
        android:scaleX="0.8"
        android:scaleY="0.8"
        android:translateY="-1">
'''
body = ''
body += radial_fill(circle(67, 41, 30), 67, 41, 30, "FF4FD8", "8C")
body += '''    <group
        android:pivotX="67"
        android:pivotY="41"
        android:rotation="12">
'''
body += stroke(HEX_BIG, "FF4FD8", 3.4, fill_color="FF4FD8", fill_alpha=0.32)
body += stroke(HEX_BIG, "FFD6F5", 1, alpha=0.7)
body += fill(circle(67, 41, 4), "FF4FD8", 0.8)
body += '    </group>\n'
body += stroke(HEX_ORANGE, "FF9E4F", 2.2, fill_color="FF9E4F", fill_alpha=0.12)
body += stroke(HEX_LIME, "B6FF5C", 2, fill_color="B6FF5C", fill_alpha=0.15)
body += '''    <path
        android:strokeWidth="7"
        android:strokeLineCap="round"
        android:pathData="%s">
        <aapt:attr name="android:strokeColor">
            <gradient
                android:type="linear"
                android:startX="30"
                android:startY="98"
                android:endX="43"
                android:endY="66"
                android:startColor="#005FF3FF"
                android:endColor="#E65FF3FF" />
        </aapt:attr>
    </path>
''' % TRAIL
body += fill(circle(25, 88, 1.6), "5FF3FF", 0.5)
body += fill(circle(37, 92, 1.2), "5FF3FF", 0.4)
body += radial_fill(circle(45, 64, 24), 45, 64, 24, "5FF3FF", "BF")
body += stroke(circle(45, 64, 13.5), "FFFFFF", 2, alpha=0.75)
body += fill(circle(45, 64, 10), "5FF3FF")
body += fill(circle(45, 64, 6.3), "BFFAFF")
body += fill(circle(45, 64, 3), "FFFFFF")
body += fill(SPARK, "FFF3C4")
fg += '\n'.join('    ' + l if l else l for l in body.split('\n'))
fg = fg.rstrip(' ') + '    </group>\n</vector>\n'

# ---------- monochrome (тематические иконки Android 13+) ----------
mono = HEAD
mono += '''    <group
        android:pivotX="54"
        android:pivotY="56"
        android:scaleX="0.8"
        android:scaleY="0.8"
        android:translateY="-1">
        <group
            android:pivotX="67"
            android:pivotY="41"
            android:rotation="12">
'''
mono += '        ' + stroke(HEX_BIG, "FFFFFF", 3.4).replace('\n    ', '\n            ').lstrip()
mono += '        </group>\n'
for p, w in [(HEX_ORANGE, 2.2), (HEX_LIME, 2)]:
    mono += '        ' + stroke(p, "FFFFFF", w).replace('\n    ', '\n        ').lstrip()
mono += '        ' + stroke(TRAIL, "FFFFFF", 5, alpha=0.6).replace('\n    ', '\n        ').lstrip()
mono += '        ' + stroke(circle(45, 64, 13.5), "FFFFFF", 2).replace('\n    ', '\n        ').lstrip()
mono += '        ' + fill(circle(45, 64, 10), "FFFFFF").replace('\n    ', '\n        ').lstrip()
mono += '        ' + fill(SPARK, "FFFFFF").replace('\n    ', '\n        ').lstrip()
mono += '    </group>\n</vector>\n'

for name, text in [("ic_launcher_background.xml", bg), ("ic_launcher_foreground.xml", fg),
                   ("ic_launcher_monochrome.xml", mono)]:
    io.open(res + "/drawable/" + name, "w", encoding="utf-8", newline="\n").write(text)

adaptive = '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
'''
for name in ["ic_launcher.xml", "ic_launcher_round.xml"]:
    io.open(res + "/mipmap-anydpi-v26/" + name, "w", encoding="utf-8", newline="\n").write(adaptive)
print("ok")
