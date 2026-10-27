"""Item models, part C: frozen, household, cards and gifts, shopping bag."""
import math, random, bmesh
from studio import *
from items_b import open_box, shaped


def frozen_peas():
    def f(x, y, z):
        k = (1 - abs(x) ** 6) * (1 - abs(z) ** 6)
        return x, y * (0.15 + 0.85 * k), z
    o = shaped('bag', 1.0, 0.32, 1.25, 10, f); o.location = (0, 0, 1.25)
    displace(o, 0.03, 0.3)
    assign(o, mat('bagp', (0.12, 0.52, 0.18), rough=0.18, coat=0.6, bump=0.08, bump_scale=6))
    def art(im, d):
        W, H = im.size
        random.seed(3)
        for k in range(46):
            x = random.uniform(W * 0.12, W * 0.88); y = random.uniform(H * 0.45, H * 0.78); r = random.uniform(26, 38)
            d.ellipse([x - r, y - r, x + r, y + r], fill=(110, 185, 55)); d.ellipse([x - r * 0.45, y - r * 0.6, x - r * 0.05, y - r * 0.2], fill=(170, 225, 110))
    lab = text_label('peas.png', (800, 1000), (0, 0, 0, 0), [('FROZEN', 110, 0.12, (255, 255, 255)), ('PEAS', 190, 0.28, (255, 255, 255)), ('1 kg', 80, 0.9, (255, 255, 255))], art=art)
    d = decal('peaslab', lab, 1.5, 1.88, (0, -0.33, 1.25))
    root = bpy.data.objects.new('root', None); link(root); o.parent = root; d.parent = root
    root.rotation_euler = (math.radians(-6), 0, math.radians(-10))
    return [o]


def ice_cream():
    tub = lathe('tub', [(0.0, 0), (0.78, 0), (0.82, 0.04), (0.98, 1.05), (1.0, 1.08)], segs=96, cap_top=True)
    assign(tub, mat('tubm', (0.97, 0.96, 0.93), rough=0.35, sss=0.1))
    lid = lathe('lid', [(0.0, 1.06), (1.04, 1.06), (1.06, 1.1), (1.06, 1.22), (1.0, 1.25), (0.0, 1.27)], segs=96)
    assign(lid, mat('lidm', (0.86, 0.36, 0.44), rough=0.3))
    def art(im, d):
        W, H = im.size
        for (cx, col) in [(W * 0.3, (250, 240, 215)), (W * 0.5, (120, 70, 45)), (W * 0.7, (240, 160, 180))]:
            d.ellipse([cx - 120, H * 0.52 - 100, cx + 120, H * 0.52 + 100], fill=col)
    lab = text_label('icecream.png', (1500, 620), (255, 255, 255, 0), [('ICE CREAM', 170, 0.18, (120, 50, 30)), ('Vanilla  2 L', 80, 0.88, (120, 50, 30))], art=art)
    loc, rot = facing(0.93, 0.55)
    d = decal('iclab', lab, 2.1, 0.88, loc, rot=rot, bend=0.93)
    d.rotation_euler = (math.radians(90 + 8.6), 0, math.radians(-35))
    return [tub, lid]


