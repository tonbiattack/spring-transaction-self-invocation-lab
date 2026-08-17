# E001: `@Transactional` の自己呼び出しでロールバックされない

本リポジトリは、Java／Spring Bootで起きるトランザクション境界の不具合を、**失敗する統合テスト、実行時ログ、データベースの再読込、最小修正**の順に追うための実行可能なデバッグ学習ラボです。既定ブランチは常にテストが成功する状態に保ち、意図した失敗状態はGit履歴に残しています。

> **学習する契約**：残高調整が業務ルール違反で例外になる場合、呼び出し後にデータベースを再読込しても残高は更新前の `100.00` でなければなりません。

## 学習の進め方

| 段階 | 実施内容 | 確認する観測点 |
| --- | --- | --- |
| 再現 | バグコミットで統合テストを実行する | 例外は発生するが、残高は `125.00` と永続化される |
| 観測 | ログとデータベース再読込を確認する | `transactionActive=false` と `expected: 100.00 / but was: 125.00` |
| 修正 | トランザクション付き処理を別Beanへ移す | Springのプロキシを経由して処理が呼ばれる |
| 回帰防止 | 同一の統合テストを再実行する | 例外後のDB再読込で `100.00` が維持される |

## 収録済み教材

| ID | テーマ | バグ状態の観測 | 修正後に守る契約 |
| --- | --- | --- | --- |
| E001 | `@Transactional` の自己呼び出し | 例外後の残高が `100.00` ではなく `125.00`。ログは `transactionActive=false`。 | 例外後のDB再読込で残高は `100.00` のままである。 |
| E002 | MyBatis のチェック例外に対する rollback rule | `transactionActive=true` でも、例外後の注文件数が `0` ではなく `1`。 | `rollbackFor = OrderRejectedException.class` を指定し、例外後の注文件数を `0` にする。 |
| E003 | MyBatis の SESSION ローカルキャッシュと同一オブジェクト参照 | DBを書き換えていない表示用変更が、同一セッションの再検索結果にも現れ、`ACTIVE` が `DISPLAY_ONLY` になる。 | キャッシュスコープとオブジェクト変更の責務を分離し、再検索が新しい値を得る契約を守る。 |

## 必要な環境

| 項目 | 本ラボで検証したバージョン |
| --- | --- |
| JDK | 21.0.11 |
| Maven | 3.8.7 |
| Spring Boot | 3.5.0 |
| データベース | H2（インメモリ） |

## 修正後のテストを実行する

既定ブランチでは、以下のコマンドで完全なテストスイートが成功します。テストは例外の種類だけでなく、その後にデータベースを読み直した残高も確認します。

```bash
mvn --batch-mode test
```

## バグを自分で再現する

次の手順では、失敗状態を確認した後、必ず既定ブランチへ戻します。バグコミットでの失敗は、設定や依存関係の不備ではなく、残高の最終状態に対する意図的なアサーション差分です。

```bash
git switch --detach ef27ac3
mvn --batch-mode -Dtest=BalanceAdjustmentServiceIntegrationTest test
# expected: 100.00
#  but was: 125.00

git switch main
mvn --batch-mode test
# BUILD SUCCESS
```

## プロジェクト構成

```text
src/main/java/com/example/transactionlab/
├── BalanceAdjustmentService.java       # 呼び出し元の公開境界
├── TransactionalBalanceWriter.java     # @Transactional を持つ別Bean
└── AccountBalanceRepository.java       # H2への読み書き
src/test/java/com/example/transactionlab/
└── BalanceAdjustmentServiceIntegrationTest.java

docs/
├── topic-brief.md
├── debugging-record.md
├── bug-state-test-output.log
└── fixed-state-test-output.log
```

Springの標準的なプロキシ方式では、外部からプロキシを経由して入る呼び出しだけが横断的処理の対象になります。そのため、同じ対象オブジェクト内での自己呼び出しでは、呼び出されたメソッドに `@Transactional` が付いていてもトランザクションが開始されません。[1] 本ラボはこの挙動を示す一点に限定しています。

詳しい観測、競合仮説、最小修正の根拠は、[デバッグ記録](docs/debugging-record.md)を参照してください。
E002 の観測、競合仮説、MyBatis mapper の確認、最小修正の根拠は、[チェック例外のデバッグ記録](docs/mybatis-checked-exception-debugging-record.md)を参照してください。

## References

[1] [Spring Framework Reference Documentation — Using `@Transactional`](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)
