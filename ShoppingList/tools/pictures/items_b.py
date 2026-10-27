"""Item models, part B: meat, bakery, pantry, drinks."""
import math, random, bmesh
from studio import *


def open_box(name, sx, sy, sz, wall=0.04):
    """A tray or bag: a box with no top, given thickness."""
    me = bpy.data.meshes.new(name); bm = bmesh.new()
    bmesh.ops.create_cube(bm, size=1.0)
    for v in bm.verts:
        v.co = Vector((v.co.x * sx, v.co.y * sy, v.co.z * sz))
    top = [f for f in bm.faces if f.normal.z > 0.9]
    bmesh.ops.delete(bm, geom=top, context='FACES')
    bm.to_mesh(me); bm.free()
    o = link(bpy.data.objects.new(name, me))
    so = o.modifiers.new('solid', 'SOLIDIFY'); so.thickness = wall
    bv = o.modifiers.new('bev', 'BEVEL'); bv.width = wall * 0.6; bv.segments = 3
    for p in o.data.polygons: p.use_smooth = True
    return o


def shaped(name, sx, sy, sz, cuts, fn, sub=2):
    """A subdivided box whose vertices are moved by fn(x, y, z) -> (x, y, z)."""
    me = bpy.data.meshes.new(name); bm = bmesh.new()
    bmesh.ops.create_cube(bm, size=2.0)
    bmesh.ops.subdivide_edges(bm, edges=bm.edges, cuts=cuts, use_grid_fill=True)
    for v in bm.verts:
        x, y, z = fn(v.co.x, v.co.y, v.co.z)
        v.co = Vector((x * sx, y * sy, z * sz))
    bm.to_mesh(me); bm.free()
    o = link(bpy.data.objects.new(name, me))
    return smooth(o, sub)


def chicken():
    # one raw chicken drumstick: knuckle bone at one end, meaty end at the other
    prof = [(0.0, -0.05), (0.16, -0.02), (0.2, 0.06), (0.17, 0.16), (0.13, 0.3), (0.14, 0.55)]
    for i in range(1, 21):
        t = i / 20; z = 0.55 + t * 1.6
        prof.append((0.14 + 0.46 * math.sin(t * math.pi / 2) ** 1.5, z))
    for i in range(1, 11):
        t = i / 10; prof.append((0.60 * math.cos(t * math.pi / 2) ** 0.8, 2.15 + 0.5 * math.sin(t * math.pi / 2)))
    o = lathe('drum', prof, segs=64); displace(o, 0.05, 0.35)
    skin = mat('skin', (0.84, 0.54, 0.24), rough=0.35, sss=0.25, sss_radius=(1, 0.5, 0.3), bump=0.45, bump_scale=30,
               color2=(0.66, 0.36, 0.13), mix_scale=4, coat=0.45)
    bone = mat('bone', (0.93, 0.88, 0.80), rough=0.5, sss=0.3)
    o.data.materials.append(skin); o.data.materials.append(bone)
    for p in o.data.polygons:
        p.material_index = 1 if p.center.z < 0.42 else 0
    o.rotation_euler = (0, math.radians(-86), math.radians(-30)); o.location = (1.2, 0, 0.62)
    return [o]


