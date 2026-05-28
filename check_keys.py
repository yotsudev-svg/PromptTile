import json

with open('app/src/main/assets/seed_data.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

for cat in data.get('categories', []):
    for word in cat.get('words', []):
        for key in word.keys():
            if key not in ['wordEn', 'wordJa', 'toppingGroupIds']:
                print(f"Unexpected key '{key}' in category '{cat.get('nameJa')}': {word}")

for group in data.get('topping_groups', []):
    for item in group.get('items', []):
        for key in item.keys():
            if key not in ['valueEn', 'nameJa', 'colorHex']:
                print(f"Unexpected key '{key}' in topping_group '{group.get('nameJa')}': {item}")
