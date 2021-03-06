package org.adridadou.openlaw.parser.template.variableTypes

import org.adridadou.openlaw.parser.template._
import org.adridadou.openlaw.parser.template.formatters.{Formatter, NoopFormatter}
import io.circe._
import io.circe.syntax._
import io.circe.parser._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case object StripeCallType extends VariableType(name = "StripeCall") {
  implicit val smartContractEnc: Encoder[StripeCall] =
    deriveEncoder[StripeCall]
  implicit val smartContractDec: Decoder[StripeCall] =
    deriveDecoder[StripeCall]

  override def cast(value: String, executionResult: TemplateExecutionResult): StripeCall =
    handleEither(decode[StripeCall](value))

  override def internalFormat(value: Any): String =
    value match {
      case call: StripeCall => call.asJson.noSpaces
    }

  override def defaultFormatter: Formatter = new NoopFormatter

  override def construct(
    constructorParams: Parameter,
    executionResult: TemplateExecutionResult
  ): Option[StripeCall] =
    constructorParams match {
      case Parameters(values) =>
        Some(StripeCall(beneficiary = getExpression(values.toMap, "beneficiary")))
      case _ =>
        val msg = "Stripe Calls need to get 'beneficiary' as constructor parameter"
        throw new RuntimeException(msg)
    }

  def thisType: VariableType = StripeCallType
}