def toilet_paper():
    prof = [(0.38, 0.0), (0.97, 0.0), (1.0, 0.03), (1.0, 0.97), (0.97, 1.0), (0.38, 1.0), (0.38, 0.0)]
    o = lathe('roll', prof, segs=96, cap_bottom=False, cap_top=False)
    paper = mat('tp', (0.97, 0.97, 0.96), rough=0.9, sss=0.2, sss_radius=(1, 1, 1))
    nt = paper.node_tree; p = nt.nodes['Principled BSDF']
    vo = nt.nodes.new('ShaderNodeTexVoronoi'); vo.inputs['Scale'].default_value = 26
    bp = nt.nodes.new('ShaderNodeBump'); bp.inputs['Strength'].default_value = 0.25
    nt.links.new(vo.outputs['Distance'], bp.inputs['Height']); nt.links.new(bp.outputs['Normal'], p.inputs['Normal'])
    core = mat('core', (0.70, 0.54, 0.36), rough=0.8)
    o.data.materials.append(paper); o.data.materials.append(core)
    for poly in o.data.polygons:
        r = math.hypot(poly.center.x, poly.center.y)
        poly.material_index = 1 if r < 0.42 and abs(poly.normal.z) < 0.5 else 0
    o.rotation_euler = (math.radians(90), 0, math.radians(-30)); o.location = (0, 0.5, 1.0)
    tail = box('tail', 1.0, 1.1, 0.008, (0, 0, 0.004)); assign(tail, paper)
    tail.parent = None
    root = bpy.data.objects.new('root', None); link(root)
    o.parent = root; tail.parent = root
    o.rotation_euler = (math.radians(90), 0, 0); o.location = (-0.5, 0.5, 1.0)
    tail.rotation_euler = (0, 0, 0); tail.location = (0.0, -0.05, 0.004)
    o.location = (0.5, 0.45, 1.0); o.rotation_euler = (0, math.radians(90), 0)
    tail.location = (1.0, -0.1, 0.004)
    root.rotation_euler = (0, 0, math.radians(-25))
    return [o, tail]


def dish_soap():
    prof = [(0.0, 0), (0.5, 0), (0.56, 0.06), (0.58, 0.2), (0.58, 1.9), (0.52, 2.2), (0.3, 2.45), (0.2, 2.5)]
    btl = lathe('ds', prof)
    assign(btl, mat('dsg', (0.45, 0.82, 0.30), rough=0.12, trans=0.92, ior=1.4, sss=0.0))
    cap = lathe('dscap', [(0.0, 2.48), (0.24, 2.48), (0.25, 2.75), (0.12, 2.9), (0.0, 2.92)]); assign(cap, mat('capw', (0.96, 0.96, 0.95), rough=0.3))
    lab = text_label('dish.png', (1100, 760), (255, 255, 255, 255), [('DISH', 180, 0.30, (30, 120, 40)), ('SOAP', 180, 0.62, (30, 120, 40)), ('Lemon  500 ml', 60, 0.88, (90, 90, 90))])
    loc, rot = facing(0.585, 1.05)
    decal('dslab', lab, 1.05, 0.72, loc, rot=rot, bend=0.585)
    return [btl, cap]


def laundry():
    def art(im, d):
        W, H = im.size
        for k in range(6):
            d.arc([W * (0.1 + 0.03 * k), H * (0.48 + 0.03 * k), W * (0.9 - 0.03 * k), H * (0.98 - 0.03 * k)], 200, 340, fill=(255, 255, 255), width=14)
    lab = text_label('laundry.png', (1000, 1100), (30, 90, 200, 255), [('LAUNDRY', 150, 0.15, (255, 255, 255)), ('POWDER', 150, 0.31, (255, 230, 60)), ('2 kg', 90, 0.86, (255, 255, 255))], art=art)
    b = box('ld', 1.8, 0.9, 2.0, (0, 0, 1.0)); bevel(b, 0.02, 3); assign(b, mat('ldm', (0.12, 0.35, 0.78), rough=0.35))
    d = decal('ldlab', lab, 1.79, 1.97, (0, -0.454, 1.0))
    h = lathe('handle', [(0.42, 0), (0.42, 0.12)], cap_bottom=False, cap_top=False)
    hs = h.modifiers.new('s', 'SOLIDIFY'); hs.thickness = 0.05
    h.rotation_euler = (math.radians(90), 0, 0); h.location = (0, 0, 2.0); h.scale = (1, 0.6, 1)
    assign(h, mat('hdl', (0.95, 0.95, 0.95), rough=0.4))
    root = bpy.data.objects.new('root', None); link(root)
    for o in (b, d, h): o.parent = root
    root.rotation_euler = (0, 0, math.radians(-14))
    return [b, h]