def mince():
    tray = open_box('tray', 2.4, 1.7, 0.42, wall=0.05); tray.location = (0, 0, 0.21)
    assign(tray, mat('tray', (0.08, 0.08, 0.08), rough=0.35))
    meat = shaped('meat', 1.12, 0.78, 0.2, 10, lambda x, y, z: (x, y, z + (0.5 * (1 - x * x) * (1 - y * y) if z > 0 else 0)), sub=2)
    meat.location = (0, 0, 0.3)
    displace(meat, 0.06, 0.04, depth=1)
    assign(meat, mat('mince', (0.72, 0.16, 0.17), rough=0.45, sss=0.2, sss_radius=(1, 0.2, 0.2), bump=0.6, bump_scale=60,
                     color2=(0.93, 0.72, 0.70), mix_scale=45, coat=0.2))
    film = box('film', 2.42, 1.72, 0.004, (0, 0, 0.47)); assign(film, mat('film', (1, 1, 1), rough=0.05, trans=1.0, ior=1.3))
    lab = text_label('mince.png', (700, 420), (255, 255, 255, 255), [('Beef mince', 96, 0.33, (150, 20, 20)), ('500 g', 80, 0.68, (40, 40, 40))],
                     band=(0.88, 1.0, (190, 30, 30, 255)))
    d = decal('mincelab', lab, 0.9, 0.54, (0.55, -0.35, 0.476), rot=(0, 0, math.radians(-8)))
    root = bpy.data.objects.new('root', None); link(root)
    for o in (tray, meat, film, d): o.parent = root
    root.rotation_euler = (0, 0, math.radians(8))
    return [tray, meat]


def sausages():
    prof = [(0.0, 0.0), (0.05, 0.02), (0.07, 0.07), (0.16, 0.12)]
    for i in range(1, 30):
        t = i / 30; prof.append((0.27 * (1 - (2 * t - 1) ** 8) + 0.16 * (2 * t - 1) ** 8, 0.12 + t * 2.8))
    prof += [(0.16, 2.92), (0.07, 2.97), (0.05, 3.02), (0.0, 3.04)]
    o = lathe('saus', prof, segs=48)
    bd = o.modifiers.new('bend', 'SIMPLE_DEFORM'); bd.deform_method = 'BEND'; bd.angle = math.radians(35); bd.deform_axis = 'Y'
    assign(o, mat('saus', (0.70, 0.38, 0.22), rough=0.3, sss=0.3, sss_radius=(1, 0.4, 0.3), bump=0.3, bump_scale=25,
                  color2=(0.55, 0.26, 0.13), mix_scale=4, coat=0.5))
    o.rotation_euler = (0, math.radians(90), math.radians(-60)); o.location = (0, 0, 0.3)
    return [o]


def fish():
    def f(x, y, z):
        t = (x + 1) / 2              # 0 at the tail, 1 at the head end
        w = 0.35 + 0.65 * math.sin(min(1, t * 1.6) * math.pi / 2)
        h = 0.25 + 0.75 * math.sin(min(1, t * 1.3) * math.pi / 2)
        return x, y * w, (z + 1) / 2 * h * (1 - 0.35 * y * y)
    o = shaped('salmon', 1.7, 0.66, 0.62, 8, f)
    m = mat('salmon', (0.98, 0.52, 0.33), rough=0.3, sss=0.5, sss_radius=(1, 0.45, 0.3), coat=0.25)
    nt = m.node_tree; p = nt.nodes['Principled BSDF']
    wv = nt.nodes.new('ShaderNodeTexWave'); wv.inputs['Scale'].default_value = 3.0; wv.inputs['Distortion'].default_value = 5
    wv.bands_direction = 'DIAGONAL'
    ramp = nt.nodes.new('ShaderNodeValToRGB')
    ramp.color_ramp.elements[0].color = (0.96, 0.26, 0.11, 1); ramp.color_ramp.elements[0].position = 0.86
    ramp.color_ramp.elements[1].color = (0.98, 0.62, 0.45, 1); ramp.color_ramp.elements[1].position = 0.97
    nt.links.new(wv.outputs['Fac'], ramp.inputs['Fac']); nt.links.new(ramp.outputs['Color'], p.inputs['Base Color'])
    assign(o, m)
    o.rotation_euler = (0, 0, math.radians(-20))
    return [o]


