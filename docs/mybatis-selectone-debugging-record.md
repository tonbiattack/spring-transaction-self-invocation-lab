# E005 デバッグ記録：最新1件の selectOne が複数行で失敗する

## 症状

顧客の最新注文を返す mapper は `ORDER BY created_at DESC` を持つが、同じ顧客に2件の注文があると `TooManyResultsException` が発生した。

## 観測事実

```text
Expected one result (or null) to be returned by selectOne(), but found: 2
Tests run: 1, Failures: 0, Errors: 1, Skipped: 0
BUILD FAILURE
```

`ORDER BY` は行の順番を決めるが、行数を1件へ制限しない。Java mapper の戻り値型が単一オブジェクトであるため、MyBatis は `selectOne` として実行し、複数行を許容しない。

## 仮説の比較

| 仮説 | 予測 | 結果 | 判定 |
| --- | --- | --- | --- |
| SQLの並び順が無効 | 最新行が先頭にならない | 2件とも返され、順序だけは正しい | 棄却 |
| mapper が selectList で実行される | 複数行がリストとして返る | `selectOne` の例外が発生 | 棄却 |
| 最新1件のSQL契約が不足 | 2件以上を1件へ制限していない | `LIMIT 1` 追加後に成功 | 採用 |

## 最小修正

`ORDER BY created_at DESC` の後に `LIMIT 1` を追加し、最新1件というサービス契約をSQLへ明示した。結果列は camelCase プロパティへ明示的にエイリアスし、E006の設定問題と分離した。

## 回帰結果

対象テスト1件、全テスト8件が成功した。
