import json
import sys

try:
    with open('app/src/main/assets/seed_data.json', 'r', encoding='utf-8') as f:
        data = json.load(f)

    # Check categories
    if 'categories' in data:
        for i, cat in enumerate(data['categories']):
            cat_name = cat.get('nameJa', f'Category index {i}')
            if 'words' in cat:
                for j, word in enumerate(cat['words']):
                    if 'wordEn' not in word:
                        print(f'ERROR: Missing wordEn in category \"{cat_name}\", word index {j}: {word}')
                    if 'wordJa' not in word:
                        print(f'ERROR: Missing wordJa in category \"{cat_name}\", word index {j}: {word}')
            else:
                print(f'ERROR: Missing words array in category \"{cat_name}\"')

    # Check topping_groups
    if 'topping_groups' in data:
        for i, group in enumerate(data['topping_groups']):
            group_name = group.get('nameJa', f'Group index {i}')
            if 'items' in group:
                for j, item in enumerate(group['items']):
                    if 'valueEn' not in item:
                        print(f'ERROR: Missing valueEn in topping_group \"{group_name}\", item index {j}: {item}')
                    if 'nameJa' not in item:
                        print(f'ERROR: Missing nameJa in topping_group \"{group_name}\", item index {j}: {item}')
            else:
                 print(f'ERROR: Missing items array in topping_group \"{group_name}\"')

except json.JSONDecodeError as e:
    print(f'JSON Syntax Error: {e}')
except Exception as e:
    print(f'Error: {e}')
