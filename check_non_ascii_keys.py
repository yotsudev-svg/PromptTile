import json

with open('app/src/main/assets/seed_data.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

for cat in data.get('categories', []):
    for j, word in enumerate(cat.get('words', [])):
        for key in word.keys():
            if not all(ord(c) < 128 for c in key):
                print(f"Non-ASCII key found: '{key}' in {cat.get('nameJa')} words[{j}]")
            if key.strip() != key:
                print(f"Key with whitespace found: '{key}' in {cat.get('nameJa')} words[{j}]")
