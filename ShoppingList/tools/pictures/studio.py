"""Shared studio for photoreal item pictures: soft lights, shadow catcher, auto-framed camera."""
import bpy, bmesh, math, os, sys
from mathutils import Vector, Matrix

HERE = os.path.dirname(os.path.abspath(__file__))
FONT = os.path.join(HERE, '..', '..', 'app', 'src', 'main', 'res', 'font', 'atkinson_bold.ttf')
OUT = os.path.dirname(os.path.abspath(__file__)) + '/out/'
TEX = os.path.dirname(os.path.abspath(__file__)) + '/tex/'
os.makedirs(OUT, exist_ok=True); os.makedirs(TEX, exist_ok=True)


def reset():
    bpy.ops.wm.read_factory_settings(use_empty=True)
    sc = bpy.context.scene
    sc.render.engine = 'CYCLES'
    sc.cycles.device = 'CPU'
    sc.cycles.samples = int(os.environ.get('SAMPLES', '96'))
    sc.cycles.use_denoising = True
    try:
        sc.cycles.denoiser = 'OPENIMAGEDENOISE'
    except Exception:
        pass
    sc.cycles.max_bounces = 8
    sc.cycles.transmission_bounces = 12
    sc.render.resolution_x = sc.render.resolution_y = int(os.environ.get('RES', '640'))
    sc.render.film_transparent = True
    sc.view_settings.view_transform = 'AgX'
    sc.view_settings.exposure = float(os.environ.get('EXPOSURE', '0.3'))
    try:
        sc.view_settings.look = 'AgX - Medium High Contrast'
    except Exception:
        pass
    w = bpy.data.worlds.new('w'); sc.world = w; w.use_nodes = True
    nt = w.node_tree; bg = nt.nodes['Background']
    bg.inputs[0].default_value = (0.89, 0.88, 0.84, 1)   # the app background, so glass shows it through
    lp = nt.nodes.new('ShaderNodeLightPath'); mx = nt.nodes.new('ShaderNodeMix'); mx.data_type = 'FLOAT'
    mx.inputs['A'].default_value = 0.85; mx.inputs['B'].default_value = 0.3      # diffuse light is soft
    nt.links.new(lp.outputs['Is Diffuse Ray'], mx.inputs['Factor']); nt.links.new(mx.outputs['Result'], bg.inputs['Strength'])
    return sc


# ---------------------------------------------------------------- materials

