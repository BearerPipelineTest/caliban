package caliban.schema

import caliban.schema.Annotations.GQLInterface
import caliban.{ GraphQL, RootResolver }
import zio.test._

object RootTypeSpec extends ZIOSpecDefault {

  override def spec =
    suite("RootTypeSpec")(
      test("do not override interface") {
        case class Queries(findCommon: CommonInterface, findInterface: MyInterface, findField: MyField)
        case class Mutations(createA: String => MyInterface.A, createB: String => MyInterface.B)

        val interfaceA = MyInterface.A(123, "321")
        val interfaceB = MyInterface.B(234, different = false)
        val myField    = MyField(1)

        val query     = Queries(interfaceA, interfaceB, myField)
        val mutations = Mutations(_ => interfaceA, _ => interfaceB)
        val resolver  = RootResolver(query, mutations)

        val graphQL: GraphQL[Any] = GraphQL.graphQL(resolver)

        graphQL.validateRootSchema.map { schema =>
          val rootType =
            RootType(
              schema.query.opType,
              schema.mutation.map(_.opType),
              schema.subscription.map(_.opType),
              Nil
            )

          def interfaceName(tpe: String): Option[List[String]] =
            rootType.types.get(tpe).flatMap(_.interfaces()).map(_.flatMap(_.name))

          assertTrue(interfaceName("A").contains(List("CommonInterface", "MyInterface"))) &&
          assertTrue(interfaceName("B").contains(List("MyInterface"))) &&
          assertTrue(interfaceName("MyField").contains(List("CommonInterface")))
        }
      }
    )

  @GQLInterface
  sealed trait CommonInterface

  @GQLInterface
  sealed trait MyInterface
  object MyInterface {
    case class A(common: Int, different: String)  extends MyInterface with CommonInterface
    case class B(common: Int, different: Boolean) extends MyInterface
  }

  case class MyField(common: Int) extends CommonInterface

}
