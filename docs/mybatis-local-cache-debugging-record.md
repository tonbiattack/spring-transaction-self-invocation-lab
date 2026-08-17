# E003 デバッグ記録：MyBatis の SESSION ローカルキャッシュが同じ参照を返す

## 症状

`ProductViewService` は商品を取得した後、DB保存を意図しない表示用の変更として、取得済みオブジェクトの `status` を `DISPLAY_ONLY` に変更する。その後、同じサービスメソッド内で同じ商品を再検索すると、DBの値は `ACTIVE` のままなのに、再検索結果まで `DISPLAY_ONLY` になる。

## 観測事実

バグ状態のログは次のとおりである。

```text
mybatisLocalCache transactionActive=true, sameReference=true, firstStatus=DISPLAY_ONLY, secondStatus=DISPLAY_ONLY
expected: "ACTIVE"
 but was: "DISPLAY_ONLY"
```

`transactionActive=true` なので、E001 のようにトランザクションが開始されていない問題ではない。`sameReference=true` から、2回の mapper 呼び出しが同じ Java オブジェクト参照を返していることが分かる。

## 仮説の比較

| 仮説 | 予測 | 最小実験 | 結果 | 判定 |
| --- | --- | --- | --- | --- |
| A. DBの `ACTIVE` 更新が別処理で発生した | 1回目と2回目の間にDB値自体が `DISPLAY_ONLY` になる | 2回の取得の間に更新SQLを実行していないことを確認し、DB初期値と mapper 結果を比較する | DBの初期値は `ACTIVE`。更新SQLはない | 棄却 |
| B. Spring の read-only トランザクションが値を書き換えた | `@Transactional(readOnly=true)` が Java オブジェクトを変換する | トランザクション状態と Java オブジェクトの参照同一性をログ出力する | `transactionActive=true` だが、参照共有は MyBatis の結果 | 棄却 |
| C. MyBatis の SESSION ローカルキャッシュが同じ参照を返す | 同一セッションの2回目の検索で `sameReference=true` になり、1回目の変更が2回目に見える | `localCacheScope=SESSION` と `STATEMENT` を切り替える | SESSION では `true`、STATEMENT では `false` | 採用 |

## 根本原因

MyBatis の `localCacheScope` の既定値は `SESSION` であり、セッション中の繰り返しクエリをローカルキャッシュする。MyBatis の Java API ドキュメントは、SESSION スコープではローカルキャッシュに保存された同じオブジェクト参照を返すと説明している。[1]

このラボでは、MyBatis-Spring により Spring 管理の `SqlSession` がトランザクションの期間使われるため、同じサービスメソッド内の2回の mapper 呼び出しが同じセッション境界に入る。[2] 1回目の取得結果を直接変更すると、DBではなくキャッシュされた Java オブジェクトを変更しただけでも、2回目の結果にその変更が現れる。

## 最小修正

テスト設定を次のように変更した。

```diff
-mybatis.configuration.local-cache-scope=SESSION
+mybatis.configuration.local-cache-scope=STATEMENT
```

`STATEMENT` ではローカルキャッシュが statement 実行単位になり、異なる mapper 呼び出しの間で結果を共有しない。[1] 修正後のログは次のとおりである。

```text
mybatisLocalCache transactionActive=true, sameReference=false, firstStatus=DISPLAY_ONLY, secondStatus=ACTIVE
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

ただし、常に `STATEMENT` に変更することが正解とは限らない。セッション内の同一結果を再利用したい処理では性能上の利点がある一方、取得したオブジェクトを変更する設計とは衝突する。より局所的な設計として、mapper の戻り値を直接変更せず表示用 DTO へコピーする方法もある。今回のラボでは、MyBatis のキャッシュスコープ契約を明示的に比較するため、`STATEMENT` を最小設定変更として採用した。

## 回帰確認

対象テスト 1 件と、既存を含む全テスト 4 件が成功した。元の失敗テストは削除せず、`ACTIVE` が保持される利用者視点の契約として残している。
