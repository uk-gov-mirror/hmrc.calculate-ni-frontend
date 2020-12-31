package eoi

package object frontend {
  implicit class RichOptBigDecimal(in: Option[BigDecimal]) {
    def positive: Option[BigDecimal] = in.map(_.max(BigDecimal(0)))
    def negative: Option[BigDecimal] = in.map(- _).positive
  }
}
