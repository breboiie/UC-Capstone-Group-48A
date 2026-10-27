"""Usage: python3 run.py item [item ...]  -- renders each item to out/<item>.png"""
import sys, os, time
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import studio
import items_a
ITEMS = dict(items_a.ITEMS)
try:
    import items_b; ITEMS.update(items_b.ITEMS)
except ImportError:
    pass
try:
    import items_c; ITEMS.update(items_c.ITEMS)
except ImportError:
    pass

CAM = {  # per-item camera tweaks
    'cream': dict(elev=14), 'water': dict(elev=12), 'dish_soap': dict(elev=12), 'coffee': dict(elev=16),
    'candles': dict(elev=14), 'toilet_paper': dict(elev=22), 'birthday_card': dict(elev=16, azim=-20),
    'kiwi_fruit': dict(elev=38), 'mince': dict(elev=40), 'fish': dict(elev=40), 'biscuits': dict(elev=42),
}

for name in sys.argv[1:]:
    t = time.time()
    studio.reset()
    objs = ITEMS[name]()
    studio.studio(objs, **CAM.get(name, {}))
    studio.render(name)
    print('rendered', name, '%.0fs' % (time.time() - t), flush=True)
