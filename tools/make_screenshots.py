from PIL import Image, ImageDraw, ImageFont, ImageFilter
import math, random, shutil, os
W,H=1080,2400
random.seed(17)
ROOT=os.path.dirname(os.path.dirname(__file__)) if '__file__' in globals() else r'D:\DS\Work\Projects\NeonDodge'
OUT=os.path.join(ROOT,'screenshots'); STORE=os.path.join(ROOT,'store','screenshots')
os.makedirs(OUT,exist_ok=True); os.makedirs(STORE,exist_ok=True)
font_dir=r'C:\Windows\Fonts'
def font(name,size):
    for n in [name, name.replace('.ttf','.TTF')]:
        p=os.path.join(font_dir,n)
        if os.path.exists(p): return ImageFont.truetype(p,size)
    return ImageFont.truetype(os.path.join(font_dir,'arial.ttf'),size)
REG=font('segoeui.ttf',32); MED=font('segoeuib.ttf',32); BOLD=font('segoeuib.ttf',48); COND=font('arialbd.ttf',34); HUGE=font('impact.ttf',132)

def bg():
    im=Image.new('RGB',(W,H)); px=im.load()
    for y in range(H):
        t=y/(H-1)
        for x in range(W):
            n=math.sin(x/370)+math.sin(y/270)
            px[x,y]=(int(8+5*t), int(7+14*t), int(30+24*t))
    d=ImageDraw.Draw(im,'RGBA')
    for _ in range(150):
        x=random.randrange(W); y=random.randrange(H); r=random.choice([1,2,3,4])
        d.ellipse((x-r,y-r,x+r,y+r),fill=(180,215,235,random.randrange(90,210)))
    return im

def glow_circle(im,xy,r,color,alpha=200):
    layer=Image.new('RGBA',(W,H)); d=ImageDraw.Draw(layer)
    for k in range(5,0,-1):
        rr=r+k*18; a=alpha//(k+1)
        d.ellipse((xy[0]-rr,xy[1]-rr,xy[0]+rr,xy[1]+rr),fill=(*color,a))
    d.ellipse((xy[0]-r,xy[1]-r,xy[0]+r,xy[1]+r),fill=(*color,255))
    im.alpha_composite(layer)

def hexagon(d,x,y,r,color,fill=(20,30,55,180),width=7):
    pts=[(x+math.cos(math.radians(i*60))*r,y+math.sin(math.radians(i*60))*r) for i in range(6)]
    d.polygon(pts,fill=fill); d.line(pts+[pts[0]],fill=(*color,255),width=width,joint='curve')

def centered(d,text,y,f,fill):
    box=d.textbbox((0,0),text,font=f); d.text(((W-(box[2]-box[0]))/2,y),text,font=f,fill=fill)

def rounded(d,box,r,fill,outline=None,w=3): d.rounded_rectangle(box,r,fill=fill,outline=outline,width=w)
def label(d,text,x,y,f=REG,fill=(210,230,240,255),anchor='la'): d.text((x,y),text,font=f,fill=fill,anchor=anchor)

def player(im,x,y):
    glow_circle(im,(x,y),34,(95,243,255),120); d=ImageDraw.Draw(im,'RGBA')
    d.ellipse((x-34,y-34,x+34,y+34),fill=(95,243,255,255),outline=(95,243,255,255),width=5)
    d.ellipse((x-18,y-18,x+18,y+18),fill=(190,250,255,255)); d.ellipse((x-7,y-7,x+7,y+7),fill='white')

