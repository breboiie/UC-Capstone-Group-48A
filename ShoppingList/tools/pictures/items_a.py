"""Item models, part A: eggs, dairy, produce."""
import math, random
from studio import *


def eggs():
    # egg profile: wider at the bottom, narrower at the top
    prof = []
    for i in range(41):
        t = i / 40; z = -math.cos(math.pi * t)  # -1..1
        r = math.sqrt(max(0, 1 - z * z)) * (0.78 - 0.08 * z)
        prof.append((r, z * 1.0))
    o = lathe('egg', prof)
    o.rotation_euler = (0, math.radians(90), math.radians(-60)); o.location = (0, 0, 0.8)
    assign(o, mat('shell', (0.70, 0.45, 0.29), rough=0.6, sss=0.08, sss_radius=(1, 0.6, 0.4),
                  bump=0.15, bump_scale=60, color2=(0.62, 0.38, 0.24), mix_scale=4))
    return [o]


def cheese():
    # a wedge of Swiss cheese with holes
    import bmesh
    me = bpy.data.meshes.new('w'); bm = bmesh.new()
    R, H, ang = 2.0, 1.0, math.radians(38)
    pts = [(0, 0)] + [(R * math.cos(-ang / 2 + ang * i / 12), R * math.sin(-ang / 2 + ang * i / 12)) for i in range(13)]
    bot = [bm.verts.new((x, y, 0)) for x, y in pts]; top = [bm.verts.new((x, y, H)) for x, y in pts]
    bm.faces.new(list(reversed(bot))); bm.faces.new(top)
    n = len(pts)
    for i in range(n):
        j = (i + 1) % n; bm.faces.new((bot[i], bot[j], top[j], top[i]))
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
    bm.to_mesh(me); bm.free()
    w = link(bpy.data.objects.new('wedge', me))
    random.seed(3)
    holes = []
    for k in range(16):
        rr = random.uniform(0.1, 0.22); d = random.uniform(0.5, 1.85); side = random.choice([-1, 1, 0, 2])
        if side in (-1, 1):   # on a cut face, pushed out so it makes a dimple, not a tunnel
            a = side * ang / 2; nrm = (-math.sin(a) * side * -1, math.cos(a) * side, 0)
            loc = (d * math.cos(a) + nrm[0] * rr * 0.45, d * math.sin(a) + nrm[1] * rr * 0.45, random.uniform(0.2, 0.8))
        elif side == 0:       # on top
            a = random.uniform(-ang / 3, ang / 3); loc = (d * math.cos(a), d * math.sin(a), 1.0 + rr * 0.45)
        else:                 # on the curved rind
            a = random.uniform(-ang / 3, ang / 3); loc = ((R + rr * 0.45) * math.cos(a), (R + rr * 0.45) * math.sin(a), random.uniform(0.25, 0.75))
        holes.append(uvsphere('hole%d' % k, r=rr, segs=32, rings=16, loc=loc))
    for k, s in enumerate(holes):
        md = w.modifiers.new('b%d' % k, 'BOOLEAN'); md.object = s; md.operation = 'DIFFERENCE'; md.solver = 'EXACT'
        s.hide_render = True; s.display_type = 'WIRE'; s.parent = w
    bev = w.modifiers.new('bev', 'BEVEL'); bev.width = 0.02; bev.segments = 3
    for p in w.data.polygons: p.use_smooth = True
    assign(w, mat('cheese', (0.98, 0.80, 0.38), rough=0.45, sss=0.4, sss_radius=(1, 0.8, 0.4), bump=0.05, bump_scale=30))
    w.rotation_euler = (0, 0, math.radians(200))
    return [w]


def butter():
    b = box('butter', 2.2, 1.1, 0.75, (0.45, 0, 0.375)); bevel(b, 0.05, 4)
    assign(b, mat('butter', (0.99, 0.90, 0.58), rough=0.32, sss=0.5, sss_radius=(1, 0.9, 0.5), bump=0.04, bump_scale=8))
    wr = box('wrap', 1.45, 1.15, 0.80, (-0.35, 0, 0.40)); bevel(wr, 0.03, 3)
    assign(wr, mat('foil', (0.86, 0.70, 0.30), rough=0.28, metal=0.9, bump=0.25, bump_scale=14))
    lab = text_label('butter.png', (1100, 520), (250, 246, 232, 255), [('BUTTER', 170, 0.42, (20, 60, 140)), ('salted  250 g', 70, 0.75, (90, 90, 90))])
    d = decal('butterlab', lab, 1.1, 0.52, (-0.35, -0.58, 0.40)); d.parent = wr; d.location = (0, -0.58, 0)
    root = bpy.data.objects.new('root', None); link(root)
    for o in (b, wr): o.parent = root
    wr.location = (-0.35, 0, 0.40); b.location = (0.45, 0, 0.375)
    root.rotation_euler = (0, 0, math.radians(-12))
    return [b, wr]


