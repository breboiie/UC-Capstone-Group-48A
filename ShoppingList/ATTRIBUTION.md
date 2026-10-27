# Attribution

Everything bundled in this app that was not written by Team 48, with its licence.
Full licence texts are in the `licenses/` folder.

## Product photographs (26 images)

**Grocery Store Dataset**, Marcus Klasson, Cheng Zhang and Hedvig Kjellström.
MIT License, Copyright (c) 2019 Marcus Klasson. See `licenses/GroceryStoreDataset-MIT.txt`.
Source: https://github.com/marcusklasson/GroceryStoreDataset

Cite as: M. Klasson, C. Zhang and H. Kjellström, "A Hierarchical Grocery Store Image Dataset
with Visual and Semantic Labels", IEEE Winter Conference on Applications of Computer Vision (WACV), 2019.

These are the dataset's **iconic images**: one product photographed on its own on a white
background, so **each picture shows a single item** (one apple, one carton of milk), never a pile.
For this app the white background was replaced with the app's soft background colour and the
product centred on a 320 × 320 square. Packaged items (milk, yoghurt, juice, sour cream) show
Swedish brands, so replacing them with photos of Australian products is recommended before any
evaluation with participants. Keep the same file names. Asparagus shows one tied bunch, which is
how it is sold.

| File in `app/src/main/res/drawable-nodpi/` | Original image in the dataset |
|---|---|
| `photo_apples.jpg` | `dataset/iconic-images-and-descriptions/Fruit/Apple/Pink-Lady/Pink-Lady_Iconic.jpg` |
| `photo_asparagus.jpg` | `dataset/iconic-images-and-descriptions/Vegetables/Asparagus/Asparagus_Iconic.jpg` |
| `photo_avocados.jpg` | `dataset/iconic-images-and-descriptions/Fruit/Avocado/Avocado_Iconic.jpg` |
| `photo_cabbage.jpg` | `dataset/iconic-images-and-descriptions/Vegetables/Cabbage/Cabbage_Iconic.jpg` |
| `photo_capsicum.jpg` | `dataset/iconic-images-and-descriptions/Vegetables/Pepper/Red-Bell-Pepper/Red-Bell-Pepper_Iconic.jpg` |
| `photo_cucumber.jpg` | `dataset/iconic-images-and-descriptions/Vegetables/Cucumber/Cucumber_Iconic.jpg` |
| `photo_eggplant.jpg` | `dataset/iconic-images-and-descriptions/Vegetables/Aubergine/Aubergine_Iconic.jpg` |
| `photo_garlic.jpg` | `dataset/iconic-images-and-descriptions/Vegetables/Garlic/Garlic_Iconic.jpg` |
| `photo_ginger.jpg` | `dataset/iconic-images-and-descriptions/Vegetables/Ginger/Ginger_Iconic.jpg` |
| `photo_grapefruit.jpg` | `dataset/iconic-images-and-descriptions/Fruit/Red-Grapefruit/Red-Grapefruit_Iconic.jpg` |
| `photo_juice.jpg` | `dataset/iconic-images-and-descriptions/Packages/Juice/God-Morgon-Orange-Juice/God-Morgon-Orange-Juice_Iconic.jpg` |
| `photo_leek.jpg` | `dataset/iconic-images-and-descriptions/Vegetables/Leek/Leek_Iconic.jpg` |
| `photo_lemons.jpg` | `dataset/iconic-images-and-descriptions/Fruit/Lemon/Lemon_Iconic.jpg` |
| `photo_mangoes.jpg` | `dataset/iconic-images-and-descriptions/Fruit/Mango/Mango_Iconic.jpg` |
| `photo_milk.jpg` | `dataset/iconic-images-and-descriptions/Packages/Milk/Arla-Standard-Milk/Arla-Standard-Milk_Iconic.jpg` |
| `photo_nectarines.jpg` | `dataset/iconic-images-and-descriptions/Fruit/Nectarine/Nectarine_Iconic.jpg` |
| `photo_oranges.jpg` | `dataset/iconic-images-and-descriptions/Fruit/Orange/Orange_Iconic.jpg` |
| `photo_peaches.jpg` | `dataset/iconic-images-and-descriptions/Fruit/Peach/Peach_Iconic.jpg` |
| `photo_pears.jpg` | `dataset/iconic-images-and-descriptions/Fruit/Pear/Conference/Conference_Iconic.jpg` |
| `photo_pineapple.jpg` | `dataset/iconic-images-and-descriptions/Fruit/Pineapple/Pineapple_Iconic.jpg` |
| `photo_rockmelon.jpg` | `dataset/iconic-images-and-descriptions/Fruit/Melon/Cantaloupe/Cantaloupe_Iconic.jpg` |
| `photo_sour_cream.jpg` | `dataset/iconic-images-and-descriptions/Packages/Sour-Cream/Arla-Sour-Cream/Arla-Sour-Cream_Iconic.jpg` |
| `photo_sweet_potato.jpg` | `dataset/iconic-images-and-descriptions/Vegetables/Potato/Sweet-Potato/Sweet-Potato_Iconic.jpg` |
| `photo_watermelon.jpg` | `dataset/iconic-images-and-descriptions/Fruit/Melon/Watermelon/Watermelon_Iconic.jpg` |
| `photo_yoghurt.jpg` | `dataset/iconic-images-and-descriptions/Packages/Yoghurt/Arla-Natural-Yoghurt/Arla-Natural-Yoghurt_Iconic.jpg` |
| `photo_zucchini.jpg` | `dataset/iconic-images-and-descriptions/Vegetables/Zucchini/Zucchini_Iconic.jpg` |

