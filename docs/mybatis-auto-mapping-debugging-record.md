# E006 デバッグ記録：snake_case 列が camelCase プロパティへ自動マッピングされない

## 症状

`inventory_items.item_id` と `inventory_items.display_name` を、`InventoryItem.itemId` と `displayName` へ `resultType` だけで読み込もうとした。しかし mapper は null を返し、テストの getter 呼び出しで `NullPointerException` になった。

## 観測事実

```text
Cannot invoke "InventoryItem.getItemId()" because "item" is null
Tests run: 1, Failures: 0, Errors: 1, Skipped: 0
BUILD FAILURE
```

MyBatis の `mapUnderscoreToCamelCase` の既定値は `false` である。`item_id` と `itemId` は同じ名前として扱われないため、自動マッピングでは結果行を Java プロパティへ結び付けられない。

## 仮説の比較

| 仮説 | 予測 | 結果 | 判定 |
| --- | --- | --- | --- |
| SQLのWHERE条件が一致しない | 結果行が0件になる | SQL条件は一致するが戻り値が null | 棄却 |
| H2が列名を返さない | エイリアスを付けても null のまま | エイリアス追加後は値が設定された | 棄却 |
| snake_case と camelCase の自動変換が無効 | 列名とプロパティ名を明示すれば成功する | `AS itemId`, `AS displayName` で成功 | 採用 |

## 最小修正

SQL列に Java プロパティ名のエイリアスを指定した。

```diff
-SELECT item_id, display_name
+SELECT item_id AS itemId, display_name AS displayName
```

グローバル設定を `mapUnderscoreToCamelCase=true` にする方法もあるが、今回の修正では mapper 単位で契約を明示できる列エイリアスを採用した。

## 回帰結果

対象テスト1件、全テスト8件が成功した。E005の mapper でも同じ理由で明示的な列エイリアスを使用し、題材間の設定依存を避けた。