def birthday_card():
    def art(im, d):
        W, H = im.size
        random.seed(5)
        for k in range(70):
            x, y = random.uniform(0, W), random.uniform(0, H); r = random.uniform(6, 14)
            d.ellipse([x - r, y - r, x + r, y + r], fill=random.choice([(240, 90, 110), (80, 160, 230), (250, 200, 60), (120, 200, 120)]))
        for (cx, cy, col) in [(W * 0.32, H * 0.52, (235, 70, 90)), (W * 0.52, H * 0.46, (60, 140, 225)), (W * 0.70, H * 0.54, (250, 190, 40))]:
            d.line([cx, cy + 120, cx + 10, cy + 330], fill=(120, 120, 120), width=4)
            d.ellipse([cx - 95, cy - 120, cx + 95, cy + 120], fill=col)
            d.ellipse([cx - 55, cy - 85, cx - 20, cy - 30], fill=(255, 255, 255))
    lab = text_label('card.png', (900, 1250), (255, 250, 240, 255), [('Happy', 130, 0.12, (200, 50, 80)), ('Birthday', 130, 0.24, (200, 50, 80))], art=art)
    paper = mat('cardp', (0.98, 0.97, 0.94), rough=0.7, bump=0.05, bump_scale=40)
    front = box('front', 1.6, 0.02, 2.2, (0.8, 0, 1.1)); assign(front, paper)
    back = box('back', 1.6, 0.02, 2.2, (0.8, 0, 1.1)); assign(back, paper)
    hinge = bpy.data.objects.new('hinge', None); link(hinge)
    hinge2 = bpy.data.objects.new('hinge2', None); link(hinge2)
    front.parent = hinge; back.parent = hinge2
    d = decal('cardlab', lab, 1.58, 2.18, (0.8, -0.0115, 1.1)); d.parent = hinge
    hinge.rotation_euler = (0, 0, math.radians(-10)); hinge2.rotation_euler = (0, 0, math.radians(40))
    hinge.location = hinge2.location = (-0.8, 0.3, 0)
    env = box('env', 2.2, 1.6, 0.03, (0.5, 0.9, 0.015)); assign(env, mat('envm', (0.70, 0.84, 0.95), rough=0.6))
    env.rotation_euler = (0, 0, math.radians(14))
    return [front, back, env]


def _dots_material(name, base, dot, scale=5.0):
    m = mat(name, base, rough=0.45)
    nt = m.node_tree; p = nt.nodes['Principled BSDF']
    vo = nt.nodes.new('ShaderNodeTexVoronoi'); vo.inputs['Scale'].default_value = scale
    tc = nt.nodes.new('ShaderNodeTexCoord'); nt.links.new(tc.outputs['Object'], vo.inputs['Vector'])
    ramp = nt.nodes.new('ShaderNodeValToRGB'); ramp.color_ramp.interpolation = 'CONSTANT'
    ramp.color_ramp.elements[0].color = (*[c ** 2.2 for c in dot], 1); ramp.color_ramp.elements[1].position = 0.22
    ramp.color_ramp.elements[1].color = (*[c ** 2.2 for c in base], 1)
    nt.links.new(vo.outputs['Distance'], ramp.inputs['Fac']); nt.links.new(ramp.outputs['Color'], p.inputs['Base Color'])
    return m


def present():
    b = box('gift', 1.8, 1.8, 1.4, (0, 0, 0.7)); bevel(b, 0.015, 2)
    assign(b, _dots_material('wrap', (0.80, 0.10, 0.16), (0.98, 0.95, 0.90), 5.5))
    rib = mat('ribbon', (0.95, 0.78, 0.30), rough=0.25, metal=0.5, sss=0.0)
    r1 = box('r1', 1.84, 0.28, 1.44, (0, 0, 0.7)); assign(r1, rib)
    r2 = box('r2', 0.28, 1.84, 1.44, (0, 0, 0.7)); assign(r2, rib)
    objs = [b]
    for k, ang in enumerate([35, 145]):
        prof = [(0.32 + 0.06 * math.cos(2 * math.pi * i / 16), 0.03 * math.sin(2 * math.pi * i / 16)) for i in range(17)]
        lp = lathe('loop%d' % k, prof, segs=48, cap_bottom=False, cap_top=False)
        lp.scale = (1.0, 0.55, 1.0); lp.rotation_euler = (math.radians(90), math.radians(-20 if k else 20), math.radians(ang))
        lp.location = (0.28 * math.cos(math.radians(ang)), 0.28 * math.sin(math.radians(ang)), 1.62)
        assign(lp, rib)
    knot = uvsphere('knot', 0.16, loc=(0, 0, 1.45), scale=(1, 1, 0.7)); assign(knot, rib)
    root = bpy.data.objects.new('root', None); link(root)
    for o in [b, r1, r2, knot] + [bpy.data.objects['loop0'], bpy.data.objects['loop1']]:
        o.parent = root
    root.rotation_euler = (0, 0, math.radians(-20))
    return [b, bpy.data.objects['loop0'], bpy.data.objects['loop1']]


