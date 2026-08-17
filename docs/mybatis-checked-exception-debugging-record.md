# E002 デバッグ記録：MyBatis の INSERT がチェック例外後も残る

## 症状

`OrderRegistrationService.registerThenReject` は MyBatis mapper で `orders` テーブルへ INSERT した直後、`OrderRejectedException`（`Exception` の直接のサブクラス）を送出する。期待する契約は、業務上の拒否後に注文が DB に残らないことである。

## 観測事実

バグ状態では、ログに `mybatisInsert transactionActive=true` が出た。一方、テストが mapper で `COUNT(*)` を再読込すると、期待値 `0` に対して実測値 `1` となった。

```text
mybatisInsert transactionActive=true, orderId=order-001
expected: 0
 but was: 1
Tests run: 2, Failures: 1, Errors: 0, Skipped: 0
BUILD FAILURE
```

修正後は同じログで `transactionActive=true` を確認しながら、対象テスト 2 件と全テスト 3 件が成功した。

## 仮説の比較

| 仮説 | 予測 | 最小実験 | 結果 | 判定 |
| --- | --- | --- | --- | --- |
| A. MyBatis mapper が Spring のトランザクションに参加していない | INSERT 時に `transactionActive=false` になる、または別接続で即時コミットされる | `TransactionSynchronizationManager.isActualTransactionActive()` を INSERT 直後にログ出力する | `true`。MyBatis-Spring のトランザクション境界内で実行されている | 棄却 |
| B. 例外が送出されていない | 例外アサーションが失敗する | `assertThatThrownBy` で型とメッセージを確認する | `OrderRejectedException` は期待どおり送出された | 棄却 |
| C. チェック例外は Spring の既定 rollback rule に該当しない | トランザクションは有効だが、メソッド終了時にコミットされ、再読込で 1 件になる | `@Transactional` を維持したまま `rollbackFor` の有無だけを比較する | 修正前は 1 件、`rollbackFor = OrderRejectedException.class` 後は 0 件 | 採用 |

## 根本原因

Spring Framework の既定設定では、`RuntimeException` と `Error` はロールバックされるが、チェック例外はロールバックされない。したがって、`@Transactional` が有効であることと、任意の例外でロールバックされることは別の契約である。

MyBatis-Spring は既存の Spring のトランザクションマネージャを利用し、トランザクション中は単一の `SqlSession` を使い、完了時にコミットまたはロールバックする。今回の失敗は MyBatis がトランザクションを無視したのではなく、Spring がチェック例外を既定のロールバック条件とみなさなかったことで起きた。

## 最小修正

```java
@Transactional(rollbackFor = OrderRejectedException.class)
public void registerThenReject(String orderId) throws OrderRejectedException {
    orderMapper.insert(new OrderRecord(orderId, "PENDING"));
    throw new OrderRejectedException("在庫確認に失敗したため注文を取り消します");
}
```

修正の中心は `rollbackFor` の指定だけである。例外を `RuntimeException` に変更すること、MyBatis の `SqlSession` を手動で rollback すること、トランザクションをプログラムで管理することは、今回の根本原因に対する最小修正ではない。

## 回帰確認

```text
対象テスト: Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
全テスト:   Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

元の失敗ケースを回帰テストとして残し、対照ケースとして通常の INSERT が 1 件保存されることも確認した。
