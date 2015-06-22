import play.api.libs.json._

val jsonString = """{
                 |  "a": {
                 |    "id": 1
                 |  },
                 |  "b": {
                 |    "id": 2
                 |  }
                 |}
               """.stripMargin

val jsonAst = Json.parse(jsonString)

(jsonAst \\ "id").map(_.as[Int])
(jsonAst \\ "not-id").map(_.as[Int])

Vector().mkString(",")