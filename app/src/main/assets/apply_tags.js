const fs = require('fs');

/**
 * A. カテゴリID -> デフォルトタグ
 * ID 13 (髪の色) には新設された 'hair_color' タグを、
 * ID 14 (髪型) には 'hair' タグを付与するように修正しました。
 * また、廃止されたタグ (creature_parts, detail) をマッピングから削除しました。
 */
const categoryTagMap = {
  // 1: 画質・スタイル系 -> タグなし
  1: null, 2: null, 3: null, 4: null,
  // 2: 環境・背景系 -> background
  5: 'background', 6: 'background', 7: 'background', 8: 'background',
  9: 'background', 10: 'background', 11: 'background', 12: 'background',
  // 3: キャラクター系
  13: 'hair_color', // 髪の色
  14: 'hair',       // 髪型
  15: 'color_accent',
  16: null,
  17: null,
  18: null,
  19: null,
  20: 'skin',
  21: null,
  22: null, // creature_parts 廃止
  // 4: ポーズ・動作系 -> タグなし
  23: null, 24: null, 25: null, 26: null, 27: null, 28: null,
  // 5: 服装・装飾系
  29: null,
  30: 'clothing', 31: 'clothing', 32: 'clothing', 33: 'clothing',
  34: 'clothing', 35: 'clothing', 36: 'clothing', 37: 'clothing', 38: 'clothing',
  39: 'accessory', 40: 'accessory', 41: 'accessory', 42: 'accessory',
  43: 'accessory', 44: 'accessory',
  45: null, // detail 廃止
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

// 入力ファイルパス（必要に応じて修正してください）
const inputPath = 'seed_data.json';
const outputPath = 'seed_data.tagged.json';

if (!fs.existsSync(inputPath)) {
  console.error(`❌ エラー: ${inputPath} が見つかりません。`);
  process.exit(1);
}

const data = JSON.parse(fs.readFileSync(inputPath, 'utf8'));

console.log('🚀 タグの適用を開始します...');

for (const category of data.categories) {
  const defaultTag = categoryTagMap[category.id];

  for (const word of category.words) {
    const override = wordOverrides[word.wordEn];

    if (override) {
      word.tags = override;
    } else if (defaultTag) {
      word.tags = defaultTag;
    } else {
      // タグが不要な場合はプロパティを削除（クリーンアップ）
      delete word.tags;
    }
  }
}

fs.writeFileSync(
  outputPath,
  JSON.stringify(data, null, 2),
  'utf8'
);

console.log(`✅ ${outputPath} を正常に生成しました。`);