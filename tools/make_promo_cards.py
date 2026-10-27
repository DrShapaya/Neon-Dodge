"""
Neon Dodge - RuStore Marketing Promotional Cards Generator (Clean Polish Edition)
Generates 4 ultra-high-quality, professional store cards (1080x1920)
featuring floating smartphone mockups, glowing neon typography,
3D pop-out vector game elements, and cosmic gradient backgrounds.
"""

import os
import math
import random
import shutil
from PIL import Image, ImageDraw, ImageFont, ImageFilter

W, H = 1080, 1920

# Authentic Game Palette
C_CYAN = (95, 243, 255)
C_MAGENTA = (255, 79, 216)
C_VIOLET = (164, 107, 255)
C_ORANGE = (255, 158, 79)
C_LIME = (182, 255, 92)
C_GOLD = (255, 215, 94)
C_RED = (255, 79, 106)
C_MINT = (124, 249, 208)
C_WHITE = (255, 255, 255)

FONT_BLACK_PATH = "app/src/main/res/font/unbounded_black.ttf"
FONT_BOLD_PATH = "app/src/main/res/font/unbounded_bold.ttf"

def load_fonts():
    return {
        "title_large": ImageFont.truetype(FONT_BLACK_PATH, 62),
        "sub": ImageFont.truetype(FONT_BOLD_PATH, 25),
        "pill": ImageFont.truetype(FONT_BOLD_PATH, 20),
        "tag": ImageFont.truetype(FONT_BOLD_PATH, 16),
    }

def create_gradient_bg(w, h, top_color, mid_color, bottom_color):
    """Creates a rich, smooth 3-stop vertical cosmic gradient."""
    img = Image.new("RGBA", (w, h))
    for y in range(h):
        if y < h * 0.45:
            t = y / (h * 0.45)
            r = int(top_color[0] * (1 - t) + mid_color[0] * t)
            g = int(top_color[1] * (1 - t) + mid_color[1] * t)
            b = int(top_color[2] * (1 - t) + mid_color[2] * t)
        else:
            t = (y - h * 0.45) / (h * 0.55)
            r = int(mid_color[0] * (1 - t) + bottom_color[0] * t)
            g = int(mid_color[1] * (1 - t) + bottom_color[1] * t)
            b = int(mid_color[2] * (1 - t) + bottom_color[2] * t)
        img.paste((r, g, b, 255), [0, y, w, y + 1])
    return img

