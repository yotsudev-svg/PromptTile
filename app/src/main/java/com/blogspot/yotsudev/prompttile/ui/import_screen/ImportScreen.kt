package com.blogspot.yotsudev.prompttile.ui.import_screen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blogspot.yotsudev.prompttile.data.importer.ImportCategory
import com.blogspot.yotsudev.prompttile.ui.components.PromptTileTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    onBack: () -> Unit,
    viewModel: ImportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // インポート完了 → 自動で前画面に戻る
    LaunchedEffect(uiState.importDone) {
        if (uiState.importDone) onBack()
    }

    Scaffold(
        topBar = {
            PromptTileTopAppBar(
                title = "JSONインポート",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ---- セクション1: 入力エリア ----
            item {
                InputSection(
                    jsonInput  = uiState.jsonInput,
                    parseError = uiState.parseError,
                    onInput    = viewModel::onJsonInput,
                    onClear    = viewModel::reset,
                )
            }

            // ---- セクション2: プレビュー ----
            if (uiState.preview.isNotEmpty()) {
                item {
                    Text(
                        text  = "プレビュー（${uiState.preview.size}カテゴリ、${uiState.preview.sumOf { it.words.size }}単語）",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                items(uiState.preview) { category ->
                    PreviewCategoryCard(category)
                }
                item {
                    ImportButton(
                        isImporting = uiState.isImporting,
                        onClick     = viewModel::executeImport,
                    )
                }
            }
        }
    }
}

// ---- 入力エリア --------------------------------------------------------

@Composable
private fun InputSection(
    jsonInput: String,
    parseError: String?,
    onInput: (String) -> Unit,
    onClear: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // コピー対象となる、AIへの具体的なプロンプト文章（「」の内側）
    val promptTemplate = """
        以下の条件に従って、「〇〇」に関するプロンプト単語をいくつか、指定のJSON形式で出力してください。

        【親カテゴリ一覧】
        作成するカテゴリの内容に応じて、最も適切な親カテゴリのID番号を「parentId」に設定してください。
        1: 画質・スタイル (Quality & Style)
        2: 照明・色調 (Lighting & Color)
        3: キャラクター (Character)
        4: 顔・髪 (Face & Hair)
        5: 服装 (Clothing)
        6: 環境・構図 (Environment & Composition)
        7: エフェクト (Effects)
        8: 画面演出 (Post Processing)
        9: 未分類 (Others & Uncategorized)

        【出力フォーマット】
        {
          "categories": [
            {
              "nameJa": "子カテゴリ名(日本語)",
              "nameEn": "Subcategory Name(英語)",
              "parentId": 適切な親カテゴリのID番号,
              "words": [
                { "wordEn": "english_word", "wordJa": "日本語の意味" }
              ]
            }
          ]
        }
    """.trimIndent()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text  = "GeminiなどのAIが生成したJSONをここに貼り付けてください",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value         = jsonInput,
            onValueChange = onInput,
            modifier      = Modifier
                .fillMaxWidth()
                .height(200.dp),
            placeholder = {
                Text(
                    text  = "{ \"categories\": [ ... ] }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            },
            isError       = parseError != null,
            supportingText = parseError?.let {
                { Text(it, color = MaterialTheme.colorScheme.error) }
            },
        )
        // 正常にパースできた場合の成功表示
        AnimatedVisibility(visible = parseError == null && jsonInput.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text     = "  JSONを認識しました",
                    style    = MaterialTheme.typography.labelMedium,
                    color    = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (jsonInput.isNotBlank()) {
            TextButton(onClick = onClear) {
                Text("クリア")
            }
        }

        // AIへの指示テンプレートヒント（ここをコピー可能なカード型に変更）
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Text(
            text = "💡 AIへの指示例（タップしてコピー）",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    // クリップボードにコピーを実行
                    clipboardManager.setText(AnnotatedString(promptTemplate))
                    // ユーザーへのフィードバックトースト
                    Toast.makeText(context, "指示文をコピーしました。AIに貼り付けてください！", Toast.LENGTH_SHORT).show()
                }
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = promptTemplate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "コピー",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ---- プレビューカード --------------------------------------------------

@Composable
private fun PreviewCategoryCard(category: ImportCategory) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    Text(
                        text  = category.nameJa,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text  = category.nameEn,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
                Text(
                    text  = "${category.words.size}単語",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            // 単語を最大5件プレビュー表示
            category.words.take(5).forEach { word ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text  = word.wordEn,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text  = word.wordJa.ifBlank { "－" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (category.words.size > 5) {
                Text(
                    text  = "… 他 ${category.words.size - 5}件",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
}

// ---- インポートボタン --------------------------------------------------

@Composable
private fun ImportButton(isImporting: Boolean, onClick: () -> Unit) {
    Box(
        modifier        = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        if (isImporting) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick  = onClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("インポートを実行する")
            }
        }
    }
}