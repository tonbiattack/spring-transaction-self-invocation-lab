# E004 デバッグ記録：空リストで MyBatis の動的 IN 句が不完全になる

## 症状

商品検索 API の入力は `List<String> productIds` である。選択IDが空なら、全件を返さず0件を返す契約にした。しかし mapper XML では `IN` を `<foreach>` の外側に置いていたため、空リストで `<foreach>` が出力を生成せず、DBへ `WHERE product_id IN` だけが送られた。

## 観測事実

バグ状態の統合テストでは、空リストの入力だけが H2 の構文エラーとなった。非空リストでは指定した商品だけを返す対照テストが成功した。

```text
### SQL: SELECT product_id, status FROM products WHERE product_id IN
### Cause: org.h2.jdbc.JdbcSQLSyntaxErrorException: expected "("
Tests run: 2, Failures: 0, Errors: 1, Skipped: 0
BUILD FAILURE
```

この出力は、SQLの値バインド前の段階ではなく、動的 SQL のテキスト生成で `IN` の右辺が欠落したことを示す。

## 仮説の比較

| 仮説 | 予測 | 最小実験 | 結果 | 判定 |
| --- | --- | --- | --- | --- |
| A. mapper のパラメータ名を解決できない | `BindingException` が発生する | `@Param("productIds")` を使い、非空リストの検索を実行する | 非空リストは成功し、指定IDを返す | 棄却 |
| B. H2 が `IN` 条件を扱えない | 非空リストでも同じ構文エラーになる | 1要素のリストで mapper を実行する | `WHERE product_id IN (?)` が実行される | 棄却 |
| C. 空リストで `<foreach>` の出力がなくなる | 空リスト時だけ `WHERE product_id IN` で終わる | 空・非空の入力を同じ mapper で比較する | 空だけ構文エラー、非空は成功 | 採用 |

## 根本原因

MyBatis の `<foreach>` はコレクションを反復し、反復時の要素・区切り・開閉文字列を SQL へ出力する。空コレクションには反復する要素がないため、`<foreach>` の外側に置いた `IN` を補完できない。[1]

`nullable="true"` や `nullableOnForEach` は null コレクションに対する扱いを設定する属性であり、空コレクションを「常に0件」とする業務ルールを代替するものではない。[1] 空入力を SQL レベルでどう表すかを明示しなければならない。

## 最小修正

`<choose>` で入力を分岐し、非空のときだけ `IN (...)` を生成する。空または null のときは、意図的に偽となる `1 = 0` を生成する。

```xml
<select id="findByIds" resultMap="catalogProductMap">
  SELECT product_id, status
  FROM products
  <where>
    <choose>
      <when test="productIds != null and !productIds.isEmpty()">
        product_id IN
        <foreach collection="productIds" item="productId" open="(" separator="," close=")">
          #{productId}
        </foreach>
      </when>
      <otherwise>
        1 = 0
      </otherwise>
    </choose>
  </where>
</select>
```

`<where>` は内部コンテンツがある場合だけ `WHERE` を生成する。`<choose>` は条件を一つだけ選ぶため、空入力で全件検索になるような条件欠落も防げる。[1]

## 回帰確認

修正後は、空リストが構文エラーではなく0件となるテストと、非空リストが指定IDだけを返す対照テストが成功した。さらに既存E001〜E003を含む全テスト6件が成功した。

```text
対象テスト: Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
全テスト:   Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```