def add_stars_and_nebula(bg, seed=42, nebula_blooms=None):
    """Draws multi-layered distant nebulae, cosmic dust and twinkling starfield."""
    w, h = bg.size
    random.seed(seed)
    
    # 1. Nebulae layer
    nebula = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ndraw = ImageDraw.Draw(nebula)
    if nebula_blooms:
        for cx, cy, rad, col in nebula_blooms:
            ndraw.ellipse([cx - rad, cy - rad, cx + rad, cy + rad], fill=col)
    nebula = nebula.filter(ImageFilter.GaussianBlur(150))
    bg = Image.alpha_composite(bg, nebula)
    
    # 2. Subtle sci-fi grid
    grid = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(grid)
    for x in range(0, w, 120):
        gdraw.line([(x, 0), (x, h)], fill=(255, 255, 255, 6), width=1)
    for y in range(0, h, 120):
        gdraw.line([(0, y), (w, y)], fill=(255, 255, 255, 6), width=1)
    bg = Image.alpha_composite(bg, grid)

    # 3. Stars
    stars = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    sdraw = ImageDraw.Draw(stars)
    for _ in range(190):
        x = random.randint(0, w - 1)
        y = random.randint(0, h - 1)
        size = random.choice([1, 1, 1, 2, 2, 3])
        alpha = random.randint(50, 230)
        c = random.choice([
            (255, 255, 255, alpha),
            (200, 240, 255, alpha),
            (255, 220, 240, alpha),
            (220, 255, 240, alpha),
            (255, 245, 180, alpha)
        ])
        sdraw.ellipse([x, y, x + size, y + size], fill=c)
        if size == 3 and random.random() < 0.45:
            flare_col = (255, 255, 255, alpha // 2)
            sdraw.line([x - 5, y + 1, x + 7, y + 1], fill=flare_col, width=1)
            sdraw.line([x + 1, y - 5, x + 1, y + 7], fill=flare_col, width=1)
            
    return Image.alpha_composite(bg, stars)

def draw_glowing_text(canvas, text, font, pos, fill_color, glow_color, glow_radius=22, center=False):
    """Draws crisp Russian typography with a multi-layered neon glow."""
    w, h = canvas.size
    t_layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    t_draw = ImageDraw.Draw(t_layer)
    
    bbox = t_draw.textbbox((0, 0), text, font=font)
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]
    
    if center:
        x = pos[0] - tw // 2
        y = pos[1] - th // 2
    else:
        x, y = pos
        
    t_draw.text((x, y), text, font=font, fill=(glow_color[0], glow_color[1], glow_color[2], 170))
    wide_glow = t_layer.filter(ImageFilter.GaussianBlur(glow_radius))
    tight_glow = t_layer.filter(ImageFilter.GaussianBlur(glow_radius // 3))
    
    canvas = Image.alpha_composite(canvas, wide_glow)
    canvas = Image.alpha_composite(canvas, tight_glow)
    
    fg = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    fg_draw = ImageDraw.Draw(fg)
    fg_draw.text((x + 1, y + 3), text, font=font, fill=(0, 0, 0, 160))
    fg_draw.text((x, y), text, font=font, fill=fill_color)
    
    canvas = Image.alpha_composite(canvas, fg)
    return canvas, (x, y, tw, th)

def draw_pill_badge(canvas, text, font, center_x, center_y, border_color, fill_color, text_color, icon=""):
    """Draws a modern high-tech rounded pill badge."""
    w, h = canvas.size
    d = ImageDraw.Draw(canvas)
    full_text = f"{icon}  {text}".strip() if icon else text
    bbox = d.textbbox((0, 0), full_text, font=font)
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]
    
    pad_x = 28
    pad_y = 14
    bw = tw + pad_x * 2
    bh = th + pad_y * 2
    bx1 = center_x - bw // 2
    by1 = center_y - bh // 2
    bx2 = bx1 + bw
    by2 = by1 + bh
    
    glow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.rounded_rectangle([bx1, by1, bx2, by2], radius=bh // 2, outline=(border_color[0], border_color[1], border_color[2], 180), width=4)
    glow = glow.filter(ImageFilter.GaussianBlur(10))
    canvas = Image.alpha_composite(canvas, glow)
    
    body = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    bd = ImageDraw.Draw(body)
    bd.rounded_rectangle([bx1, by1, bx2, by2], radius=bh // 2, fill=fill_color, outline=border_color, width=2)
    bd.text((center_x - tw // 2, center_y - th // 2 - 2), full_text, font=font, fill=text_color)
    return Image.alpha_composite(canvas, body)

def draw_phone_mockup(canvas, screenshot_path, px, py, pw, ph, accent_color=C_CYAN):
    """Renders a sleek modern smartphone floating mockup with realistic bezel and shadow."""
    w, h = canvas.size
    
    # 1. Soft 3D drop shadow
    shadow_layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow_layer)
    sd.rounded_rectangle([px + 10, py + 30, px + pw - 10, py + ph + 42], radius=50, fill=(0, 0, 0, 180))
    sd.rounded_rectangle([px + 30, py + 40, px + pw - 30, py + ph + 30], radius=50, 
                         fill=(accent_color[0], accent_color[1], accent_color[2], 55))
    shadow_layer = shadow_layer.filter(ImageFilter.GaussianBlur(40))
    canvas = Image.alpha_composite(canvas, shadow_layer)
    
    # 2. Phone body
    phone_layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    pd = ImageDraw.Draw(phone_layer)
    
    # Outer bezel body
    pd.rounded_rectangle([px, py, px + pw, py + ph], radius=48, fill=(12, 16, 28, 255), 
                         outline=(48, 62, 92, 255), width=3)
    pd.rounded_rectangle([px + 3, py + 3, px + pw - 3, py + ph - 3], radius=46, 
                         outline=(22, 28, 44, 255), width=2)
    
    # Screen coordinates
    bezel_x = 18
    bezel_top = 22
    bezel_bottom = 22
    sx = px + bezel_x
    sy = py + bezel_top
    sw = pw - bezel_x * 2
    sh = ph - (bezel_top + bezel_bottom)
    
    # 3. Insert screenshot
    if os.path.exists(screenshot_path):
        sc_img = Image.open(screenshot_path).convert("RGBA")
        sc_resized = sc_img.resize((sw, sh), Image.Resampling.LANCZOS)
        
        sheen = Image.new("RGBA", (sw, sh), (0, 0, 0, 0))
        shd = ImageDraw.Draw(sheen)
        sheen_poly = [
            (0, 0),
            (int(sw * 0.85), 0),
            (0, int(sh * 0.50))
        ]
        shd.polygon(sheen_poly, fill=(255, 255, 255, 20))
        
        screen_combined = Image.alpha_composite(sc_resized, sheen)
        
        mask = Image.new("L", (sw, sh), 0)
        md = ImageDraw.Draw(mask)
        md.rounded_rectangle([0, 0, sw, sh], radius=32, fill=255)
        
        phone_layer.paste(screen_combined, (sx, sy), mask)
    
    # 4. Inner screen border
    pd.rounded_rectangle([sx, sy, sx + sw, sy + sh], radius=32, outline=(60, 80, 120, 140), width=2)
    
    # 5. Hardware details: Camera punch hole & speaker
    cx = px + pw // 2
    pd.ellipse([cx - 7, sy + 14, cx + 7, sy + 28], fill=(6, 8, 14, 255), outline=(32, 42, 62, 255), width=1)
    pd.ellipse([cx - 2, sy + 18, cx + 2, sy + 22], fill=(accent_color[0], accent_color[1], accent_color[2], 120))
    pd.rounded_rectangle([cx - 30, py + 8, cx + 30, py + 12], radius=2, fill=(26, 34, 48, 255))
    
    canvas = Image.alpha_composite(canvas, phone_layer)
    return canvas, (sx, sy, sw, sh)

# ----------------- VECTOR GAME GRAPHICS (POP-OUT 3D ELEMENTS) -----------------

def draw_neon_orb(canvas, cx, cy, radius, primary_color=C_CYAN, trail=True, trail_angle=1.2):
    """Draws the glowing player orb with bright aura and curved particle tail."""
    w, h = canvas.size
    
    if trail:
        trail_layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        td = ImageDraw.Draw(trail_layer)
        for i in range(16):
            t = (i + 1) / 16.0
            tx = cx - math.cos(trail_angle) * t * 160 - math.sin(t * 3.0) * 25
            ty = cy + math.sin(trail_angle) * t * 240 + math.cos(t * 2.0) * 20
            prad = radius * (1 - t * 0.75) * 0.45
            alpha = int(190 * (1 - t * 0.85))
            td.ellipse([tx - prad, ty - prad, tx + prad, ty + prad], 
                       fill=(primary_color[0], primary_color[1], primary_color[2], alpha))
        trail_layer = trail_layer.filter(ImageFilter.GaussianBlur(10))
        canvas = Image.alpha_composite(canvas, trail_layer)
    
    corona = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    cd = ImageDraw.Draw(corona)
    cd.ellipse([cx - radius * 2.2, cy - radius * 2.2, cx + radius * 2.2, cy + radius * 2.2], 
               fill=(primary_color[0], primary_color[1], primary_color[2], 130))
    corona = corona.filter(ImageFilter.GaussianBlur(int(radius * 0.8)))
    canvas = Image.alpha_composite(canvas, corona)
    
    orb = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    od = ImageDraw.Draw(orb)
    od.ellipse([cx - radius * 1.35, cy - radius * 1.35, cx + radius * 1.35, cy + radius * 1.35], 
               outline=(255, 255, 255, 190), width=3)
    od.ellipse([cx - radius, cy - radius, cx + radius, cy + radius], 
               fill=(primary_color[0], primary_color[1], primary_color[2], 255),
               outline=(255, 255, 255, 240), width=2)
    
    c_rad = radius * 0.42
    od.ellipse([cx - c_rad, cy - c_rad, cx + c_rad, cy + c_rad], fill=(255, 255, 255, 255))
    
    return Image.alpha_composite(canvas, orb)

def draw_neon_asteroid(canvas, cx, cy, radius, color, shape='hex', rot=0, armor=False, spiked=False):
    """Draws a neon asteroid polygon with vivid glow and high-tech detailing."""
    w, h = canvas.size
    
    pts = []
    if shape == 'hex':
        for i in range(6):
            a = rot + i * math.pi / 3
            pts.append((cx + radius * math.cos(a), cy + radius * math.sin(a)))
    elif shape == 'diamond':
        rx, ry = radius * 0.75, radius * 1.25
        base_pts = [(0, -ry), (rx, 0), (0, ry), (-rx, 0)]
        cos_a, sin_a = math.cos(rot), math.sin(rot)
        pts = [(cx + px * cos_a - py * sin_a, cy + px * sin_a + py * cos_a) for px, py in base_pts]
    elif shape == 'boss':
        for i in range(12):
            a = rot + i * math.pi / 6
            r = radius if i % 2 == 0 else radius * 0.72
            pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))

    glow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.polygon(pts, fill=(color[0], color[1], color[2], 150))
    glow = glow.filter(ImageFilter.GaussianBlur(int(radius * 0.5)))
    canvas = Image.alpha_composite(canvas, glow)
    
    ast = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ad = ImageDraw.Draw(ast)
    ad.polygon(pts, fill=(color[0] // 4, color[1] // 4, color[2] // 4, 220), 
               outline=(color[0], color[1], color[2], 255), width=3)
    
    if armor:
        inner_pts = []
        for px, py in pts:
            inner_pts.append((cx + (px - cx) * 0.75, cy + (py - cy) * 0.75))
        ad.polygon(inner_pts, outline=(color[0], color[1], color[2], 200), width=2)
    
    ad.ellipse([cx - radius * 0.22, cy - radius * 0.22, cx + radius * 0.22, cy + radius * 0.22], 
               fill=(255, 255, 255, 220))
    
    return Image.alpha_composite(canvas, ast)

def draw_neon_star(canvas, cx, cy, r_outer, color=C_GOLD, rot=0):
    """Draws a bright 4-point collectible neon star."""
    w, h = canvas.size
    r_inner = r_outer * 0.28
    pts = []
    for i in range(8):
        a = rot + i * math.pi / 4
        r = r_outer if i % 2 == 0 else r_inner
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
        
    glow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.polygon(pts, fill=(color[0], color[1], color[2], 190))
    glow = glow.filter(ImageFilter.GaussianBlur(int(r_outer * 0.6)))
    canvas = Image.alpha_composite(canvas, glow)
    
    star = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    sd = ImageDraw.Draw(star)
    sd.polygon(pts, fill=(color[0], color[1], color[2], 240), outline=(255, 255, 255, 255), width=2)
    sd.ellipse([cx - r_inner * 0.8, cy - r_inner * 0.8, cx + r_inner * 0.8, cy + r_inner * 0.8], fill=(255, 255, 255, 255))
    return Image.alpha_composite(canvas, star)

def draw_neon_shield(canvas, cx, cy, radius, color=C_CYAN):
    """Draws a glowing shield powerup bubble."""
    w, h = canvas.size
    glow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse([cx - radius * 1.5, cy - radius * 1.5, cx + radius * 1.5, cy + radius * 1.5], 
               fill=(color[0], color[1], color[2], 120))
    glow = glow.filter(ImageFilter.GaussianBlur(int(radius * 0.6)))
    canvas = Image.alpha_composite(canvas, glow)
    
    layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    d.ellipse([cx - radius, cy - radius, cx + radius, cy + radius], 
              fill=(color[0] // 4, color[1] // 4, color[2] // 4, 180), outline=color, width=3)
    d.ellipse([cx - radius * 0.65, cy - radius * 0.65, cx + radius * 0.65, cy + radius * 0.65], 
              outline=(255, 255, 255, 220), width=2)
    return Image.alpha_composite(canvas, layer)

def draw_celebration_rays(canvas, cx, cy, num_rays=20, color=C_GOLD):
    """Draws subtle golden radial victory beams behind the phone."""
    w, h = canvas.size
    layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    for i in range(num_rays):
        a1 = i * 2 * math.pi / num_rays
        a2 = a1 + math.pi / num_rays * 0.42
        r = 1100
        p1 = (cx, cy)
        p2 = (cx + r * math.cos(a1), cy + r * math.sin(a1))
        p3 = (cx + r * math.cos(a2), cy + r * math.sin(a2))
        d.polygon([p1, p2, p3], fill=(color[0], color[1], color[2], 26))
    layer = layer.filter(ImageFilter.GaussianBlur(22))
    return Image.alpha_composite(canvas, layer)

# ----------------- INDIVIDUAL CARD GENERATORS -----------------

def make_card_1(fonts):
    """Card 1: Core Gameplay, Controls, Responsiveness."""
    print("Generating Card 1: Gameplay & Controls...")
    bg = create_gradient_bg(W, H, (6, 10, 24), (10, 20, 48), (6, 42, 70))
    bg = add_stars_and_nebula(bg, seed=101, nebula_blooms=[
        (200, 250, 260, (30, 90, 180, 80)),
        (880, 750, 320, (0, 190, 230, 65)),
        (540, 1550, 340, (20, 120, 200, 90))
    ])
    
    # Header
    bg = draw_pill_badge(bg, "АРКАДА НОВОГО ПОКОЛЕНИЯ", fonts["tag"], 540, 88, 
                         border_color=C_CYAN, fill_color=(12, 28, 54, 230), text_color=(190, 248, 255), icon="✦")
    
    bg, _ = draw_glowing_text(bg, "НЕОНОВЫЙ КОСМОС", fonts["title_large"], (540, 155), 
                              fill_color=C_WHITE, glow_color=C_CYAN, glow_radius=24, center=True)
    bg, _ = draw_glowing_text(bg, "Плавное управление пальцем и динамичный экшен", fonts["sub"], (540, 222), 
                              fill_color=(200, 240, 255), glow_color=(30, 140, 220), glow_radius=12, center=True)
    
    # Phone mockup
    bg, sbox = draw_phone_mockup(bg, "screenshots/gameplay.png", 160, 290, 760, 1370, accent_color=C_CYAN)
    
    # 3D Pop-out elements breaking the bezel
    # 1. Player orb bursting near bottom-right
    bg = draw_neon_orb(bg, 840, 1540, radius=54, primary_color=C_CYAN, trail=True, trail_angle=1.2)
    
    # 2. Golden 4-point star on top-left edge
    bg = draw_neon_star(bg, 175, 480, r_outer=36, color=C_GOLD, rot=0.2)
    
    # 3. Floating purple diamond asteroid on the left
    bg = draw_neon_asteroid(bg, 130, 1020, radius=42, color=C_VIOLET, shape='diamond', rot=-0.3)
    
    # 4. Floating green bouncing asteroid on the right edge
    bg = draw_neon_asteroid(bg, 935, 920, radius=38, color=C_LIME, shape='hex', rot=0.4, armor=True)
    
    # Bottom badges
    bg = draw_pill_badge(bg, "120 FPS ОТКЛИК", fonts["pill"], 330, 1750, 
                         border_color=C_CYAN, fill_color=(10, 26, 50, 230), text_color=(220, 250, 255), icon="⚡")
    bg = draw_pill_badge(bg, "УПРАВЛЕНИЕ В 1 КАСАНИЕ", fonts["pill"], 730, 1750, 
                         border_color=C_CYAN, fill_color=(10, 26, 50, 230), text_color=(220, 250, 255), icon="🎯")
    
    return bg

def make_card_2(fonts):
    """Card 2: 9 Asteroid Types & Epic Mini-Bosses."""
    print("Generating Card 2: 9 Asteroid Types & Bosses...")
    bg = create_gradient_bg(W, H, (14, 6, 20), (32, 10, 36), (22, 8, 48))
    bg = add_stars_and_nebula(bg, seed=202, nebula_blooms=[
        (250, 220, 280, (180, 30, 120, 85)),
        (850, 680, 320, (230, 40, 80, 75)),
        (540, 1500, 360, (140, 20, 160, 90))
    ])
    
    # Header
    bg = draw_pill_badge(bg, "ЭПИЧНЫЕ ВЫЗОВЫ", fonts["tag"], 540, 88, 
                         border_color=C_MAGENTA, fill_color=(46, 14, 38, 230), text_color=(255, 200, 235), icon="⚠")
    
    bg, _ = draw_glowing_text(bg, "9 ТИПОВ ВРАГОВ", fonts["title_large"], (540, 155), 
                              fill_color=C_WHITE, glow_color=C_RED, glow_radius=24, center=True)
    bg, _ = draw_glowing_text(bg, "Уникальные повадки астероидов и опасные боссы", fonts["sub"], (540, 222), 
                              fill_color=(255, 210, 225), glow_color=(200, 40, 90), glow_radius=12, center=True)
    
    # Clean phone mockup
    bg, sbox = draw_phone_mockup(bg, "screenshots/gameplay.png", 160, 290, 760, 1370, accent_color=C_RED)
    
    # 3D Pop-out elements breaking the bezel
    # 1. Massive Mini-boss looming over top-right
    bg = draw_neon_asteroid(bg, 900, 420, radius=72, color=C_RED, shape='boss', rot=0.25)
    
    # 2. Armored lime bouncing asteroid on bottom-left
    bg = draw_neon_asteroid(bg, 135, 1220, radius=48, color=C_LIME, shape='hex', rot=-0.2, armor=True)
    
    # 3. Bursting orange speed asteroid on bottom-right
    bg = draw_neon_asteroid(bg, 935, 1320, radius=42, color=C_ORANGE, shape='diamond', rot=0.5)
    
    # 4. Purple seeker asteroid on mid-left
    bg = draw_neon_asteroid(bg, 120, 720, radius=38, color=C_MAGENTA, shape='hex', rot=0.1)
    
    # 5. Floating Shield powerup on mid-right
    bg = draw_neon_shield(bg, 940, 880, radius=34, color=C_CYAN)
    
    # Bottom badges
    bg = draw_pill_badge(bg, "ОПАСНЫЕ БОССЫ", fonts["pill"], 320, 1750, 
                         border_color=C_RED, fill_color=(40, 12, 24, 230), text_color=(255, 220, 230), icon="👾")
    bg = draw_pill_badge(bg, "ЩИТ, БОМБА И ФАЗА", fonts["pill"], 730, 1750, 
                         border_color=C_MAGENTA, fill_color=(36, 14, 42, 230), text_color=(255, 220, 250), icon="🛡️")
    
    return bg

def make_card_3(fonts):
    """Card 3: 3 Modes (Classic, Hardcore x5, Zen) & Upgrades."""
    print("Generating Card 3: 3 Modes & Progression...")
    bg = create_gradient_bg(W, H, (10, 6, 26), (22, 12, 50), (8, 34, 46))
    bg = add_stars_and_nebula(bg, seed=303, nebula_blooms=[
        (220, 260, 280, (140, 40, 200, 85)),
        (860, 720, 300, (40, 180, 160, 75)),
        (540, 1480, 350, (100, 30, 180, 85))
    ])
    
    # Header
    bg = draw_pill_badge(bg, "ВЫБЕРИ СВОЙ СТИЛЬ", fonts["tag"], 540, 88, 
                         border_color=C_VIOLET, fill_color=(32, 16, 54, 230), text_color=(235, 210, 255), icon="★")
    
    bg, _ = draw_glowing_text(bg, "3 РЕЖИМА ИГРЫ", fonts["title_large"], (540, 155), 
                              fill_color=C_WHITE, glow_color=C_VIOLET, glow_radius=24, center=True)
    bg, _ = draw_glowing_text(bg, "Классика, Хардкор x5 и Дзен без проигрыша", fonts["sub"], (540, 222), 
                              fill_color=(230, 220, 255), glow_color=(120, 50, 200), glow_radius=12, center=True)
    
    # Phone mockup displaying menu screen cleanly
    bg, sbox = draw_phone_mockup(bg, "screenshots/menu.png", 160, 290, 760, 1370, accent_color=C_VIOLET)
    
    # 3D Pop-out elements placed cleanly without text clutter
    # 1. Unlocked Golden Sun skin floating on left side
    bg = draw_neon_orb(bg, 115, 820, radius=46, primary_color=C_GOLD, trail=True, trail_angle=2.2)
    
    # 2. Violet Nova skin floating on right side
    bg = draw_neon_orb(bg, 965, 860, radius=42, primary_color=C_MAGENTA, trail=True, trail_angle=-0.8)
    
    # 3. Emerald Zen star on top-left
    bg = draw_neon_star(bg, 150, 420, r_outer=34, color=C_MINT, rot=0.1)
    
    # 4. Cyan cosmic diamond on bottom-right
    bg = draw_neon_asteroid(bg, 935, 1360, radius=36, color=C_CYAN, shape='diamond', rot=0.3)
    
    # Bottom badges
    bg = draw_pill_badge(bg, "ИСПЫТАНИЕ ДНЯ", fonts["pill"], 320, 1750, 
                         border_color=C_MINT, fill_color=(10, 36, 32, 230), text_color=(200, 255, 240), icon="📅")
    bg = draw_pill_badge(bg, "15 НЕОНОВЫХ СКИНОВ", fonts["pill"], 730, 1750, 
                         border_color=C_VIOLET, fill_color=(28, 14, 48, 230), text_color=(235, 215, 255), icon="🎨")
    
    return bg

def make_card_4(fonts):
    """Card 4: High Scores, Offline, 100% Free, Zero Ads."""
    print("Generating Card 4: High Scores & Fair Play...")
    bg = create_gradient_bg(W, H, (10, 8, 22), (28, 20, 12), (8, 22, 42))
    bg = add_stars_and_nebula(bg, seed=404, nebula_blooms=[
        (220, 240, 280, (200, 140, 30, 80)),
        (860, 660, 300, (40, 120, 220, 75)),
        (540, 1480, 350, (220, 160, 20, 85))
    ])
    
    # Golden celebration rays behind the phone
    bg = draw_celebration_rays(bg, 540, 950, num_rays=20, color=C_GOLD)
    
    # Header
    bg = draw_pill_badge(bg, "ЧЕСТНАЯ АРКАДА", fonts["tag"], 540, 88, 
                         border_color=C_GOLD, fill_color=(42, 32, 12, 230), text_color=(255, 235, 170), icon="★")
    
    bg, _ = draw_glowing_text(bg, "БЕЙ РЕКОРДЫ", fonts["title_large"], (540, 155), 
                              fill_color=C_WHITE, glow_color=C_GOLD, glow_radius=24, center=True)
    bg, _ = draw_glowing_text(bg, "100% офлайн • Без рекламы, доната и интернета", fonts["sub"], (540, 222), 
                              fill_color=(255, 240, 200), glow_color=(180, 130, 30), glow_radius=12, center=True)
    
    # Phone mockup displaying game-over screen cleanly
    bg, sbox = draw_phone_mockup(bg, "screenshots/game-over.png", 160, 290, 760, 1370, accent_color=C_GOLD)
    
    # 3D Pop-out elements breaking the bezel
    # 1. Golden celebration stars showering around
    bg = draw_neon_star(bg, 940, 440, r_outer=42, color=C_GOLD, rot=0.1)
    bg = draw_neon_star(bg, 120, 440, r_outer=34, color=C_GOLD, rot=-0.3)
    bg = draw_neon_star(bg, 115, 1180, r_outer=32, color=C_GOLD, rot=0.2)
    bg = draw_neon_star(bg, 950, 1180, r_outer=36, color=C_CYAN, rot=0.25)
    
    # 2. Golden collector gem on left
    bg = draw_neon_asteroid(bg, 110, 800, radius=38, color=C_GOLD, shape='diamond', rot=0.15)
    
    # 3. Cyan player orb ascending near bottom-right edge
    bg = draw_neon_orb(bg, 910, 1520, radius=48, primary_color=C_CYAN, trail=True, trail_angle=1.4)
    
    # Bottom badges
    bg = draw_pill_badge(bg, "0% РЕКЛАМЫ И ДОНАТА", fonts["pill"], 320, 1750, 
                         border_color=C_GOLD, fill_color=(38, 28, 10, 230), text_color=(255, 235, 170), icon="🚫")
    bg = draw_pill_badge(bg, "ИГРАЙ БЕЗ ИНТЕРНЕТА", fonts["pill"], 730, 1750, 
                         border_color=C_CYAN, fill_color=(10, 28, 48, 230), text_color=(200, 245, 255), icon="✈️")
    
    return bg

# ----------------- MAIN PIPELINE -----------------

def main():
    print("Initializing fonts...")
    fonts = load_fonts()
    
    cards_dir = "store/cards"
    store_sc_dir = "store/screenshots"
    raw_sc_dir = "store/screenshots-raw"
    
    os.makedirs(cards_dir, exist_ok=True)
    os.makedirs(raw_sc_dir, exist_ok=True)
    os.makedirs(store_sc_dir, exist_ok=True)
    
    # Back up raw screenshots if not yet backed up
    for fname in ["01-menu.png", "02-gameplay.png", "03-pause.png", "04-game-over.png"]:
        src = os.path.join(store_sc_dir, fname)
        dst = os.path.join(raw_sc_dir, fname)
        if os.path.exists(src) and not os.path.exists(dst):
            shutil.copy2(src, dst)
            print(f"Backed up raw {fname} to {raw_sc_dir}")

    # Generate all 4 cards
    cards = [
        ("01_gameplay.png", "01-gameplay.png", make_card_1(fonts)),
        ("02_enemies.png", "02-enemies.png", make_card_2(fonts)),
        ("03_modes.png", "03-modes.png", make_card_3(fonts)),
        ("04_records.png", "04-records.png", make_card_4(fonts)),
    ]
    
    for card_filename, store_filename, card_img in cards:
        # Save to store/cards/ as RGB PNG
        rgb_img = card_img.convert("RGB")
        out_path1 = os.path.join(cards_dir, card_filename)
        rgb_img.save(out_path1, quality=95)
        print(f"Saved: {out_path1}")
        
        # Also save to store/screenshots/ for direct RuStore upload
        out_path2 = os.path.join(store_sc_dir, store_filename)
        rgb_img.save(out_path2, quality=95)
        print(f"Saved: {out_path2}")
        
    print("\nAll 4 RuStore marketing cards generated successfully!")

if __name__ == "__main__":
    main()
