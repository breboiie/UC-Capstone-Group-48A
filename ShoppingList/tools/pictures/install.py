"""Puts rendered pictures into the app: out/<item>.png -> app/src/main/res/drawable-nodpi/photo_<item>.jpg.
Each picture is scaled so the item (not its shadow) fills about 70% of a 320 x 320 square on the app background."""
import os, sys
import numpy as np
from PIL import Image
HERE = os.path.dirname(os.path.abspath(__file__))
DEST = os.path.join(HERE, '..', '..', 'app', 'src', 'main', 'res', 'drawable-nodpi')
BG, N = (242, 241, 236), 320
for name in sys.argv[1:]:
    im = Image.open(os.path.join(HERE, 'out', name + '.png')).convert('RGBA')
    a = np.array(im); ys, xs = np.where(a[..., 3] > 245)
    x0, x1, y0, y1 = xs.min(), xs.max(), ys.min(), ys.max(); bw, bh = x1 - x0 + 1, y1 - y0 + 1
    fit = 262 if max(bw, bh) / min(bw, bh) > 1.6 else 226
    k = fit / max(bw, bh)
    big = im.resize((round(im.width * k), round(im.height * k)), Image.LANCZOS)
    layer = Image.new('RGBA', (N, N), (0, 0, 0, 0))
    layer.paste(big, (round(N / 2 - (x0 + x1) / 2 * k), round(N / 2 - (y0 + y1) / 2 * k)))
    canvas = Image.new('RGBA', (N, N), BG + (255,)); canvas.alpha_composite(layer)
    canvas.convert('RGB').save(os.path.join(DEST, 'photo_%s.jpg' % name), quality=92)
    print('installed', name)
