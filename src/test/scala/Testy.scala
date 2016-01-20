object Testy extends App {

  case class Person(
                     id: Option[Long],
                     firstName: String,
                     lastName: String,
                     address: Address)

  case class Address(id: Option[Long],
                     name: String,
                     number: Int)

  val personOrAddress: AnyRef = Person(Some(1L), "first", "last", Address(Some(1L), "street", 1))
  type HasCopyId = {def copyId(id: Option[Long]): AnyRef}
  val id = Some(123L)
  personOrAddress.asInstanceOf[HasCopyId].copyId(id = id)
}
