import json

def find_all_objects(obj):
    if isinstance(obj, dict):
        yield obj
        for value in obj.values():
            yield from find_all_objects(value)
    elif isinstance(obj, list):
        for item in obj:
            yield from find_all_objects(item)

with open('app/src/main/assets/seed_data.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

for obj in find_all_objects(data):
    if 'wordJa' in obj and 'wordEn' not in obj:
        print(f"Object with wordJa but NO wordEn: {obj}")
    if 'nameJa' in obj and 'nameEn' not in obj and 'id' in obj:
        # Check categories or groups
        if 'words' in obj or 'items' in obj:
             print(f"Object with nameJa but NO nameEn (and has id): {obj}")
