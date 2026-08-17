# E002 題材メモ：MyBatis の INSERT がチェック例外後も残る

## 選定理由

Spring の `@Transactional` は、例外が発生したことだけで一律にロールバックするわけではない。既定では `RuntimeException` と `Error` がロールバック対象であり、チェック例外は対象外である。この契約は、MyBatis の mapper が同じ Spring 管理トランザクションに参加している場合にも適用される。

今回の最小再現では、`OrderRegistrationService.registerThenReject` が MyBatis mapper で注文を INSERT した直後に `OrderRejectedException extends Exception` を送出する。テストは「例外が出た」だけではなく、別の mapper 呼び出しで DB を再読込し、注文が残っていないことを利用者視点で確認する。

## 既存題材との差分

既存の E001 は、同一 Bean 内の自己呼び出しにより `@Transactional` のプロキシを通らず、トランザクション自体が開始されない問題を扱う。E002 は別 Bean の公開メソッドを外部から呼び出しており、ログで `transactionActive=true` を確認できる。それでもチェック例外に対する既定の rollback rule が適用されないため INSERT がコミットされる、という異なる契約を扱う。

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
mybatisInsert transactionActive=true, orderId=order-001
expected: 0
 but was: 1
Tests run: 2, Failures: 1, Errors: 0, Skipped: 0
BUILD FAILURE
```