def mat(name, color, rough=0.5, metal=0.0, sss=0.0, sss_radius=(1, 0.5, 0.3), trans=0.0, ior=1.45,
        bump=None, bump_scale=40.0, bump_detail=8.0, alpha=1.0, coat=0.0, emission=None, emit=0.0, spec=0.5,
        color2=None, mix_scale=6.0, image=None):
    m = bpy.data.materials.new(name); m.use_nodes = True
    nt = m.node_tree; p = nt.nodes['Principled BSDF']
    lin = lambda c: tuple(x ** 2.2 for x in c)  # colours are given as on screen (sRGB)
    color = lin(color)
    if color2 is not None: color2 = lin(color2)
    if emission: emission = lin(emission)
    p.inputs['Base Color'].default_value = (*color, 1)
    p.inputs['Roughness'].default_value = rough
    p.inputs['Metallic'].default_value = metal
    p.inputs['IOR'].default_value = ior
    p.inputs['Transmission Weight'].default_value = trans
    p.inputs['Coat Weight'].default_value = coat
    p.inputs['Specular IOR Level'].default_value = spec
    if sss:
        p.inputs['Subsurface Weight'].default_value = sss
        p.inputs['Subsurface Radius'].default_value = sss_radius
        p.inputs['Subsurface Scale'].default_value = 0.05
    if alpha < 1:
        p.inputs['Alpha'].default_value = alpha
        m.blend_method = 'BLEND'
    if emission:
        p.inputs['Emission Color'].default_value = (*emission, 1)
        p.inputs['Emission Strength'].default_value = emit
    if image:
        tex = nt.nodes.new('ShaderNodeTexImage'); tex.image = bpy.data.images.load(image)
        tex.extension = 'CLIP'
        nt.links.new(tex.outputs['Color'], p.inputs['Base Color'])
        nt.links.new(tex.outputs['Alpha'], p.inputs['Alpha'])
        m.blend_method = 'HASHED'
    elif color2 is not None:
        n = nt.nodes.new('ShaderNodeTexNoise'); n.inputs['Scale'].default_value = mix_scale; n.inputs['Detail'].default_value = 6
        ramp = nt.nodes.new('ShaderNodeValToRGB')
        ramp.color_ramp.elements[0].color = (*color, 1); ramp.color_ramp.elements[1].color = (*color2, 1)
        ramp.color_ramp.elements[0].position = 0.35; ramp.color_ramp.elements[1].position = 0.65
        nt.links.new(n.outputs['Fac'], ramp.inputs['Fac']); nt.links.new(ramp.outputs['Color'], p.inputs['Base Color'])
    if bump:
        n = nt.nodes.new('ShaderNodeTexNoise'); n.inputs['Scale'].default_value = bump_scale; n.inputs['Detail'].default_value = bump_detail
        b = nt.nodes.new('ShaderNodeBump'); b.inputs['Strength'].default_value = bump; b.inputs['Distance'].default_value = 0.02
        nt.links.new(n.outputs['Fac'], b.inputs['Height']); nt.links.new(b.outputs['Normal'], p.inputs['Normal'])
    return m


def glass(name, color=(1, 1, 1), rough=0.03, ior=1.45):
    return mat(name, color, rough=rough, trans=1.0, ior=ior)


def assign(obj, m):
    obj.data.materials.clear(); obj.data.materials.append(m)
    return obj


# ---------------------------------------------------------------- geometry helpers

def smooth(obj, level=2, shade=True):
    if level:
        md = obj.modifiers.new('sub', 'SUBSURF'); md.levels = level; md.render_levels = level
    if shade:
        for p in obj.data.polygons:
            p.use_smooth = True
    return obj


def bevel(obj, width=0.02, segs=4):
    md = obj.modifiers.new('bev', 'BEVEL'); md.width = width; md.segments = segs; md.limit_method = 'ANGLE'
    for p in obj.data.polygons:
        p.use_smooth = True
    try:
        obj.data.use_auto_smooth = True
    except Exception:
        pass
    return obj


def link(obj):
    bpy.context.scene.collection.objects.link(obj)
    return obj


def box(name, sx, sy, sz, loc=(0, 0, 0)):
    me = bpy.data.meshes.new(name); bm = bmesh.new()
    bmesh.ops.create_cube(bm, size=1.0)
    for v in bm.verts:
        v.co = Vector((v.co.x * sx, v.co.y * sy, v.co.z * sz))
    bm.to_mesh(me); bm.free()
    o = bpy.data.objects.new(name, me); o.location = loc
    return link(o)


def uvsphere(name, r=1.0, segs=64, rings=32, loc=(0, 0, 0), scale=(1, 1, 1)):
    me = bpy.data.meshes.new(name); bm = bmesh.new()
    bmesh.ops.create_uvsphere(bm, u_segments=segs, v_segments=rings, radius=r)
    for v in bm.verts:
        v.co = Vector((v.co.x * scale[0], v.co.y * scale[1], v.co.z * scale[2]))
    bm.to_mesh(me); bm.free()
    o = bpy.data.objects.new(name, me); o.location = loc
    for p in me.polygons:
        p.use_smooth = True
    return link(o)


def cyl(name, r, h, segs=64, loc=(0, 0, 0)):
    me = bpy.data.meshes.new(name); bm = bmesh.new()
    bmesh.ops.create_cone(bm, cap_ends=True, segments=segs, radius1=r, radius2=r, depth=h)
    bm.to_mesh(me); bm.free()
    o = bpy.data.objects.new(name, me); o.location = loc
    return link(o)


