# E004 題材メモ：空リストで MyBatis の動的 IN 句が不完全になる

## 選定理由

MyBatis の `<foreach>` はコレクションから `IN` 条件の括弧・区切り・プレースホルダーを組み立てられる。一方で、コレクションが空の場合はループ本体も `open` / `close` も出力されない。`WHERE product_id IN` のように `<foreach>` の外側に `IN` を置くと、空入力時だけ不完全な SQL になる。

今回の最小再現では、「選択した商品IDが空なら全件ではなく0件を返す」というサービス契約を設定する。バグ状態では H2 が `WHERE product_id IN` の構文エラーを返し、空選択が正常な業務入力として扱われない。

## 既存題材との差分

E001 は Spring プロキシ、E002 はチェック例外の rollback rule、E003 は MyBatis の SESSION ローカルキャッシュを扱う。E004 は mapper XML の動的 SQL が入力コレクションの境界条件でどの SQL テキストを生成するかを扱う。トランザクションやキャッシュではなく、`<foreach>` の展開結果と業務上の「空選択」契約が中心である。

## 固定した環境

| 項目 | 値 |
| --- | --- |
| JDK | 21.0.11 |
| Maven | 3.8.7 |
| Spring Boot | 3.5.0 |
| MyBatis Spring Boot Starter | 3.0.4 |
| データベース | H2 インメモリ |

## 失敗テストの要約

```text
### SQL: SELECT product_id, status FROM products WHERE product_id IN
### Cause: org.h2.jdbc.JdbcSQLSyntaxErrorException: expected "("
Tests run: 2, Failures: 0, Errors: 1, Skipped: 0
BUILD FAILURE
```