def bread():
    def f(x, y, z):
        if z > 0:
            z = z + 0.7 * (1 - y * y) ** 0.7 * z
            y = y * (1 + 0.12 * z)
        return x, y, z
    crust = mat('crust', (0.78, 0.50, 0.20), rough=0.5, bump=0.2, bump_scale=20, color2=(0.62, 0.35, 0.12), mix_scale=1.2, sss=0.05)
    crumb = mat('crumb', (0.96, 0.90, 0.76), rough=0.9, bump=1.0, bump_scale=70, bump_detail=4, sss=0.3,
                color2=(0.90, 0.82, 0.66), mix_scale=40)
    def piece(name, length, loc, rot):
        o = shaped(name, length, 0.62, 0.5, 8, f)
        o.data.materials.append(crust); o.data.materials.append(crumb)
        for p in o.data.polygons:
            p.material_index = 1 if abs(p.normal.x) > 0.85 else 0
        o.location = loc; o.rotation_euler = rot
        return o
    loaf = piece('loaf', 1.25, (0.3, 0, 0.5), (0, 0, 0))
    s1 = piece('slice1', 0.07, (-1.12, -0.02, 0.5), (0, math.radians(10), 0))
    s2 = piece('slice2', 0.07, (-1.42, -0.06, 0.48), (0, math.radians(22), 0))
    root = bpy.data.objects.new('root', None); link(root)
    for o in (loaf, s1, s2): o.parent = root
    root.rotation_euler = (0, 0, math.radians(38))
    return [loaf, s1, s2]


def _box_item(name, sx, sy, sz, colour, front_png, side_colour=None, rough=0.4, azim_face=True):
    b = box(name, sx, sy, sz, (0, 0, sz / 2)); bevel(b, 0.02, 3)
    assign(b, mat(name + 'm', colour, rough=rough))
    d = decal(name + 'lab', front_png, sx * 0.995, sz * 0.995, (0, -sy / 2 - 0.004, sz / 2))
    d.parent = None
    root = bpy.data.objects.new(name + 'root', None); link(root)
    b.parent = root; d.parent = root
    root.rotation_euler = (0, 0, math.radians(-12))
    return b


def tea():
    def art(im, d):
        W, H = im.size
        d.rectangle([0, int(H * 0.70), W, int(H * 0.73)], fill=(214, 175, 80))
        d.ellipse([W * 0.62, H * 0.08, W * 0.94, H * 0.62], outline=(214, 175, 80), width=10)
    lab = text_label('tea.png', (1200, 760), (24, 86, 56, 255), [('TEA', 260, 0.36, (250, 240, 215)), ('100 tea bags', 80, 0.85, (250, 240, 215))])
    return [_box_item('tea', 2.0, 1.0, 1.25, (0.10, 0.34, 0.22), lab)]


def sugar():
    lab = text_label('sugar.png', (800, 1100), (250, 250, 250, 255), [('SUGAR', 170, 0.40, (25, 70, 160)), ('White sugar', 70, 0.56, (60, 60, 60)), ('1 kg', 90, 0.82, (255, 255, 255))],
                     band=(0.72, 0.93, (25, 70, 160, 255)))
    return [_paper_bag('sugar', lab, (0.97, 0.97, 0.96))]


def flour():
    lab = text_label('flour.png', (800, 1100), (250, 247, 240, 255), [('FLOUR', 170, 0.40, (180, 30, 35)), ('Plain flour', 70, 0.56, (60, 60, 60)), ('1 kg', 90, 0.82, (255, 255, 255))],
                     band=(0.72, 0.93, (180, 30, 35, 255)))
    return [_paper_bag('flour', lab, (0.96, 0.94, 0.88))]


def _paper_bag(name, lab, colour):
    # a paper bag with the top folded over
    def f(x, y, z):
        if z > 0.6:
            y = y * (1 - (z - 0.6) / 0.4 * 0.85)
        return x, y * (1 + 0.04 * (1 - x * x)), z
    o = shaped(name, 0.72, 0.42, 1.0, 8, f, sub=1); o.location = (0, 0, 1.0)
    displace(o, 0.015, 0.2)
    assign(o, mat(name + 'm', colour, rough=0.8, bump=0.15, bump_scale=25))
    d = decal(name + 'lab', lab, 1.3, 1.3 * 1100 / 800 * 0.98, (0, -0.445, 0.86))
    root = bpy.data.objects.new(name + 'root', None); link(root)
    o.parent = root; d.parent = root
    root.rotation_euler = (0, 0, math.radians(-14))
    return o