def lathe(name, profile, segs=96, loc=(0, 0, 0), cap_bottom=True, cap_top=True):
    """Revolve a list of (radius, z) points around Z. Radius 0 points close the shape."""
    me = bpy.data.meshes.new(name); bm = bmesh.new()
    rings = []
    for (r, z) in profile:
        ring = []
        for i in range(segs):
            a = 2 * math.pi * i / segs
            ring.append(bm.verts.new((r * math.cos(a), r * math.sin(a), z)))
        rings.append(ring)
    for k in range(len(rings) - 1):
        for i in range(segs):
            j = (i + 1) % segs
            bm.faces.new((rings[k][i], rings[k][j], rings[k + 1][j], rings[k + 1][i]))
    if cap_bottom and profile[0][0] > 0:
        bm.faces.new(list(reversed(rings[0])))
    if cap_top and profile[-1][0] > 0:
        bm.faces.new(rings[-1])
    bmesh.ops.remove_doubles(bm, verts=bm.verts, dist=1e-5)
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
    bm.to_mesh(me); bm.free()
    o = bpy.data.objects.new(name, me); o.location = loc
    for p in me.polygons:
        p.use_smooth = True
    return link(o)


def displace(obj, strength=0.05, scale=1.0, kind='CLOUDS', depth=2, mid=0.5):
    t = bpy.data.textures.new(obj.name + 'dt', kind)
    try:
        t.noise_scale = scale; t.noise_depth = depth
    except Exception:
        pass
    md = obj.modifiers.new('disp', 'DISPLACE'); md.texture = t; md.strength = strength; md.mid_level = mid
    return obj


def decal(name, image, w, h, loc, rot=(math.pi / 2, 0, 0), bend=0.0):
    """A flat printed label: a plane carrying an RGBA image, placed just off a surface."""
    me = bpy.data.meshes.new(name); bm = bmesh.new()
    nx, ny = (24 if bend else 1), 1
    verts = []
    for j in range(ny + 1):
        row = []
        for i in range(nx + 1):
            u, v = i / nx, j / ny
            x = (u - 0.5) * w; y = (v - 0.5) * h; z = 0.0
            if bend:
                ang = x / bend; x = bend * math.sin(ang); z = bend * (math.cos(ang) - 1)
            row.append((bm.verts.new((x, y, z)), (u, v)))
        verts.append(row)
    uv = bm.loops.layers.uv.new()
    for j in range(ny):
        for i in range(nx):
            q = [verts[j][i], verts[j][i + 1], verts[j + 1][i + 1], verts[j + 1][i]]
            f = bm.faces.new([a for a, _ in q])
            for loop, (_, c) in zip(f.loops, q):
                loop[uv].uv = c
    bm.to_mesh(me); bm.free()
    o = bpy.data.objects.new(name, me); o.location = loc; o.rotation_euler = rot
    for p in me.polygons:
        p.use_smooth = True
    link(o)
    return assign(o, mat(name + 'm', (1, 1, 1), rough=0.45, image=image))