def cream():
    prof = [(0.0, 0), (0.55, 0), (0.6, 0.05), (0.6, 1.25), (0.52, 1.5), (0.3, 1.7), (0.26, 1.78), (0.26, 1.8)]
    btl = lathe('btl', prof); assign(btl, mat('hdpe', (0.97, 0.97, 0.96), rough=0.35, sss=0.3, sss_radius=(1, 1, 1)))
    cap = lathe('cap', [(0, 1.78), (0.3, 1.78), (0.31, 1.8), (0.31, 2.02), (0.28, 2.05), (0, 2.05)])
    assign(cap, mat('cap', (0.15, 0.35, 0.75), rough=0.35, bump=0.3, bump_scale=200))
    lab = text_label('cream.png', (1400, 700), (255, 255, 255, 255), [('Cream', 230, 0.45, (25, 60, 140))],
                     band=(0.74, 0.88, (40, 110, 200, 255)))
    loc, rot = facing(0.612, 0.62)
    decal('creamlab', lab, 1.5, 0.75, loc, rot=rot, bend=0.612)
    return [btl, cap]


def carrots():
    prof = [(0.0, 0)]
    for i in range(1, 41):
        t = i / 40; prof.append((0.48 * (t ** 0.6), t * 2.6))
    prof += [(0.44, 2.64), (0.2, 2.68), (0.0, 2.69)]
    c = lathe('carrot', prof, segs=48); displace(c, 0.03, 0.3)
    m = mat('carrot', (0.96, 0.45, 0.08), rough=0.5, sss=0.25, sss_radius=(1, 0.4, 0.1), color2=(0.92, 0.38, 0.05), mix_scale=3)
    nt = m.node_tree; p = nt.nodes['Principled BSDF']
    wv = nt.nodes.new('ShaderNodeTexWave'); wv.wave_type = 'BANDS'; wv.bands_direction = 'Z'; wv.inputs['Scale'].default_value = 3.5; wv.inputs['Distortion'].default_value = 10
    bp = nt.nodes.new('ShaderNodeBump'); bp.inputs['Strength'].default_value = 0.35
    nt.links.new(wv.outputs['Fac'], bp.inputs['Height']); nt.links.new(bp.outputs['Normal'], p.inputs['Normal'])
    assign(c, m)
    random.seed(5)
    stem_m = mat('stem', (0.28, 0.52, 0.14), rough=0.5, sss=0.2)
    leaf_m = mat('leafc', (0.22, 0.48, 0.12), rough=0.45, sss=0.3)
    for k in range(9):
        a = random.uniform(-0.55, 0.55); b = random.uniform(-0.4, 0.4); h = random.uniform(1.0, 1.5)
        st = cyl('st%d' % k, 0.03, h, segs=10)
        st.location = (math.sin(a) * h / 2, math.sin(b) * h / 2, 2.62 + math.cos(a) * h / 2)
        st.rotation_euler = (-b, a, 0); assign(st, stem_m); st.parent = c
        for j in range(4):
            t = 0.55 + 0.15 * j
            lf = uvsphere('lf%d_%d' % (k, j), 0.16, segs=12, rings=8, scale=(1.0, 0.45, 0.08))
            lf.location = (math.sin(a) * h * t, math.sin(b) * h * t, 2.62 + math.cos(a) * h * t)
            lf.rotation_euler = (random.uniform(0, 3), random.uniform(0, 3), random.uniform(0, 3))
            assign(lf, leaf_m); lf.parent = c
    c.rotation_euler = (0, math.radians(-80), math.radians(-35)); c.location = (1.1, 0, 0.5)
    return [c]


def limes():
    prof = [(0.0, -1.18)]
    for i in range(1, 40):
        t = i / 40; z = -1.0 + 2 * t
        prof.append((0.86 * math.sqrt(max(0, 1 - z * z)) ** 0.9, z * 1.08))
    prof.append((0.0, 1.16))
    o = lathe('lime', prof)
    assign(o, mat('lime', (0.30, 0.56, 0.12), rough=0.3, sss=0.15, sss_radius=(0.5, 1, 0.3), bump=0.3, bump_scale=110,
                  color2=(0.42, 0.66, 0.16), mix_scale=3, coat=0.2))
    o.rotation_euler = (0, math.radians(90), math.radians(-55)); o.location = (0, 0, 0.86)
    return [o]


