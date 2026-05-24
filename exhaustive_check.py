import json

def check():
    try:
        with open('app/src/main/assets/seed_data.json', 'r', encoding='utf-8') as f:
            data = json.load(f)

        errors = []

        # Check Categories
        for i, cat in enumerate(data.get('categories', [])):
            cat_id = cat.get('id', 'N/A')
            cat_name = cat.get('nameJa', 'unknown')
            path = f'categories[{i}] (id:{cat_id}, name:{cat_name})'

            for field in ['id', 'nameJa', 'nameEn', 'words']:
                if field not in cat:
                    errors.append(f'Missing field "{field}" in {path}')

            words = cat.get('words', [])
            if not isinstance(words, list):
                errors.append(f'Field "words" in {path} is not a list')
                continue

            for j, word in enumerate(words):
                word_path = f'{path}.words[{j}]'
                if not isinstance(word, dict):
                    errors.append(f'Entry in {word_path} is not an object: {word}')
                    continue
                for field in ['wordEn', 'wordJa']:
                    if field not in word:
                        errors.append(f'Missing field "{field}" in {word_path}: {word}')

        # Check Topping Groups
        for i, group in enumerate(data.get('topping_groups', [])):
            group_id = group.get('id', 'N/A')
            group_name = group.get('nameJa', 'unknown')
            path = f'topping_groups[{i}] (id:{group_id}, name:{group_name})'

            for field in ['id', 'nameJa', 'nameEn', 'items']:
                if field not in group:
                    errors.append(f'Missing field "{field}" in {path}')

            items = group.get('items', [])
            if not isinstance(items, list):
                errors.append(f'Field "items" in {path} is not a list')
                continue

            for j, item in enumerate(items):
                item_path = f'{path}.items[{j}]'
                if not isinstance(item, dict):
                    errors.append(f'Entry in {item_path} is not an object: {item}')
                    continue
                for field in ['valueEn', 'nameJa']:
                    if field not in item:
                        errors.append(f'Missing field "{field}" in {item_path}: {item}')

        if errors:
            for err in errors:
                print(err)
        else:
            print('No structural errors found.')

    except json.JSONDecodeError as e:
        print(f'JSON Syntax Error: {e}')
    except Exception as e:
        print(f'Error during validation: {e}')

if __name__ == "__main__":
    check()