def base_menu():
    im=bg().convert('RGBA'); d=ImageDraw.Draw(im,'RGBA')
    # top bar
    rounded(d,(48,56,340,136),38,(13,23,48,220),(255,215,94,150),3)
    d.polygon([(68,96),(80,80),(92,96),(80,112)],fill=(255,215,94,255)); label(d,'1 250',110,96,font('segoeuib.ttf',34),(255,233,166,255),'lm')
    d.ellipse((956,56,1036,136),fill=(13,23,48,220),outline=(160,190,210,170),width=3); d.ellipse((980,80,1012,112),outline=(220,235,245,255),width=5); d.ellipse((992,92,1000,100),fill=(220,235,245,255))
    centered(d,'NEON',268,HUGE,(255,255,255,255)); centered(d,'DODGE',400,HUGE,(255,236,251,255)); centered(d,'уклоняйся от астероидов',565,font('segoeui.ttf',34),(180,205,220,230))
    # decor
    hexagon(d,145,700,66,(255,79,216),(47,12,65,190),7); hexagon(d,930,610,48,(255,158,79),(63,34,12,180),7); hexagon(d,865,840,32,(182,255,92),(32,58,18,180),7); player(im,540,780)
    # mode selector
    rounded(d,(48,990,1032,1096),54,(8,18,38,220),(70,170,190,210),3)
    seg=[('КЛАССИКА',48,365,(95,243,255)),('ХАРДКОР',371,692,(255,100,120)),('ДЗЕН',698,1032,(164,107,255))]
    for txt,l,r,col in seg:
        rounded(d,(l+6,996,r-6,1090),42,(*col,150) if txt=='КЛАССИКА' else (10,20,40,220),(*col,210),3); centered_text=(txt)
        box=d.textbbox((0,0),txt,font=COND); d.text(((l+r-box[2]+box[0])/2,1043),txt,font=COND,fill=(235,247,250,255),anchor='mm')
    label(d,'Три жизни, сложность растёт',58,1144,font('segoeui.ttf',28),(160,180,200,230)); label(d,'РЕКОРД  2 540',1022,1144,font('segoeuib.ttf',29),(220,240,248,255),'ra')
    rounded(d,(48,1200,1032,1350),75,(83,238,255,240),(95,243,255,255),4); centered(d,'ИГРАТЬ',1240,font('segoeuib.ttf',55),(5,22,38,255))
    rounded(d,(48,1380,1032,1500),30,(53,18,70,210),(255,79,216,210),3); d.polygon([(100,1415),(108,1432),(126,1440),(108,1448),(100,1465),(92,1448),(74,1440),(92,1432)],fill=(255,79,216,255)); label(d,'ИСПЫТАНИЕ ДНЯ',160,1422,font('segoeuib.ttf',32),(255,240,250,255)); label(d,'Одна попытка, одинаковая у всех',160,1460,font('segoeui.ttf',24),(255,159,233,230))
    names=['ЗАДАНИЯ','СКИНЫ','СТАТИСТИКА','ПРАВИЛА']; icons=['flag','orb','bars','doc']; x0=48; gap=18; tw=(984-54)//4
    for i,n in enumerate(names):
        l=x0+i*(tw+gap); r=l+tw; rounded(d,(l,1550,r,1704),30,(20,33,57,230),(100,150,175,180),3); ix=(l+r)//2; iy=1602
        if icons[i]=='orb': d.ellipse((ix-20,iy-20,ix+20,iy+20),outline=(190,230,245,255),width=5); d.ellipse((ix-7,iy-7,ix+7,iy+7),fill=(190,230,245,255))
        elif icons[i]=='bars':
            for k,h in enumerate([24,38,52]): d.rounded_rectangle((ix-30+k*22,iy-h//2,ix-14+k*22,iy+h//2),5,fill=(190,230,245,255))
        elif icons[i]=='flag': d.polygon([(ix-20,iy-28),(ix+22,iy-16),(ix-20,iy-4)],fill=(190,230,245,255)); d.line((ix-20,iy-28,ix-20,iy+28),fill=(190,230,245,255),width=5)
        else: d.rectangle((ix-20,iy-26,ix+20,iy+26),outline=(190,230,245,255),width=5); d.line((ix-10,iy-8,ix+12,iy-8),fill=(190,230,245,255),width=4); d.line((ix-10,iy+8,ix+12,iy+8),fill=(190,230,245,255),width=4)
        label(d,n,(l+r)//2,1670,font('arialbd.ttf',24),(207,228,239,255),'mm')
    rounded(d,(48,1770,1032,1885),28,(9,17,34,220),(85,160,185,130),3); centered(d,'Версия 1.7  ·  Офлайн  ·  Без рекламы',1805,font('segoeui.ttf',26),(160,185,201,240))
    return im

def make_game():
    im=bg().convert('RGBA'); d=ImageDraw.Draw(im,'RGBA')
    # HUD
    for x in [58,122,186]:
        label(d,'♥',x,125,font('segoeui.ttf',58),(255,91,116,255),'mm')
    centered(d,'1 284',112,font('segoeuib.ttf',88),(225,250,255,255)); centered(d,'РЕКОРД  2 540',224,font('segoeuib.ttf',30),(130,165,185,255))
    rounded(d,(886,72,1028,214),72,(12,25,48,210),(100,155,180,200),3); label(d,'Ⅱ',957,143,font('segoeuib.ttf',56),(195,230,240,255),'mm')
    # combo and buffs
    rounded(d,(410,278,670,350),36,(27,21,58,220),(166,107,255,190),3); centered(d,'КОМБО  ×7',294,font('segoeuib.ttf',32),(245,220,255,255))
    label(d,'ЩИТ',70,286,font('segoeuib.ttf',24),(137,242,255,255)); label(d,'МАГНИТ  6с',70,326,font('segoeuib.ttf',23),(255,215,94,255)); label(d,'×2,4',1016,314,font('segoeuib.ttf',30),(255,239,166,255),'ra')
    # objects
    hexagon(d,240,690,38,(95,243,255),(13,67,78,210),6); hexagon(d,820,570,54,(255,215,94),(66,45,8,200),7); hexagon(d,180,1290,50,(255,79,216),(65,15,62,210),7); hexagon(d,720,1120,36,(182,255,92),(26,63,16,210),7); hexagon(d,900,1500,42,(255,158,79),(66,35,10,210),7)
    # split asteroid
    hexagon(d,465,880,48,(255,215,94),(60,47,10,200),6); d.line((440,850,490,910),fill=(255,245,180,220),width=4)
    # mine spikes
    cx,cy=670,1420; pts=[]
    for i in range(12):
        a=math.radians(i*30); rr=54 if i%2==0 else 38; pts.append((cx+math.cos(a)*rr,cy+math.sin(a)*rr))
    d.polygon(pts,fill=(55,20,66,230)); d.line(pts+[pts[0]],fill=(255,79,216,255),width=6)
    # pickups star and heart
    star=[]
    for i in range(8):
        a=math.radians(-90+i*45); rr=52 if i%2==0 else 22; star.append((540+math.cos(a)*rr,580+math.sin(a)*rr))
    d.polygon(star,fill=(255,220,100,255)); label(d,'♥',360,1040,font('segoeui.ttf',60),(255,91,116,255),'mm')
    player(im,760,1960); return im

def make_pause():
    im=make_game(); d=ImageDraw.Draw(im,'RGBA'); d.rectangle((0,0,W,H),fill=(2,5,15,165)); rounded(d,(100,760,980,1660),46,(10,22,46,235),(95,243,255,150),4); centered(d,'ПАУЗА',860,font('impact.ttf',100),(245,255,255,255)); centered(d,'Забег сохранён',990,font('segoeui.ttf',34),(170,200,215,255))
    for i,(txt,y,col) in enumerate([('ПРОДОЛЖИТЬ',1120,(95,243,255,240)),('ПРАВИЛА',1280,(24,44,70,240)),('В МЕНЮ',1440,(24,44,70,240))]):
        rounded(d,(180,y,900,y+104),52,col,(130,190,210,220),3); centered(d,txt,y+29,font('segoeuib.ttf',34),(5,22,38,255) if i==0 else (220,238,245,255))
    return im

def make_over():
    im=make_game(); d=ImageDraw.Draw(im,'RGBA'); d.rectangle((0,0,W,H),fill=(2,5,15,140)); rounded(d,(82,700,998,1700),48,(8,19,40,242),(95,243,255,170),4); centered(d,'ЗАБЕГ ОКОНЧЕН',790,font('impact.ttf',75),(255,236,251,255)); centered(d,'КЛАССИКА',900,font('segoeuib.ttf',30),(166,205,220,255)); centered(d,'СЧЁТ',1008,font('segoeuib.ttf',26),(140,175,190,255)); centered(d,'1 861',1080,font('impact.ttf',120),(245,255,255,255)); centered(d,'РЕКОРД  2 540',1235,font('segoeuib.ttf',34),(95,243,255,255));
    rows=[('Звёзды','18'),('Пролёты','74'),('Макс. комбо','x9'),('Время','01:26')]
    for i,(a,b) in enumerate(rows):
        y=1335+i*60; label(d,a,210,y,font('segoeui.ttf',28),(170,200,215,255)); label(d,b,870,y,font('segoeuib.ttf',28),(235,248,252,255),'ra')
    rounded(d,(170,1590,910,1690),50,(95,243,255,245),None); centered(d,'ЕЩЁ РАЗ',1614,font('segoeuib.ttf',34),(5,22,38,255)); return im

imgs=[('menu.png',base_menu()),('gameplay.png',make_game()),('pause.png',make_pause()),('game-over.png',make_over())]
for name,im in imgs:
    im.convert('RGB').save(os.path.join(OUT,name),optimize=True)
for src,dst in zip(['menu.png','gameplay.png','pause.png','game-over.png'],['01-menu.png','02-gameplay.png','03-pause.png','04-game-over.png']): shutil.copy2(os.path.join(OUT,src),os.path.join(STORE,dst))
print('generated',[(n,os.path.getsize(os.path.join(OUT,n))) for n,_ in imgs])