def rice():
    def art(im, d):
        W, H = im.size
        d.rounded_rectangle([W * 0.12, H * 0.50, W * 0.88, H * 0.78], 40, fill=(245, 243, 236))
        random.seed(1)
        for k in range(700):
            x = random.uniform(W * 0.14, W * 0.86); y = random.uniform(H * 0.52, H * 0.76); a = random.uniform(0, 3.14)
            dx, dy = 14 * math.cos(a), 14 * math.sin(a)
            d.line([x - dx, y - dy, x + dx, y + dy], fill=random.choice([(255, 255, 255), (232, 228, 214), (220, 215, 200)]), width=8)
    lab = text_label('rice.png', (800, 1100), (235, 190, 40, 255), [('RICE', 190, 0.20, (150, 25, 25)), ('Long grain  1 kg', 66, 0.40, (90, 40, 20))], art=art)
    return [_paper_bag('rice', lab, (0.93, 0.76, 0.18))]


def cereal():
    def art(im, d):
        W, H = im.size
        d.ellipse([W * 0.12, H * 0.52, W * 0.88, H * 0.92], fill=(250, 250, 250))
        d.ellipse([W * 0.12, H * 0.48, W * 0.88, H * 0.62], fill=(255, 250, 235))
        random.seed(2)
        for k in range(90):
            x = random.uniform(W * 0.2, W * 0.8); y = random.uniform(H * 0.44, H * 0.58)
            r = random.uniform(16, 30)
            d.ellipse([x - r, y - r * 0.6, x + r, y + r * 0.6], fill=random.choice([(230, 160, 50), (215, 140, 40), (240, 180, 70)]))
    lab = text_label('cereal.png', (900, 1300), (230, 110, 30, 255), [('CEREAL', 150, 0.13, (255, 255, 255)), ('Corn flakes', 80, 0.27, (255, 240, 200))], art=art)
    b = _box_item('cereal', 1.55, 0.55, 2.25, (0.85, 0.40, 0.10), lab)
    return [b]


def coffee():
    jar = lathe('jar', [(0.0, 0), (0.72, 0), (0.78, 0.06), (0.78, 1.55), (0.7, 1.68), (0.6, 1.72), (0.6, 1.8)], cap_top=False)
    sol = jar.modifiers.new('s', 'SOLIDIFY'); sol.thickness = 0.03
    assign(jar, glass('jarg', (0.97, 0.96, 0.93)))
    fill = lathe('grounds', [(0.0, 0.05), (0.74, 0.05), (0.74, 1.4), (0.0, 1.45)])
    assign(fill, mat('gr', (0.22, 0.12, 0.06), rough=0.8, bump=1.0, bump_scale=120, color2=(0.32, 0.18, 0.08), mix_scale=60))
    lid = lathe('lid', [(0.0, 1.75), (0.66, 1.75), (0.68, 1.78), (0.68, 2.05), (0.64, 2.1), (0.0, 2.1)])
    assign(lid, mat('lid', (0.30, 0.17, 0.08), rough=0.35, bump=0.4, bump_scale=150))
    lab = text_label('coffee.png', (1300, 560), (245, 236, 220, 255), [('COFFEE', 200, 0.42, (70, 35, 15)), ('Instant  200 g', 70, 0.80, (110, 70, 40))])
    loc, rot = facing(0.795, 0.8)
    decal('coffeelab', lab, 1.55, 0.67, loc, rot=rot, bend=0.795)
    return [jar, lid]