def text_label(path, size, bg, lines, fg=(255, 255, 255), band=None, art=None, radius=0):
    """Draws a printed label with PIL: background colour, optional band, centred lines of text."""
    from PIL import Image, ImageDraw, ImageFont
    W, H = size
    im = Image.new('RGBA', (W, H), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    d.rounded_rectangle([0, 0, W - 1, H - 1], radius, fill=bg)
    if band:
        y0, y1, col = band; d.rectangle([0, int(H * y0), W, int(H * y1)], fill=col)
    if art:
        art(im, d)
    for (txt, fs, y, col) in lines:
        f = ImageFont.truetype(FONT, fs)
        tw = d.textlength(txt, font=f)
        d.text(((W - tw) / 2, int(H * y) - fs // 2), txt, font=f, fill=col if col else fg)
    p = TEX + path; im.save(p)
    return p


# ---------------------------------------------------------------- lights, ground, camera, render

def studio(target_objs, elev=24, azim=-35, lens=70, margin=1.18, ground=True, key=1.0, look_down=0.0):
    sc = bpy.context.scene
    # bounds
    pts = []
    bpy.context.view_layer.update()
    allobjs = []
    for o in target_objs:
        allobjs += [o] + [c for c in o.children_recursive if c.type == 'MESH' and not c.hide_render]
    for o in allobjs:
        if o.type != 'MESH':
            continue
        dg = bpy.context.evaluated_depsgraph_get(); eo = o.evaluated_get(dg)
        for c in eo.bound_box:
            pts.append(eo.matrix_world @ Vector(c))
    mn = Vector((min(p.x for p in pts), min(p.y for p in pts), min(p.z for p in pts)))
    mx = Vector((max(p.x for p in pts), max(p.y for p in pts), max(p.z for p in pts)))
    centre = (mn + mx) / 2; size = (mx - mn).length
    if ground:
        g = box('ground', 60, 60, 0.01, (0, 0, mn.z - 0.005)); g.is_shadow_catcher = True
        assign(g, mat('g', (0.9, 0.9, 0.9), rough=0.9))
    # lights: big soft key top-left, fill right, rim behind
    def area(name, loc, energy, s):
        L = bpy.data.lights.new(name, 'AREA'); L.energy = energy * key; L.size = s
        o = bpy.data.objects.new(name, L); o.location = loc
        d = centre - Vector(loc); o.rotation_euler = d.to_track_quat('-Z', 'Y').to_euler(); link(o)
    r = size * 2.2
    area('key', centre + Vector((-r * 0.8, -r * 0.9, r * 1.2)), 150 * size ** 2, size * 2.0)
    area('fill', centre + Vector((r * 1.1, -r * 0.6, r * 0.4)), 22 * size ** 2, size * 2.5)
    area('rim', centre + Vector((r * 0.3, r * 1.2, r * 0.9)), 60 * size ** 2, size * 1.5)
    area('top', centre + Vector((0, 0, r * 1.5)), 35 * size ** 2, size * 3)
    # camera on a sphere around the centre, distance chosen so the object fills the frame
    cam = bpy.data.cameras.new('cam'); cam.lens = lens; cam.sensor_width = 36
    co = bpy.data.objects.new('cam', cam); link(co); sc.camera = co
    e, a = math.radians(elev), math.radians(azim)
    dirv = Vector((math.sin(a) * math.cos(e), -math.cos(a) * math.cos(e), math.sin(e)))
    aim = centre + Vector((0, 0, -look_down * size))
    fov = 2 * math.atan(18 / lens)
    # fit using projected bounds
    dist = size
    for _ in range(25):
        co.location = aim + dirv * dist
        co.rotation_euler = (aim - co.location).to_track_quat('-Z', 'Y').to_euler()
        bpy.context.view_layer.update()
        mi = co.matrix_world.inverted()
        ext = 0
        for p in pts:
            q = mi @ p
            if q.z < 0:
                ext = max(ext, abs(q.x / -q.z), abs(q.y / -q.z))
        want = math.tan(fov / 2) / margin
        dist *= ext / want
    co.location = aim + dirv * dist
    return co


def render(name):
    sc = bpy.context.scene
    sc.render.filepath = OUT + name + '.png'
    sc.render.image_settings.file_format = 'PNG'; sc.render.image_settings.color_mode = 'RGBA'
    bpy.ops.render.render(write_still=True)


def facing(r, z, azim=-35):
    """Location and rotation for a label wrapped on a round item, facing the default camera."""
    a = math.radians(azim)
    return (r * math.sin(a), -r * math.cos(a), z), (math.pi / 2, 0, a)
