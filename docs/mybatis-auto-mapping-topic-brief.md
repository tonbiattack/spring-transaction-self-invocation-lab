# E006 題材メモ：snake_case 列が camelCase プロパティへ自動マッピングされない

## 選定理由

MyBatis の `mapUnderscoreToCamelCase` は、`A_COLUMN` を `aColumn` へ自動マッピングする設定であり、既定値は `false` である。既定のまま `resultType` だけを指定し、SQL列名 `item_id` と Java プロパティ `itemId` を対応させようとすると、値が設定されず mapper は null を返す。

今回のテストは `inventory_items` の `item_id` と `display_name` を `InventoryItem.itemId` と `displayName` に読み込む契約を表現する。バグ状態では明示的な resultMap も列エイリアスもないため、結果オブジェクトが null になり、テストが NullPointerException になる。

## 既存題材との差分

E001〜E005 は、トランザクション境界、rollback rule、ローカルキャッシュ、動的SQL、selectOne の件数契約を扱う。E006 は SQL 列名と Java プロパティ名の変換規則、および MyBatis 設定の既定値を扱う。

## 失敗観測

```text
InventoryItemServiceIntegrationTest... NullPointerException
Cannot invoke "InventoryItem.getItemId()" because "item" is null
Tests run: 1, Failures: 0, Errors: 1, Skipped: 0
BUILD FAILURE
```
