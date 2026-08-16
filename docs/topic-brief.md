# 題材企画: Springの `@Transactional` 自己呼び出しによるロールバック不全

## 対象

| 項目 | 内容 |
| --- | --- |
| 対象言語 | Java 21 |
| 対象読者 | Javaの例外処理とSpring DIの基本を理解し、`@Transactional` を利用する初中級〜中級開発者 |
| 難易度プロファイル | 実践・上級 |
| 選定理由 | 例外の送出と最終的なDB状態が一致しないため、直接結果と永続化状態を分けて観測し、複数仮説を比較する必要がある。 |
| 実行基盤 | Maven 3.8.7、Spring Boot 3.5.0、Spring JDBC、H2、JUnit 5 |
| フレームワーク非依存性 | 該当しない。ユーザー指定どおりJava／Springのプロキシ方式を直接扱うため、言語仕様だけでは再現できない。Java向けのデバッグラボ要件を併用する。 |

## 学習する契約

> 入力 `account-001`（初期残高 `100.00`）と増分 `25.00` に対して、期待する状態は「業務例外後に残高が `100.00` のまま」である。しかしバグ状態では残高が `125.00` になる。

### 対象の直接原因

Springの標準的なプロキシ方式では、同じ対象Bean内での自己呼び出しはプロキシを経由せず、呼び出された `@Transactional` メソッドにトランザクションが適用されない。[1]

### 対象外

AspectJモード、自己注入、トランザクション伝播の個別設定、JPAのフラッシュ時点、複数データソース、並行更新、実運用における責務分割の全般的な設計判断は扱わない。

## 再現設計

| 要素 | 決定 |
| --- | --- |
| 公開境界 | `BalanceAdjustmentService#adjustAndReject` |
| 入力・初期状態 | H2に `account-001` と残高 `100.00` を保存し、増分 `25.00` を渡す。 |
| Redの観測 | `BalanceAdjustmentRejectedException` の後にDBを再読込し、`100.00` を期待して `125.00` となるアサーション差分を観測する。 |
| 最終観測 | `AccountBalanceRepository#findBalance` によるデータベースからの再読込。ログで `transactionActive` も補助的に確認する。 |
| 決定性 | ローカルのH2のみを使い、時刻、待機、外部ネットワーク、並行実行を使わない。 |
| 固定状態の検証コマンド | `mvn --batch-mode test` |
| バグ状態の確認コマンド | `mvn --batch-mode -Dtest=BalanceAdjustmentServiceIntegrationTest test` |

## 仮説

| 仮説 | どう検証または除外するか |
| --- | --- |
| `@Transactional` 自体が有効化されていない | 別Bean上の同じアノテーション付きメソッドを実行し、`transactionActive=true` と最終残高のロールバックを確認する。 |
| 例外がロールバック対象外である | 同じ `RuntimeException` を維持したまま呼び出し経路だけを変え、最終残高を比較する。 |
| 自己呼び出しがプロキシを迂回する | バグ状態と修正状態でログのトランザクション状態とDBの最終状態を比較し、公式仕様と照合する。 |

## 予定した履歴

| 順序 | コミットの目的 | 期待する状態 |
| --- | --- | --- |
| 1 | `ef27ac3`：残高調整のロールバック不全を再現する | 対象テストが `expected: 100.00 / but was: 125.00` で失敗する。 |
| 2 | `9a922b5`：トランザクション境界を別Beanへ移す | 同じテストと全テストが成功する。 |

## References

[1] [Spring Framework Reference Documentation — Using `@Transactional`](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)