## Fruit photographs (4 images)

**Fruits-360 dataset**, Mihai Oltean (and Horea Mureșan). Licence: **CC BY-SA 4.0**, see
`licenses/Fruits360-CC-BY-SA-4.0.txt`. Source: https://github.com/fruits-360/fruits-360-original-size

Each is one real fruit or vegetable photographed on white. For this app the white background was
removed, the item centred on the app's background colour and saved at 320 × 320. Under the licence's
share-alike terms these four adapted pictures are also available under CC BY-SA 4.0.

| File in `app/src/main/res/drawable-nodpi/` | Original image |
|---|---|
| `photo_bananas.jpg` | `Training/Banana 3/r0_0.jpg` |
| `photo_mushrooms.jpg` | `Training/Mushroom champignon 5/r0_204.jpg` |
| `photo_onions.jpg` | `Training/Onion White 2/r0_0.jpg` |
| `photo_tomatoes.jpg` | `Training/Tomato 8/r0_100.jpg` |

## Photorealistic 3D pictures (35 images)

Made for this project by Team 48 and free for the team to use and change. Each item was modelled
and rendered in **Blender 4.2** (Cycles, physically based materials, studio lighting) because no
free real photographs of a single item could be found for them. The scripts that make them are in
`tools/pictures/`, so any picture can be changed and made again (see the README there). Packaging
shows plain, made-up labels (for example *TEA, 100 tea bags*), not real brands.

eggs, cheese, butter, cream, carrots, limes, mandarins, potatoes, kiwi_fruit, beetroot, plums, chicken, mince, sausages, fish, bread, tea, sugar, flour, rice, cereal, coffee, pasta, biscuits, water, frozen_peas, ice_cream, toilet_paper, dish_soap, laundry, birthday_card, present, wrapping_paper, candles and the shopping bag shown for typed items (`photo_custom.jpg`).

## Icons (54 vector drawables)

**Material Symbols** by Google, Apache License 2.0. See `licenses/MaterialSymbols-Apache-2.0.txt`.
Source: https://github.com/google/material-design-icons
Converted from SVG to Android vector drawables; shapes unchanged. Used only on buttons and in
menus. Shopping items always show a picture, never an icon.

## Typeface

**Atkinson Hyperlegible**, Copyright 2020 Braille Institute of America, Inc.
SIL Open Font License 1.1. See `licenses/AtkinsonHyperlegible-OFL.txt`.
Designed for readers with low vision, which is why it was chosen for this app.

## App logo

The **My List** logo (shopping bag with a tick) was supplied by Team 48. It is used as the welcome
screen picture (`drawable-nodpi/app_logo.webp`) and, cropped to the bag without the words, as the
launcher icon (`mipmap-*/ic_launcher_foreground.png`). The single-colour themed icon and the
notification icon are simple shapes drawn by the team.