def pasta():
    # a bundle of dry spaghetti
    random.seed(7)
    m = mat('pasta', (0.97, 0.85, 0.50), rough=0.35, sss=0.4, sss_radius=(1, 0.8, 0.3))
    me = bpy.data.meshes.new('sp'); bm = bmesh.new()
    for k in range(240):
        a = random.uniform(0, 6.283); r = 0.42 * math.sqrt(random.random())
        y, z = r * math.cos(a), r * math.sin(a)
        L = 3.2 + random.uniform(-0.05, 0.05); off = random.uniform(-0.04, 0.04)
        res = bmesh.ops.create_cone(bm, cap_ends=True, segments=8, radius1=0.022, radius2=0.022, depth=L)
        for v in res['verts']:
            co = v.co.copy(); v.co = Vector((co.z + off, co.x + y, co.y + z))
    bm.to_mesh(me); bm.free()
    o = link(bpy.data.objects.new('spaghetti', me)); assign(o, m)
    band = lathe('band', [(0.45, -0.12), (0.45, 0.12)], cap_bottom=False, cap_top=False)
    sb = band.modifiers.new('s', 'SOLIDIFY'); sb.thickness = 0.02
    band.rotation_euler = (0, math.radians(90), 0); band.location = (0.4, 0, 0)
    assign(band, mat('band', (0.15, 0.35, 0.70), rough=0.4))
    root = bpy.data.objects.new('root', None); link(root)
    o.parent = root; band.parent = root
    root.location = (0, 0, 0.45); root.rotation_euler = (0, math.radians(-8), math.radians(-28))
    return [o]


def biscuits():
    prof = [(0.0, 0), (1.15, 0), (1.22, 0.06), (1.25, 0.14), (1.2, 0.24), (1.05, 0.29), (0.0, 0.31)]
    o = lathe('cookie', prof, segs=96); displace(o, 0.07, 0.25)
    assign(o, mat('dough', (0.86, 0.62, 0.33), rough=0.7, bump=0.6, bump_scale=18, color2=(0.74, 0.48, 0.22), mix_scale=6, sss=0.1))
    random.seed(11)
    objs = [o]
    choc = mat('choc', (0.24, 0.13, 0.07), rough=0.35)
    for k in range(16):
        a = random.uniform(0, 6.283); r = 0.95 * math.sqrt(random.random())
        c = uvsphere('chip%d' % k, random.uniform(0.08, 0.13), segs=12, rings=8, loc=(r * math.cos(a), r * math.sin(a), 0.29),
                     scale=(1, random.uniform(0.7, 1.2), 0.7))
        displace(c, 0.03, 0.1); assign(c, choc); c.parent = o
    return [o]


def water():
    prof = [(0.0, 0), (0.5, 0), (0.56, 0.05), (0.58, 0.15)]
    for i in range(16):
        z = 0.2 + i * 0.12; prof.append((0.58 - (0.025 if i % 2 else 0), z))
    prof += [(0.58, 2.2), (0.5, 2.5), (0.3, 2.75), (0.2, 2.85), (0.2, 2.95)]
    btl = lathe('btl', prof, cap_top=False)
    s = btl.modifiers.new('s', 'SOLIDIFY'); s.thickness = 0.015
    assign(btl, glass('pet', (0.93, 0.97, 1.0), rough=0.02, ior=1.4))
    w = lathe('water', [(0.0, 0.02), (0.555, 0.02), (0.555, 2.25), (0.0, 2.25)])
    assign(w, glass('h2o', (0.90, 0.96, 1.0), ior=1.33))
    cap = lathe('cap', [(0.0, 2.9), (0.24, 2.9), (0.24, 3.15), (0.0, 3.15)]); assign(cap, mat('capb', (0.10, 0.40, 0.85), rough=0.3, bump=0.3, bump_scale=200))
    lab = text_label('water.png', (1300, 420), (60, 150, 225, 255), [('WATER', 170, 0.45, (255, 255, 255)), ('Spring water 1.5 L', 56, 0.85, (230, 245, 255))])
    loc, rot = facing(0.6, 1.45)
    decal('waterlab', lab, 1.4, 0.5, loc, rot=rot, bend=0.6)
    return [btl, cap]


ITEMS = dict(chicken=chicken, mince=mince, sausages=sausages, fish=fish, bread=bread, tea=tea, sugar=sugar, flour=flour,
             rice=rice, cereal=cereal, coffee=coffee, pasta=pasta, biscuits=biscuits, water=water)