def wrapping_paper():
    roll = cyl('roll', 0.32, 3.0, segs=64, loc=(0, 0, 0.32)); roll.rotation_euler = (0, math.radians(90), 0)
    for p in roll.data.polygons: p.use_smooth = True
    m = _dots_material('wp', (0.15, 0.45, 0.78), (0.98, 0.85, 0.30), 6)
    assign(roll, m)
    sheet = box('sheet', 3.0, 1.4, 0.006, (0, -0.72, 0.003)); assign(sheet, m)
    bd = sheet.modifiers.new('bend', 'SIMPLE_DEFORM'); bd.deform_method = 'BEND'; bd.angle = math.radians(-25); bd.deform_axis = 'X'
    root = bpy.data.objects.new('root', None); link(root); roll.parent = root; sheet.parent = root
    root.rotation_euler = (0, 0, math.radians(-18))
    return [roll, sheet]


def candles():
    prof = [(0.0, 0), (0.5, 0), (0.52, 0.03), (0.52, 2.0), (0.49, 2.05), (0.3, 2.0), (0.0, 1.96)]
    c = lathe('candle', prof); displace(c, 0.01, 0.3)
    assign(c, mat('wax', (0.98, 0.95, 0.88), rough=0.4, sss=1.0, sss_radius=(1, 0.8, 0.5)))
    wick = cyl('wick', 0.025, 0.25, segs=12, loc=(0, 0, 2.08)); assign(wick, mat('wk', (0.08, 0.06, 0.05), rough=0.8))
    flame = lathe('flame', [(0.0, 2.15), (0.08, 2.22), (0.11, 2.32), (0.08, 2.48), (0.03, 2.6), (0.0, 2.66)], segs=32)
    assign(flame, mat('fl', (1.0, 0.75, 0.35), rough=0.5, emission=(1.0, 0.72, 0.30), emit=18))
    L = bpy.data.lights.new('glow', 'POINT'); L.energy = 6; L.color = (1.0, 0.7, 0.35); L.shadow_soft_size = 0.1
    lo = bpy.data.objects.new('glow', L); lo.location = (0, 0, 2.35); link(lo)
    return [c, flame]


def custom():
    bag = open_box('bag', 1.6, 0.85, 1.9, wall=0.02); bag.location = (0, 0, 0.95)
    assign(bag, mat('kraft', (0.72, 0.54, 0.34), rough=0.85, bump=0.25, bump_scale=30, color2=(0.66, 0.48, 0.29), mix_scale=8))
    objs = [bag]
    rope = mat('rope', (0.55, 0.40, 0.24), rough=0.9)
    for k, y in enumerate((-0.40, 0.40)):
        prof = [(0.38 + 0.03 * math.cos(2 * math.pi * i / 12), 0.03 * math.sin(2 * math.pi * i / 12)) for i in range(13)]
        h = lathe('h%d' % k, prof, segs=48, cap_bottom=False, cap_top=False)
        h.rotation_euler = (math.radians(90), 0, 0); h.location = (0, y, 1.9); h.scale = (1, 1.4, 1)
        assign(h, rope); objs.append(h)
    root = bpy.data.objects.new('root', None); link(root)
    for o in objs: o.parent = root
    root.rotation_euler = (0, 0, math.radians(-18))
    return objs


ITEMS = dict(frozen_peas=frozen_peas, ice_cream=ice_cream, toilet_paper=toilet_paper, dish_soap=dish_soap, laundry=laundry,
             birthday_card=birthday_card, present=present, wrapping_paper=wrapping_paper, candles=candles, custom=custom)
