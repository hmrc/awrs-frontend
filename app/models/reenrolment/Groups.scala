package models.reenrolment

import play.api.libs.json.{Json, Reads}

case class Groups(principalGroupIds: Seq[String], delegatedGroupIds: Seq[String])

object Groups {
  implicit val reads: Reads[Groups] = Json.reads[Groups]
}
