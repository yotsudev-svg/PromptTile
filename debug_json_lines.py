import json
import re

with open('app/src/main/assets/seed_data.json', 'r', encoding='utf-8') as f:
    content = f.read()

# This is a bit hacky but let's try to find all { ... } blocks that look like word objects
# and check if they have wordEn
matches = re.finditer(r'\{[^{}]*\}', content)

for match in matches:
    block = match.group(0)
    if '"wordJa"' in block and '"wordEn"' not in block:
        # Find line number
        line_no = content.count('\n', 0, match.start()) + 1
        print(f"Potential missing wordEn at line {line_no}: {block}")
    if '"nameJa"' in block and '"valueEn"' not in block and 'items' not in block and 'categories' not in block:
         # Likely a topping item
         if '"id"' not in block: # items don't have id, groups do
            line_no = content.count('\n', 0, match.start()) + 1
            print(f"Potential missing valueEn at line {line_no}: {block}")
