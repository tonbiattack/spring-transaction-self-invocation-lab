# E005 題材メモ：最新1件の selectOne が複数行で失敗する

## 選定理由

MyBatis の `selectOne` は、結果が0件なら null、1件ならそのオブジェクトを返すが、2件以上なら例外になる。公式 Java API も `selectOne` は exactly one object or null を要求し、複数結果では例外になると説明している。

今回のサービス契約は「顧客の最新注文を1件返す」。SQL に `ORDER BY created_at DESC` はあるが、`LIMIT 1` がないため、履歴が2件あるだけで `TooManyResultsException` が発生する。順序付けは最初の行だけを返す指定ではないことを観測する。

## 既存題材との差分

E001 は Spring プロキシ、E002 はチェック例外、E003 は MyBatis セッションキャッシュ、E004 は動的 SQL の空リストを扱う。E005 は `selectOne` の戻り値契約と、最新1件という SQL のカーディナリティを扱う。

## 失敗観測

```text
Expected one result (or null) to be returned by selectOne(), but found: 2
Tests run: 1, Failures: 0, Errors: 1, Skipped: 0
BUILD FAILURE
```
