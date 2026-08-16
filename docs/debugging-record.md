# E001: `@Transactional` の自己呼び出しでロールバックされない

## 目的

残高 `100.00` の口座に `25.00` を加算する処理が、業務ルール違反の例外で拒否された場合、**例外の発生後にデータベースを読み直しても残高は `100.00` でなければならない**という契約を守る。HTTP応答やログだけで完了を判断せず、永続化後の最終状態を独立して検証する。

## 最初に観測した事実

バグ状態はコミット `ef27ac3`（`残高調整のロールバック不全を再現する`）に保存した。次のコマンドを実行すると、テストは設定・コンパイルではなく、残高の期待値と実際値の差で失敗する。

```bash
git switch --detach ef27ac3
mvn --batch-mode -Dtest=BalanceAdjustmentServiceIntegrationTest test
```

| 観測項目 | 期待 | 実際 | 根拠 |
| --- | --- | --- | --- |
| 呼び出し結果 | `BalanceAdjustmentRejectedException` が送出される | 同じ例外が送出された | `assertThatThrownBy` |
| 中間処理 | 更新処理はトランザクション内で実行される | ログは `transactionActive=false, savedBalance=125.00` | `docs/bug-state-test-output.log` |
| 最終状態 | 例外後にDBを再読込した残高は `100.00` | DBを再読込すると `125.00` | `BalanceAdjustmentServiceIntegrationTest` |

```text
[例外後にDBを再読込しても、ロールバック後の残高であること]
expected: 100.00
 but was: 125.00
```

この失敗は、例外が出たかどうかだけを確認するテストでは検出できない。例外は意図どおりに送出されている一方、例外前に実行された書込みは残っているためである。

## テストの境界

`@SpringBootTest` とH2のインメモリデータベースを使う統合テストを選んだ。検証対象はJavaメソッドの戻り値だけではなく、Springが生成するトランザクション用プロキシを経由した結果としての永続化状態である。テストは、公開境界 `BalanceAdjustmentService#adjustAndReject` を実行し、まず例外を確認した後、`AccountBalanceRepository#findBalance` によりデータベースを再読込する。この二つの観測点により、例外とロールバックを混同しない。

| 設計要素 | 決定 |
| --- | --- |
| 公開境界 | `BalanceAdjustmentService#adjustAndReject` |
| 初期状態 | `account-001` の残高を `100.00` としてH2へ保存 |
| 入力 | `account-001` と増分 `25.00` |
| Redの観測 | `100.00` を期待する最終状態アサーションが `125.00` で失敗 |
| Greenの観測 | 同じアサーションが成功し、例外も維持される |
| 決定性 | 時刻・待機・外部サービスを使わず、ローカルのH2のみを使用 |

## 仮説と切り分け

| 仮説 | 確認方法 | 結果 |
| --- | --- | --- |
| トランザクション管理自体が有効化されていない | 修正後に別Bean上の `@Transactional` メソッドを同じテストで実行し、ログのトランザクション状態とDB最終状態を確認する | 否定。修正後は `transactionActive=true` となり、テストは成功した。 |
| `RuntimeException` へのロールバック規則が不足している | 同じ `BalanceAdjustmentRejectedException` を維持したまま、呼び出し経路だけを変更する | 否定。修正後は同じ例外で残高が `100.00` に維持された。Springの既定規則でも `RuntimeException` はロールバック対象である。[1] |
| 同一Bean内の自己呼び出しがプロキシを通らない | バグ状態の直接呼び出しと、別Beanへの委譲後のログおよび最終残高を比較する | 採用。バグ状態は `false`、修正後は `true` であり、契約テストもGreenになった。 |

## 原因

バグ状態の `BalanceAdjustmentService#adjustAndReject` は、同じインスタンスにある `persistAdjustmentThenReject` を直接呼び出していた。後者には `@Transactional` が付いていても、Springの既定であるプロキシ方式では、プロキシを経由して対象オブジェクトへ入る外部呼び出しだけがインターセプトされる。したがって自己呼び出しでは、実行時のトランザクションは開始されない。[1]

バグ状態のログ `transactionActive=false` と、例外後の残高 `125.00` は、この説明と整合する。特に、書込みログだけで原因を断定せず、H2からの再読込で最終状態を確認した。

## 修正

トランザクションを必要とする書込みと例外送出を `TransactionalBalanceWriter` という別のSpring Beanへ移した。呼び出し元の `BalanceAdjustmentService` はそのBeanへ委譲するため、`@Transactional` の対象メソッドはSpringプロキシを経由する。

```java
@Service
public class TransactionalBalanceWriter {
    @Transactional
    public void persistAdjustmentThenReject(String accountId, BigDecimal delta) {
        BigDecimal updatedBalance = repository.findBalance(accountId).add(delta);
        repository.save(accountId, updatedBalance);
        throw new BalanceAdjustmentService.BalanceAdjustmentRejectedException(
                "業務ルールにより残高調整を取り消します"
        );
    }
}
```

この変更は、例外型、入力、データベースアクセス、テストの期待値を変更しない。トランザクション境界の配置だけを変える最小修正である。自己注入やAspectJモードへの切替は、この小さなラボの原因を説明するには必要ないため採用しなかった。

## 再発防止テスト

`BalanceAdjustmentServiceIntegrationTest#拒否された残高調整は例外後も永続化されてはならない` は修正前に失敗し、修正後に成功する同一のテストである。テストは次の順に契約を確認する。

| 順序 | 検証 | 意義 |
| --- | --- | --- |
| 1 | `BalanceAdjustmentRejectedException` の送出を確認する | 業務上の拒否が維持されていることを確認する。 |
| 2 | 例外後にリポジトリから残高を再読込する | トランザクションの最終効果を独立に確認する。 |
| 3 | 残高が `100.00` であることを確認する | 元のロールバック不全を検出する。 |

修正後のログでは `transactionActive=true, savedBalance=125.00` が観測され、テスト終了後の再読込は `100.00` を確認する。ログは中間状態の根拠であり、正しさの判定は最終状態アサーションに依存する。

## 再現手順

```bash
# バグ状態：意図した残高差分で失敗する
git switch --detach ef27ac3
mvn --batch-mode -Dtest=BalanceAdjustmentServiceIntegrationTest test

# 修正状態：同じテストを含む全テストが成功する
git switch main
mvn --batch-mode test
```

修正コミットは `9a922b5`（`トランザクション境界を別Beanへ移す`）である。テスト出力はそれぞれ `docs/bug-state-test-output.log` と `docs/fixed-state-test-output.log` に保存している。

## 適用範囲と注意点

本ラボは、Springの標準的なプロキシ方式で、同じクラス内のメソッド呼び出しが `@Transactional` を通過しない一点を扱う。AspectJモードを使う構成、既存トランザクションへの伝播指定、例外規則のカスタマイズ、JPAのフラッシュ時点、複数データソース、並行更新は対象外である。別Beanへ分ける設計が常に最良であることを一般化するものではなく、トランザクション境界とコンポーネント責務は個別の業務設計として評価する必要がある。

## References

[1] [Spring Framework Reference Documentation — Using `@Transactional`](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)
