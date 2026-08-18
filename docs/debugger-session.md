# E001: デバッガーで確認した自己呼び出しの呼び出し経路

## 目的

この記録では、`@Transactional` が付いたメソッドへ実際に到達しているにもかかわらず、トランザクションが開始されないことを、バグ状態のコミットでデバッガーにより確認します。確認対象は例外の発生ではなく、`BalanceAdjustmentService#adjustAndReject` から同じオブジェクトの `persistAdjustmentThenReject` へ入る経路です。

## 対象状態

| 項目 | 値 |
| --- | --- |
| バグコミット | `ef27ac3` — `残高調整のロールバック不全を再現する` |
| 修正コミット | `9a922b5` — `トランザクション境界を別Beanへ移す` |
| 実行日 | 2026-08-18 |
| JDK | OpenJDK 21.0.11 |
| Maven | 3.8.7 |
| データベース | H2（インメモリ、JDBC経由） |

## 実行したデバッグ手順

次の手順で、バグコミットを別ワークツリーに展開し、Surefireのデバッグ待受へ接続しました。`jdb` はJDK付属のデバッガーです。IDEを使う場合も、同じメソッドにブレークポイントを置けば確認できます。

```bash
# 通常の作業ツリーを汚さず、バグ状態を取り出す
git worktree add --detach /tmp/transaction-self-invocation-debug ef27ac3
cd /tmp/transaction-self-invocation-debug

# テストJVMをポート5005で停止して起動する
mvn --batch-mode \
  -Dtest=BalanceAdjustmentServiceIntegrationTest \
  -Dmaven.surefire.debug test

# 別ターミナルで接続する
jdb -attach localhost:5005
stop in com.example.transactionlab.BalanceAdjustmentService.persistAdjustmentThenReject
run
where
```

実測では、次の停止点に到達しました。

```text
Breakpoint hit: "thread=main",
com.example.transactionlab.BalanceAdjustmentService.persistAdjustmentThenReject(),
line=27
```

停止点は `@Transactional` を付けたメソッドの先頭です。したがって、アノテーションの付いたメソッドが呼ばれていないわけではありません。デバッガーで呼び出し元を追うと、直前のアプリケーション側フレームは同じ `BalanceAdjustmentService` の `adjustAndReject` です。バグコミットの実装は次の直接呼び出しです。

```java
public void adjustAndReject(String accountId, BigDecimal delta) {
    this.persistAdjustmentThenReject(accountId, delta);
}
```

この経路では、`persistAdjustmentThenReject` の直前に `TransactionInterceptor` が入りません。実測ログの `TransactionSynchronizationManager.isActualTransactionActive()` も `false` でした。

```text
transactionActive=false, savedBalance=125.00
```

ここで重要なのは、停止点へ到達したこと自体は `@Transactional` の適用を意味しない点です。Springの標準的なプロキシ方式では、横断処理はプロキシへの外部呼び出しに差し込まれます。対象インスタンス内部の `this` 呼び出しでは、その入口を通りません。[1] [2]

## 修正後に見るべき経路

修正コミットでは、呼び出し元とトランザクション境界を別のSpring Beanへ分けています。

```text
テスト
  -> BalanceAdjustmentService（公開メソッド）
    -> TransactionalBalanceWriter（Spring Bean）
      -> Spring AOP Proxy
        -> TransactionInterceptor
          -> persistAdjustmentThenReject（@Transactional）
```

この状態で同じ統合テストを実行すると、`TransactionalBalanceWriter` 内の観測は `transactionActive=true` となり、例外後に残高を再読込したテストは成功します。トランザクションが有効かどうかはログだけで判定せず、`BalanceAdjustmentServiceIntegrationTest` が検証する最終残高 `100.00` も必ず確認します。

```bash
cd /home/ubuntu/spring-transaction-self-invocation-lab
mvn --batch-mode -Dtest=BalanceAdjustmentServiceIntegrationTest test
```

## 使い分け

| 観測手段 | 確認できる事実 | このラボでの役割 |
| --- | --- | --- |
| ブレークポイントとスタック | 実際にどのメソッドから入り、どの呼び出し経路を通ったか | `this` 呼び出しであることを確認する |
| `TransactionSynchronizationManager` | 現在のスレッドに実トランザクションが存在するか | バグ状態の `false` と修正後の `true` を比較する |
| DB再読込を伴う統合テスト | 例外後に更新が残ったか、ロールバックされたか | 修正の有効性を最終状態で保証する |

## 注意点

この手順はSpringのデフォルトであるプロキシ方式を対象にします。AspectJモードではバイトコードへの織り込みにより自己呼び出しにもアドバイスが適用され得るため、同じ観測結果にはなりません。[1] また、`@Transactional` の既定のロールバック規則、トランザクション伝播、JPAのフラッシュ時点、複数データソースはこの記録の対象外です。

## References

[1] [Spring Framework Reference Documentation — Using `@Transactional`](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)

[2] [Spring Framework Reference Documentation — Proxying Mechanisms](https://docs.spring.io/spring-framework/reference/core/aop/proxying.html)
