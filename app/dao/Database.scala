package dao

sealed trait Database
case class Cockroach() extends Database
case class Fauna() extends Database
case class Spanner() extends Database
case class Postgres() extends Database