def mandarins():
    o = uvsphere('mand', 1.0, scale=(1.0, 1.0, 0.78), loc=(0, 0, 0.78))
    assign(o, mat('mand', (0.97, 0.46, 0.05), rough=0.42, sss=0.15, sss_radius=(1, 0.4, 0.1), bump=0.3, bump_scale=80,
                  color2=(0.99, 0.55, 0.10), mix_scale=3))
    st = cyl('stalk', 0.05, 0.12, segs=12, loc=(0, 0, 1.58)); assign(st, mat('st', (0.3, 0.35, 0.12), rough=0.7))
    # one leaf
    leaf = uvsphere('leaf', 0.5, segs=24, rings=12, scale=(1.0, 0.38, 0.04), loc=(0.42, 0.05, 1.62))
    leaf.rotation_euler = (0, math.radians(-15), math.radians(20)); assign(leaf, mat('leaf', (0.12, 0.38, 0.08), rough=0.35, sss=0.2))
    return [o, st, leaf]


def potatoes():
    o = uvsphere('pot', 1.0, scale=(1.35, 0.9, 0.78), loc=(0, 0, 0.78))
    displace(o, 0.12, 0.7)
    smooth(o, 1)
    o.rotation_euler = (0, 0, math.radians(20))
    assign(o, mat('pot', (0.78, 0.60, 0.38), rough=0.75, bump=0.35, bump_scale=25, color2=(0.68, 0.50, 0.30), mix_scale=5))
    return [o]


def kiwi_fruit():
    # one kiwi cut in half: both halves, cut faces showing
    objs = []
    def half(name, loc, rot):
        prof = []
        for i in range(25):
            t = i / 24 * (math.pi / 2); prof.append((0.82 * math.sin(t) if i else 0.0, -1.0 * math.cos(t)))
        prof.append((0.0, 0.0))
        o = lathe(name, prof, segs=64)
        o.location = loc; o.rotation_euler = rot
        skin = mat(name + 's', (0.45, 0.32, 0.18), rough=0.9, bump=0.5, bump_scale=200, bump_detail=2)
        o.data.materials.append(skin)
        # flesh on the flat face: radial image texture
        from PIL import Image, ImageDraw
        W = 1024; im = Image.new('RGB', (W, W), (120, 70, 40)); d = ImageDraw.Draw(im); c = W / 2
        d.ellipse([c - 470, c - 470, c + 470, c + 470], fill=(118, 170, 40))
        d.ellipse([c - 420, c - 420, c + 420, c + 420], fill=(140, 190, 50))
        random.seed(9)
        for k in range(300):
            a = random.uniform(0, 6.283); r = random.uniform(150, 230)
            d.ellipse([c + r * math.cos(a) - 9, c + r * math.sin(a) - 5, c + r * math.cos(a) + 9, c + r * math.sin(a) + 5], fill=(25, 20, 15))
        d.ellipse([c - 130, c - 130, c + 130, c + 130], fill=(225, 230, 175))
        for k in range(60):
            a = random.uniform(0, 6.283); d.line([c + 130 * math.cos(a), c + 130 * math.sin(a), c + 440 * math.cos(a), c + 440 * math.sin(a)], fill=(150, 200, 60), width=3)
        p = TEX + 'kiwi.png'; im.save(p)
        fm = bpy.data.materials.new(name + 'f'); fm.use_nodes = True; nt = fm.node_tree; pb = nt.nodes['Principled BSDF']
        tx = nt.nodes.new('ShaderNodeTexImage'); tx.image = bpy.data.images.load(p)
        tc = nt.nodes.new('ShaderNodeTexCoord'); mp = nt.nodes.new('ShaderNodeMapping')
        mp.inputs['Scale'].default_value = (0.61, 0.61, 1); mp.inputs['Location'].default_value = (0.5, 0.5, 0)
        nt.links.new(tc.outputs['Object'], mp.inputs['Vector']); nt.links.new(mp.outputs['Vector'], tx.inputs['Vector'])
        nt.links.new(tx.outputs['Color'], pb.inputs['Base Color']); pb.inputs['Roughness'].default_value = 0.25
        pb.inputs['Subsurface Weight'].default_value = 0.3; pb.inputs['Subsurface Scale'].default_value = 0.05
        o.data.materials.append(fm)
        for poly in o.data.polygons:
            poly.material_index = 1 if poly.center.z > -0.01 and poly.normal.z > 0.9 else 0
        return o
    a = half('h1', (0.0, 0, 0.82), (math.radians(180 - 62), 0, math.radians(-20)))
    b = half('h2', (1.1, 0.7, 0.82), (math.radians(180 + 90), 0, math.radians(30)))
    return [a, b]


