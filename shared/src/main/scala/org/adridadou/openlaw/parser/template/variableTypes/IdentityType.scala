package org.adridadou.openlaw.parser.template.variableTypes

import java.time.LocalDateTime

import io.circe._
import io.circe.syntax._
import io.circe.parser._
import io.circe.generic.semiauto._
import Identity._
import cats.Eq
import org.adridadou.openlaw.parser.template.formatters.{Formatter, NoopFormatter, SignatureFormatter}
import org.adridadou.openlaw.parser.template._
import org.adridadou.openlaw.oracles.UserId

case object IdentityType extends VariableType(name = "Identity") {

  override def cast(value: String, executionResult: TemplateExecutionResult): Identity =
    decode[Identity](value) match {
      case Right(identity) => identity
      case Left(ex) => throw new RuntimeException(ex)
    }

  override def defaultFormatter: Formatter = new NoopFormatter

  def thisType: VariableType = IdentityType

  override def construct(constructorParams: Parameter, executionResult: TemplateExecutionResult): Option[Any] =
    throw new RuntimeException("Identity type does not support constructor")

  override def missingValueFormat(name: VariableName): Seq[AgreementElement] = Seq(FreeText(Text("")))

  override def internalFormat(value: Any): String = VariableType.convert[Identity](value).asJson.noSpaces

  override def getFormatter(formatterDefinition: FormatterDefinition, executionResult: TemplateExecutionResult):Formatter = formatterDefinition.name.trim().toLowerCase() match {
    case "signature" => new SignatureFormatter
    case _ => throw new RuntimeException(s"unknown formatter $name")
  }

  override def access(value: Any, keys: Seq[String], executionResult: TemplateExecutionResult): Either[String, Any] = {
    keys.toList match {
      case head::tail if tail.isEmpty => accessProperty(Some(VariableType.convert[Identity](value)), head)
      case _::_ => Left(s"Address has only one level of properties. invalid property access ${keys.mkString(".")}")
      case _ => Right(value)
    }
  }

  override def validateKeys(name:VariableName, keys: Seq[String], executionResult: TemplateExecutionResult): Option[String] = keys.toList match {
    case Nil => None
    case head::tail if tail.isEmpty => checkProperty(head)
    case _::_ => Some(s"invalid property ${keys.mkString(".")}")
  }

  private def checkProperty(key:String):Option[String] = accessProperty(None, key) match {
    case Left(ex) => Some(ex)
    case Right(_) => None
  }

  private def accessProperty(identity: Option[Identity], property: String):Either[String, String] = {
    property.toLowerCase() match {
      case "id" => Right(getOrNa(identity.flatMap(_.id).map(_.id)))
      case "email" => Right(getOrNa(identity.map(_.email.email)))
      case _ => Left(s"property '$property' not found for type Identity")
    }
  }

  private def getOrNa(optStr:Option[String]):String = optStr.getOrElse("[n/a]")
}

object IdentityIdentifier {
  implicit val identityIdentifierEnc: Encoder[IdentityIdentifier] = deriveEncoder[IdentityIdentifier]
  implicit val identityIdentifierDec: Decoder[IdentityIdentifier] = deriveDecoder[IdentityIdentifier]
}

case class IdentityIdentifier(identityProviderId:String, identifier:String)

case class Identity(id:Option[UserId], email:Email, identifiers:Seq[IdentityIdentifier]) {
  def withId(userId: UserId): Identity = this.copy(id = Some(userId))

  def getJsonString: String = this.asJson.noSpaces

  def userId:UserId = id.getOrElse(UserId(email.email))
}

case object Identity {
  def withEmail(email: Email): Identity = Identity(id = None, email = email, identifiers = Seq(IdentityIdentifier("openlaw", email.email))  )

  implicit val identityEnc: Encoder[Identity] = deriveEncoder[Identity]
  implicit val identityDec: Decoder[Identity] = deriveDecoder[Identity]
}

case class Email(email:String) {
  override def toString:String = email
}

object Email {

  private val emailRegex = """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r

  implicit val eqEmail:Eq[Email] = Eq.fromUniversalEquals
  implicit val emailEnc: Encoder[Email] = (a: Email) => Json.fromString(a.email)
  implicit val emailDec: Decoder[Email] = (c: HCursor) => for {
    name <- c.as[String]
  } yield Email(name)

  def validate(email:String):Either[String,Email] = emailRegex.findFirstMatchIn(email) match {
    case Some(_) => Right(new Email(email.trim.toLowerCase()))
    case None =>
      Left(s"invalid Email $email")
  }

  def apply(email:String):Email = validate(email) match {
    case Right(e) => e
    case Left(ex) => throw new RuntimeException(ex)
  }
}

case class SignatureAction(userId: UserId, identityIdentifiers:Seq[IdentityIdentifier]) extends ActionValue {
  override def nextActionSchedule(executionResult: TemplateExecutionResult, pastExecutions: Seq[OpenlawExecution]): Option[LocalDateTime] = {
    if(executionResult.hasSigned(userId)) {
      None
    } else {
      Some(LocalDateTime.now)
    }
  }
}