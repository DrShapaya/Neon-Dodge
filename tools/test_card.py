import os
import math
import random
from PIL import Image, ImageDraw, ImageFont, ImageFilter

def create_gradient_bg(w, h, top_color, mid_color, bottom_color):
    """Creates a smooth 3-stop vertical gradient background."""
    img = Image.new("RGBA", (w, h))
    for y in range(h):
        if y < h * 0.5:
            t = y / (h * 0.5)
            r = int(top_color[0] * (1 - t) + mid_color[0] * t)
            g = int(top_color[1] * (1 - t) + mid_color[1] * t)
            b = int(top_color[2] * (1 - t) + mid_color[2] * t)
        else:
            t = (y - h * 0.5) / (h * 0.5)
            r = int(mid_color[0] * (1 - t) + bottom_color[0] * t)
            g = int(mid_color[1] * (1 - t) + bottom_color[1] * t)
            b = int(mid_color[2] * (1 - t) + bottom_color[2] * t)
        img.paste((r, g, b, 255), [0, y, w, y + 1])
    return img

def add_stars_and_nebula(bg, seed=42, nebula_centers=None):
    w, h = bg.size
    random.seed(seed)
    
    # 1. Nebula glow layer
    nebula = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ndraw = ImageDraw.Draw(nebula)
    if nebula_centers:
        for cx, cy, rad, col in nebula_centers:
            ndraw.ellipse([cx - rad, cy - rad, cx + rad, cy + rad], fill=col)
    nebula = nebula.filter(ImageFilter.GaussianBlur(140))
    bg = Image.alpha_composite(bg, nebula)
    
    # 2. Stars
    stars = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    sdraw = ImageDraw.Draw(stars)
    for _ in range(160):
        x = random.randint(0, w - 1)
        y = random.randint(0, h - 1)
        size = random.choice([1, 1, 1, 2, 2, 3])
        alpha = random.randint(40, 220)
        c = random.choice([
            (255, 255, 255, alpha),
            (200, 240, 255, alpha),
            (255, 220, 240, alpha),
            (220, 255, 240, alpha)
        ])
        sdraw.ellipse([x, y, x + size, y + size], fill=c)
        if size == 3 and random.random() < 0.35:
            # tiny 4-point cross flare
            sdraw.line([x - 5, y + 1, x + 7, y + 1], fill=(255, 255, 255, alpha // 2), width=1)
            sdraw.line([x + 1, y - 5, x + 1, y + 7], fill=(255, 255, 255, alpha // 2), width=1)
    
    bg = Image.alpha_composite(bg, stars)
    return bg

def draw_glowing_text(canvas, text, font, pos, fill_color, glow_color, glow_radius=18, center=False):
    """Draws crisp text with a rich neon aura."""
    w, h = canvas.size
    # Temporary layer for text glow
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
        
    # Wide outer glow
    t_draw.text((x, y), text, font=font, fill=(glow_color[0], glow_color[1], glow_color[2], 160))
    wide_glow = t_layer.filter(ImageFilter.GaussianBlur(glow_radius))
    
    # Intense inner glow
    tight_glow = t_layer.filter(ImageFilter.GaussianBlur(glow_radius // 3))
    
    # Composite glows
    canvas = Image.alpha_composite(canvas, wide_glow)
    canvas = Image.alpha_composite(canvas, tight_glow)
    
    # Sharp foreground text with drop shadow
    fg = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    fg_draw = ImageDraw.Draw(fg)
    # subtle dark edge for contrast
    fg_draw.text((x + 1, y + 2), text, font=font, fill=(0, 0, 0, 140))
    fg_draw.text((x, y), text, font=font, fill=fill_color)
    
    canvas = Image.alpha_composite(canvas, fg)
    return canvas, (x, y, tw, th)

def draw_pill_badge(canvas, text, font, center_x, center_y, border_color, fill_color, text_color, icon=""):
    w, h = canvas.size
    d = ImageDraw.Draw(canvas)
    full_text = f"{icon} {text}".strip() if icon else text
    bbox = d.textbbox((0, 0), full_text, font=font)
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]
    
    pad_x = 24
    pad_y = 12
    bw = tw + pad_x * 2
    bh = th + pad_y * 2
    bx1 = center_x - bw // 2
    by1 = center_y - bh // 2
    bx2 = bx1 + bw
    by2 = by1 + bh
    
    # Glow around badge
    glow_layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow_layer)
    gd.rounded_rectangle([bx1, by1, bx2, by2], radius=bh // 2, outline=(border_color[0], border_color[1], border_color[2], 180), width=4)
    glow_layer = glow_layer.filter(ImageFilter.GaussianBlur(10))
    canvas = Image.alpha_composite(canvas, glow_layer)
    
    # Badge body
    badge_layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    bd = ImageDraw.Draw(badge_layer)
    bd.rounded_rectangle([bx1, by1, bx2, by2], radius=bh // 2, fill=fill_color, outline=border_color, width=2)
    bd.text((center_x - tw // 2, center_y - th // 2 - 2), full_text, font=font, fill=text_color)
    canvas = Image.alpha_composite(canvas, badge_layer)
    return canvas

def draw_phone_mockup(canvas, screenshot_path, px, py, pw, ph, accent_color=(95, 243, 255)):
    """Renders a sleek modern smartphone floating mockup with realistic bezel and shadow."""
    w, h = canvas.size
    
    # 1. Soft 3D drop shadow
    shadow_layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow_layer)
    sd.rounded_rectangle([px + 10, py + 30, px + pw - 10, py + ph + 40], radius=50, fill=(0, 0, 0, 160))
    # Ambient colored glow under phone
    sd.rounded_rectangle([px + 30, py + 50, px + pw - 30, py + ph + 30], radius=50, fill=(accent_color[0], accent_color[1], accent_color[2], 45))
    shadow_layer = shadow_layer.filter(ImageFilter.GaussianBlur(38))
    canvas = Image.alpha_composite(canvas, shadow_layer)
    
    # 2. Phone body
    phone_layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    pd = ImageDraw.Draw(phone_layer)
    
    # Outer bezel body (matte dark blue-gray with metallic gradient)
    pd.rounded_rectangle([px, py, px + pw, py + ph], radius=48, fill=(13, 18, 30, 255), outline=(42, 54, 82, 255), width=3)
    
    # Subtle inner bevel line
    pd.rounded_rectangle([px + 3, py + 3, px + pw - 3, py + ph - 3], radius=46, outline=(24, 32, 50, 255), width=2)
    
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
        
        # Subtle diagonal sheen
        sheen = Image.new("RGBA", (sw, sh), (0, 0, 0, 0))
        shd = ImageDraw.Draw(sheen)
        sheen_poly = [
            (0, 0),
            (int(sw * 0.85), 0),
            (0, int(sh * 0.50))
        ]
        shd.polygon(sheen_poly, fill=(255, 255, 255, 22))
        
        # Merge screen and sheen
        screen_combined = Image.alpha_composite(sc_resized, sheen)
        
        # Screen mask with rounded corners
        mask = Image.new("L", (sw, sh), 0)
        md = ImageDraw.Draw(mask)
        md.rounded_rectangle([0, 0, sw, sh], radius=32, fill=255)
        
        # Composite screen into phone_layer
        phone_layer.paste(screen_combined, (sx, sy), mask)
    
    # 4. Inner screen border
    pd.rounded_rectangle([sx, sy, sx + sw, sy + sh], radius=32, outline=(50, 68, 105, 140), width=2)
    
    # 5. Hardware details: Camera punch hole & speaker
    cx = px + pw // 2
    # Punch hole
    pd.ellipse([cx - 7, sy + 14, cx + 7, sy + 28], fill=(6, 8, 14, 255), outline=(28, 36, 54, 255), width=1)
    pd.ellipse([cx - 2, sy + 18, cx + 2, sy + 22], fill=(accent_color[0], accent_color[1], accent_color[2], 120))
    # Top speaker slit
    pd.rounded_rectangle([cx - 30, py + 8, cx + 30, py + 12], radius=2, fill=(25, 32, 46, 255))
    
    canvas = Image.alpha_composite(canvas, phone_layer)
    return canvas, (sx, sy, sw, sh)

# Test run
bg = create_gradient_bg(1080, 1920, (6, 10, 24), (10, 18, 44), (7, 40, 68))
bg = add_stars_and_nebula(bg, seed=1, nebula_centers=[
    (200, 300, 250, (30, 80, 160, 90)),
    (800, 800, 300, (0, 180, 220, 60)),
    (540, 1500, 320, (20, 100, 180, 80))
])

f_title = ImageFont.truetype('app/src/main/res/font/unbounded_black.ttf', 56)
f_sub = ImageFont.truetype('app/src/main/res/font/unbounded_bold.ttf', 24)
f_pill = ImageFont.truetype('app/src/main/res/font/unbounded_bold.ttf', 18)

# Top badge
bg = draw_pill_badge(bg, "АРКАДА НОВОГО ПОКОЛЕНИЯ", f_pill, 540, 90, 
                     border_color=(95, 243, 255), fill_color=(12, 28, 52, 220), text_color=(180, 248, 255), icon="✦")

# Title & subtitle
bg, _ = draw_glowing_text(bg, "НЕОНОВЫЙ КОСМОС", f_title, (540, 160), 
                          fill_color=(255, 255, 255), glow_color=(95, 243, 255), glow_radius=22, center=True)
bg, _ = draw_glowing_text(bg, "Управление в одно касание и чистый драйв", f_sub, (540, 225), 
                          fill_color=(190, 235, 255), glow_color=(30, 140, 220), glow_radius=10, center=True)

# Phone mockup
bg, sbox = draw_phone_mockup(bg, "screenshots/gameplay.png", 160, 300, 760, 1380, accent_color=(95, 243, 255))

# Bottom badges
bg = draw_pill_badge(bg, "120 FPS ОТКЛИК", f_pill, 340, 1750, 
                     border_color=(95, 243, 255), fill_color=(10, 25, 48, 220), text_color=(220, 250, 255), icon="⚡")
bg = draw_pill_badge(bg, "100% ОФЛАЙН", f_pill, 740, 1750, 
                     border_color=(182, 255, 92), fill_color=(12, 35, 28, 220), text_color=(220, 255, 200), icon="✦")

bg.convert("RGB").save("tools/test_card_out.png", quality=95)
print("Saved tools/test_card_out.png successfully!")
