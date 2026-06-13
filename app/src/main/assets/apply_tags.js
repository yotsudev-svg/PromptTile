const fs = require('fs');

// A. カテゴリID -> デフォルトタグ
const categoryTagMap = {
  // 1: 画質・スタイル系 -> タグなし
  1: null, 2: null, 3: null, 4: null,
  // 2: 環境・背景系 -> background
  5: 'background', 6: 'background', 7: 'background', 8: 'background',
  9: 'background', 10: 'background', 11: 'background', 12: 'background',
  // 3: キャラクター系
  13: 'hair',
  14: 'hair',
  15: 'color_accent',
  16: null,
  17: null,
  18: null,
  19: null,
  20: 'skin',
  21: null,
  22: 'creature_parts',
  // 4: ポーズ・動作系 -> タグなし
  23: null, 24: null, 25: null, 26: null, 27: null, 28: null,
  // 5: 服装・装飾系
  29: null,
  30: 'clothing', 31: 'clothing', 32: 'clothing', 33: 'clothing',
  34: 'clothing', 35: 'clothing', 36: 'clothing', 37: 'clothing', 38: 'clothing',
  39: 'accessory', 40: 'accessory', 41: 'accessory', 42: 'accessory',
  43: 'accessory', 44: 'accessory',
  45: 'detail',
  46: 'accessory',
  // 6: 照明・色調系 -> タグなし
  47: null, 48: null,
  // 7: エフェクト・演出系 -> タグなし
  49: null, 50: null, 51: null,
  // 8: 構図・カメラ系 -> タグなし
  52: null, 53: null, 54: null,
  // 10/11: ネガティブ・未分類 -> タグなし
  55: null, 998: null, 999: null,
};

// B. 単語単位の例外（カテゴリ既定を上書き）
const wordOverrides = {
  'eyeshadow': 'color_accent',
  'eyeliner': 'color_accent',
  'shiny lips': 'color_accent',
  'red lips': 'color_accent',
  'black lips': 'color_accent',
  'smeared lipstick': 'color_accent',
};

const data = JSON.parse(fs.readFileSync('seed_data.json', 'utf8'));

for (const category of data.categories) {
  const defaultTag = categoryTagMap[category.id];

  for (const word of category.words) {
    const override = wordOverrides[word.wordEn];

    if (override) {
      word.tags = override;
    } else if (defaultTag) {
      word.tags = defaultTag;
    }
    // defaultTag も override もない場合は "tags" を付与しない
    // （= トッピングシステムが反応しない単語として扱われる）
  }
}

fs.writeFileSync(
  'seed_data.tagged.json',
  JSON.stringify(data, null, 2),
  'utf8'
);

console.log('✅ seed_data.tagged.json を生成しました');