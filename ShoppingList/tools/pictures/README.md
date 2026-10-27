# Item pictures

The photorealistic 3D pictures in the app (bread, eggs, cheese, meat, packets, household items,
cards and gifts, and a few fruit and vegetables) are made by these scripts with Blender.

## Make or change a picture

```
pip install bpy==4.2.0 pillow numpy      # Blender as a Python module (needs Python 3.11)
python3 run.py bread eggs                # renders out/bread.png and out/eggs.png
python3 install.py bread eggs            # puts them into the app as photo_bread.jpg, photo_eggs.jpg
```

Quick drafts: `RES=360 SAMPLES=32 python3 run.py bread`. Final quality used for the app:
`RES=560 SAMPLES=72` (about 1.5 minutes per picture on two CPU cores).

- `studio.py`: lights, camera framing, shadow, materials and helpers shared by every item.
- `items_a.py`: eggs, dairy, fruit and vegetables. `items_b.py`: meat, bakery, pantry, drinks.
  `items_c.py`: frozen, household, cards and gifts, shopping bag.
- Each item is one function returning its objects; labels are drawn with Pillow in the app's font.

To use a real photo instead, replace `app/src/main/res/drawable-nodpi/photo_<item>.jpg` with a
square JPEG of the same name.