def beetroot():
    prof = [(0.0, -1.55)]
    for i in range(1, 30):
        t = i / 29
        z = -1.55 + t * 2.45
        if z < -0.55:
            r = 0.04 + 0.35 * ((z + 1.55) / 1.0) ** 2
        else:
            r = math.sqrt(max(0, 1 - ((z - 0.15) / 0.75) ** 2)) * 0.95 + (0.0)
        prof.append((max(r, 0.02), z))
    prof.append((0.0, 0.92))
    o = lathe('beet', prof, segs=64); displace(o, 0.04, 0.4)
    assign(o, mat('beet', (0.35, 0.04, 0.12), rough=0.55, sss=0.15, sss_radius=(1, 0.2, 0.3), bump=0.3, bump_scale=18,
                  color2=(0.45, 0.10, 0.16), mix_scale=4))
    objs = [o]
    random.seed(4)
    for k in range(5):
        a = random.uniform(-0.45, 0.45); h = random.uniform(0.7, 1.1)
        st = cyl('st%d' % k, 0.05, h, segs=12)
        st.location = (math.sin(a) * h / 2, random.uniform(-0.1, 0.1), 0.85 + math.cos(a) * h / 2)
        st.rotation_euler = (random.uniform(-0.3, 0.3), a, 0)
        assign(st, mat('stem', (0.65, 0.08, 0.2), rough=0.5, sss=0.3)); st.parent = o; objs.append(st)
        lf = uvsphere('lf%d' % k, 0.42, segs=24, rings=12, scale=(0.5, 1.0, 0.06))
        lf.location = (math.sin(a) * (h + 0.35), st.location[1], 0.85 + math.cos(a) * (h + 0.35))
        lf.rotation_euler = (math.radians(90), a, random.uniform(-0.5, 0.5))
        assign(lf, mat('leafb', (0.20, 0.38, 0.12), rough=0.45, sss=0.2, bump=0.2, bump_scale=10)); lf.parent = o
    o.rotation_euler = (0, math.radians(-28), math.radians(15)); o.location = (0, 0, 1.3)
    return [o]


def plums():
    o = uvsphere('plum', 1.0, scale=(0.95, 0.88, 1.05), loc=(0, 0, 1.0))
    m = mat('plum', (0.28, 0.05, 0.17), rough=0.42, sss=0.2, sss_radius=(1, 0.3, 0.5), bump=0.04, bump_scale=20,
            color2=(0.36, 0.09, 0.24), mix_scale=2.5, coat=0.1)
    pb = m.node_tree.nodes['Principled BSDF']
    pb.inputs['Sheen Weight'].default_value = 0.7; pb.inputs['Sheen Tint'].default_value = (0.55, 0.55, 0.7, 1)
    assign(o, m)
    # the groove down one side
    g = uvsphere('groove', 0.06, segs=24, rings=12, scale=(1, 1, 14), loc=(0, -0.9, 1.0))
    md = o.modifiers.new('g', 'BOOLEAN'); md.object = g; md.operation = 'DIFFERENCE'; g.hide_render = True; g.display_type = 'WIRE'
    sm = o.modifiers.new('sm', 'SMOOTH'); sm.factor = 0.8; sm.iterations = 6
    st = cyl('stalk', 0.035, 0.3, segs=12, loc=(0.03, 0, 2.15)); st.rotation_euler = (0, math.radians(12), 0)
    assign(st, mat('pst', (0.42, 0.33, 0.18), rough=0.7))
    o.rotation_euler = (0, 0, math.radians(-30))
    g.parent = o; st.parent = o; g.location = (0, -0.9, 0); st.location = (0.03, 0, 1.12)
    return [o]


ITEMS = dict(plums=plums, eggs=eggs, cheese=cheese, butter=butter, cream=cream, carrots=carrots, limes=limes, mandarins=mandarins,
             potatoes=potatoes, kiwi_fruit=kiwi_fruit, beetroot=beetroot)
