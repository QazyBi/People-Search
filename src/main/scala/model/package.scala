package object model {

  case class Person(
      id: Option[Long],
      name: String,
      embedding: List[Double]
  )
  case object PersonNotFoundError
}
