# E003 題材メモ：MyBatis の SESSION ローカルキャッシュが同じ参照を返す

## 選定理由

MyBatis の `localCacheScope` は既定値が `SESSION` であり、セッション中の繰り返しクエリをローカルキャッシュする。さらに、SESSION スコープではキャッシュされた同じオブジェクト参照が返される。このため、DBを書き換えていない表示用のオブジェクト変更が、同一トランザクション内の再検索結果に見えてしまう。

今回の再現では、Spring の `@Transactional(readOnly = true)` メソッド内で MyBatis mapper を2回呼ぶ。1回目の結果を表示用に `DISPLAY_ONLY` へ変更しただけなのに、2回目の取得結果まで `DISPLAY_ONLY` になり、DB上の `ACTIVE` と異なる。

## 既存題材との差分

E001 は Spring のプロキシ境界、E002 はチェック例外の rollback rule を扱う。E003 はトランザクションの開始・ロールバックではなく、MyBatis のセッション内キャッシュスコープとオブジェクト同一性を扱う。ログで `transactionActive=true` と `sameReference=true` を確認し、`localCacheScope=STATEMENT` への変更で挙動を比較する。

## 固定した環境

| 項目 | 値 |
| --- | --- |
| JDK | 21.0.11 |
| Maven | 3.8.7 |
| Spring Boot | 3.5.0 |
| MyBatis Spring Boot Starter | 3.0.4 |
| データベース | H2 インメモリ |
| localCacheScope（バグ状態） | SESSION |

## 失敗テストの要約

```text
mybatisLocalCache transactionActive=true, sameReference=true, firstStatus=DISPLAY_ONLY, secondStatus=DISPLAY_ONLY
expected: "ACTIVE"
 but was: "DISPLAY_ONLY"
Tests run: 1, Failures: 1, Errors: 0, Skipped: 0
BUILD FAILURE
```
